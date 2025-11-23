package com.streatfeast.app.repositories

import android.content.Context
import android.util.Log
import com.streatfeast.app.BuildConfig
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.network.SupabaseOrderDto
import com.streatfeast.app.network.SupabaseOrderItemDto
import com.streatfeast.app.network.SupabaseStoreDto
import com.streatfeast.app.storage.OrderEntity
import com.streatfeast.app.storage.OrderItemEntity
import com.streatfeast.app.storage.OrderLocalDataSource
import com.streatfeast.app.utils.Constants
import com.streatfeast.app.utils.NotificationHelper
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// PostgREST
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order as PgOrder
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

// Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.http.append

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SupabaseOrderRepository(
    private val client: SupabaseClient,
    private val localDataSource: OrderLocalDataSource,
    private val context: Context,
    /**
     * Fallback store ID. MUST be a valid UUID if your `store_id` column is UUID.
     */
    private val storeId: String = Constants.DEFAULT_STORE_ID
) {
    private var realtimeChannel: RealtimeChannel? = null

    // Store multiple callbacks for new order notifications
    private val callbacks = mutableListOf<((orderId: String, orderNumber: Int?) -> Unit)>()
    private val callbacksMutex = Mutex()

    // Cache for fetched store UUID
    @Volatile
    private var cachedStoreId: String? = null
    private val storeIdMutex = Mutex()

    // Track previous order IDs to detect new orders
    private val previousOrderIds = mutableSetOf<String>()
    private val orderIdsMutex = Mutex()

    /**
     * Fetches the first active store UUID from the stores table.
     * Returns null if no active store is found or if the query fails.
     */
    private suspend fun fetchStoreId(): String? = runCatching {
        withContext(Dispatchers.IO) {
            val stores = client.postgrest["stores"]
                .select {
                    filter { eq("is_active", true) }
                    limit(1)
                }
                .decodeList<SupabaseStoreDto>()

            stores.firstOrNull()?.id
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "Failed to fetch store ID from database", e)
    }.getOrNull()

    /**
     * Gets the store ID, fetching from database if not cached.
     * Returns the cached/fetched UUID, or the default if fetch fails.
     */
    private suspend fun getStoreId(): String {
        // Return cached value if available
        cachedStoreId?.let { return it }

        // Fetch and cache store ID (thread-safe)
        return storeIdMutex.withLock {
            // Double-check after acquiring lock
            cachedStoreId?.let { return@withLock it }

            val fetchedId = fetchStoreId()
            cachedStoreId = fetchedId ?: null
            fetchedId ?: storeId // Fallback to default if fetch fails
        }
    }

    fun observeOrders(status: OrderStatus): Flow<List<Order>> = flow {
        val currentStoreId = getStoreId()
        Log.d("SupabaseOrderRepository", "observeOrders: storeId=$currentStoreId, status=${status.toRemoteValue()}")
        emitAll(localDataSource.observeOrders(currentStoreId, status))
    }

    suspend fun refresh() {
        Log.d("SupabaseOrderRepository", "refresh() called")
        runCatching {
            withContext(Dispatchers.IO) {
                val currentStoreId = getStoreId()
                Log.d("SupabaseOrderRepository", "refresh() fetching orders for storeId=$currentStoreId")

                val orders = client.postgrest["orders"]
                    .select {
                        filter { eq("store_id", currentStoreId) }
                        order(column = "created_at", order = PgOrder.DESCENDING)
                    }
                    .decodeList<SupabaseOrderDto>()

                Log.d("SupabaseOrderRepository", "refresh() fetched ${orders.size} orders")

                val orderIds = orders.map { it.id }

                val items = if (orderIds.isEmpty()) {
                    emptyList()
                } else {
                    client.postgrest["order_items"]
                        .select {
                            filter { isIn("order_id", orderIds) }
                        }
                        .decodeList<SupabaseOrderItemDto>()
                }

                val orderEntities = orders.map { it.toEntity() }
                val itemEntities = items.map { it.toEntity() }
                localDataSource.replaceStoreData(currentStoreId, orderEntities, itemEntities)

                Log.d("SupabaseOrderRepository", "refresh() completed - stored ${orderEntities.size} orders, ${itemEntities.size} items in Room")

                // Update previous order IDs set after refresh
                orderIdsMutex.withLock {
                    previousOrderIds.clear()
                    previousOrderIds.addAll(orderIds)
                }
            }
        }.onFailure { e ->
            Log.e("SupabaseOrderRepository", "Failed to refresh orders", e)
        }
    }

    /**
     * Adds a callback to be invoked when a new order is detected.
     * Callbacks are stored and invoked even if realtime channel already exists.
     */
    suspend fun addCallback(callback: (orderId: String, orderNumber: Int?) -> Unit) {
        callbacksMutex.withLock {
            callbacks.add(callback)
            Log.d("SupabaseOrderRepository", "Callback registered. Total callbacks: ${callbacks.size}")
        }
    }

    /**
     * Starts realtime subscription for the current store's orders.
     *
     * Uses postgresChangeFlow<PostgresAction> from supabase-kt 3.x.
     *
     * @param scope Coroutine scope for collecting the flow
     * @param onNewOrder Callback invoked when a new order is detected. Receives orderId and orderNumber.
     *                   This callback will be added to the callbacks list and invoked along with all registered callbacks.
     */
    suspend fun startRealtime(
        scope: CoroutineScope,
        onNewOrder: ((orderId: String, orderNumber: Int?) -> Unit)? = null
    ) {
        // Register callback if provided (even if channel already exists)
        onNewOrder?.let { callback ->
            addCallback(callback)
        }

        // If channel already exists, just return - callbacks are already registered
        if (realtimeChannel != null) {
            Log.d("SupabaseOrderRepository", "Realtime channel already exists. Callback registered.")
            return
        }

        val currentStoreId = getStoreId()
        Log.d("SupabaseOrderRepository", "Starting realtime subscription for storeId=$currentStoreId")

        runCatching {
            val channel = client.realtime.channel("realtime:public:orders")

            // Listen to ALL Postgres actions for public.orders filtered by store_id
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "orders"
                // Use typed filter API (same syntax as postgrest filters)
                filter("store_id", FilterOperator.EQ, currentStoreId)
                // OR, if you really want raw string:
                // filter = "store_id=eq.$currentStoreId"
            }

            // Collect the flow and refresh when something changes
            scope.launch {
                changeFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            Log.d(
                                "SupabaseOrderRepository",
                                "Realtime INSERT: ${action.record}"
                            )
                            
                            // Extract order ID and number from the insert record
                            val orderId = action.record["id"]?.toString() ?: ""
                            val orderNumber = (action.record["number"] as? Number)?.toInt()
                            
                            // Check if this is a genuinely new order
                            val isNewOrder = orderIdsMutex.withLock {
                                if (orderId.isNotEmpty() && orderId !in previousOrderIds) {
                                    previousOrderIds.add(orderId)
                                    true
                                } else {
                                    false
                                }
                            }
                            
                            // Invoke callbacks and show notification for new orders
                            if (isNewOrder) {
                                Log.d(
                                    "SupabaseOrderRepository",
                                    "New order detected: id=$orderId, number=$orderNumber"
                                )
                                
                                // Show local notification
                                if (orderNumber != null) {
                                    NotificationHelper.showNewOrderNotification(context, orderNumber)
                                }
                                
                                // Invoke all registered callbacks
                                callbacksMutex.withLock {
                                    Log.d("SupabaseOrderRepository", "Invoking ${callbacks.size} registered callbacks")
                                    callbacks.forEach { callback ->
                                        try {
                                            callback(orderId, orderNumber)
                                        } catch (e: Exception) {
                                            Log.e("SupabaseOrderRepository", "Error invoking callback: ${e.message}", e)
                                        }
                                    }
                                }
                            }
                            
                            Log.d("SupabaseOrderRepository", "Calling refresh() after realtime INSERT event")
                            refresh()
                        }
                        is PostgresAction.Update,
                        is PostgresAction.Delete -> {
                            Log.d(
                                "SupabaseOrderRepository",
                                "Realtime change: ${action::class.simpleName} for orders"
                            )
                            Log.d("SupabaseOrderRepository", "Calling refresh() after realtime ${action::class.simpleName} event")
                            refresh()
                        }
                        is PostgresAction.Select -> {
                            // Usually can be ignored
                            Log.d(
                                "SupabaseOrderRepository",
                                "Realtime select: ${action.record}"
                            )
                        }
                    }
                }
            }

            // Subscribe to channel (this will connect Realtime if needed)
            channel.subscribe()
            Log.d("SupabaseOrderRepository", "Realtime channel subscribed successfully")

            realtimeChannel = channel

            // Initial sync
            scope.launch { 
                Log.d("SupabaseOrderRepository", "Performing initial sync after realtime subscription")
                refresh() 
            }
        }.onFailure { e ->
            Log.e("SupabaseOrderRepository", "Failed to start realtime subscription", e)
        }
    }

    suspend fun stopRealtime() {
        realtimeChannel?.let { ch ->
            Log.d("SupabaseOrderRepository", "Stopping realtime subscription")
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    ch.unsubscribe()
                    // In v3, removing the channel is not required: it is GC'd after unsubscribe
                    Log.d("SupabaseOrderRepository", "Realtime channel unsubscribed")
                }.onFailure { e ->
                    Log.e("SupabaseOrderRepository", "Failed to stop realtime", e)
                }
            }
            realtimeChannel = null
            // Clear callbacks when stopping realtime
            callbacksMutex.withLock {
                callbacks.clear()
                Log.d("SupabaseOrderRepository", "Callbacks cleared")
            }
        }
    }

    suspend fun acceptOrder(orderId: String): Result<Unit> =
        updateStatus(orderId, OrderStatus.IN_KITCHEN)

    suspend fun markPrepared(orderId: String): Result<Unit> =
        updateStatus(orderId, OrderStatus.PREPARED)

    suspend fun markDelivered(orderId: String): Result<Unit> =
        updateStatus(orderId, OrderStatus.DELIVERED)

    suspend fun acceptAllOrders(): Result<Int> =
        bulkUpdateStatus(OrderStatus.CREATED, OrderStatus.IN_KITCHEN)

    suspend fun markAllPrepared(): Result<Int> =
        bulkUpdateStatus(OrderStatus.IN_KITCHEN, OrderStatus.PREPARED)

    suspend fun markAllDelivered(): Result<Int> =
        bulkUpdateStatus(OrderStatus.PREPARED, OrderStatus.DELIVERED)

    private suspend fun bulkUpdateStatus(
        fromStatus: OrderStatus,
        toStatus: OrderStatus
    ): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val currentStoreId = getStoreId()
            Log.d("SupabaseOrderRepository", "Bulk update: $fromStatus -> $toStatus for store: $currentStoreId")
            
            // First, get the orders that will be updated (for notification)
            val ordersToUpdate = client.postgrest["orders"].select {
                filter {
                    eq("store_id", currentStoreId)
                    eq("status", fromStatus.toRemoteValue())
                }
            }.decodeList<SupabaseOrderDto>()
            
            val count = ordersToUpdate.size
            Log.d("SupabaseOrderRepository", "Found $count orders to update")
            
            if (count == 0) {
                return@withContext 0
            }
            
            // Update all matching orders
            client.postgrest["orders"].update(
                {
                    set("status", toStatus.toRemoteValue())
                    set("updated_at", isoNowUtc())
                }
            ) {
                filter {
                    eq("store_id", currentStoreId)
                    eq("status", fromStatus.toRemoteValue())
                }
            }
            
            Log.d("SupabaseOrderRepository", "Bulk update completed: $count orders updated")
            
            // Manually invoke Edge Function for bulk notification
            try {
                invokeBulkNotification(currentStoreId, toStatus, count)
            } catch (e: Exception) {
                Log.e("SupabaseOrderRepository", "Failed to send bulk notification", e)
                // Don't fail the update if notification fails
            }
            
            // Refresh local data
            refresh()
            
            count
        }
    }

    private suspend fun invokeBulkNotification(
        storeId: String,
        status: OrderStatus,
        count: Int
    ) {
        // Use HTTP client to invoke Edge Function directly
        withContext(Dispatchers.IO) {
            try {
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val anonKey = BuildConfig.SUPABASE_ANON_KEY
                
                val payload = mapOf(
                    "type" to "BULK_UPDATE",
                    "table" to "orders",
                    "storeId" to storeId,
                    "toStatus" to status.toRemoteValue(),
                    "count" to count
                )
                
                val httpClient = HttpClient(OkHttp) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                        })
                    }
                }
                
                val response = httpClient.post("$supabaseUrl/functions/v1/order-events") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append(HttpHeaders.Authorization, "Bearer $anonKey")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                
                val statusCode = response.status.value
                if (statusCode in 200..299) {
                    Log.d("SupabaseOrderRepository", "Bulk notification sent successfully")
                } else {
                    Log.e("SupabaseOrderRepository", "Bulk notification failed: $statusCode")
                }
                
                httpClient.close()
            } catch (e: Exception) {
                Log.e("SupabaseOrderRepository", "Exception sending bulk notification", e)
            }
        }
    }

    private suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                client.postgrest["orders"].update(
                    {
                        set("status", status.toRemoteValue())
                        set("updated_at", isoNowUtc())
                    }
                ) {
                    filter { eq("id", orderId) }
                }
                refresh()
            }
        }

    suspend fun registerDevice(playerId: String) = withContext(Dispatchers.IO) {
        client.postgrest.rpc("register_device", mapOf("player_id" to playerId))
    }

    // --- DTO -> Entity mapping (store millis, not Instant) ---
    private fun SupabaseOrderDto.toEntity() = OrderEntity(
        id = id,
        storeId = storeId,
        orderNumber = number,
        type = type,
        chefTip = chefTip,
        status = status,
        createdBy = createdBy,
        createdAt = parseIsoToMillis(createdAt),
        updatedAt = parseIsoToMillis(updatedAt),
        parentOrderId = parentOrderId
    )

    private fun SupabaseOrderItemDto.toEntity(): OrderItemEntity {
        val extractedChefTip = modifiers?.get("chefTip")
        // Log to verify chefTip extraction from modifiers
        if (extractedChefTip != null && extractedChefTip.isNotBlank()) {
            Log.d("SupabaseOrderRepository", "Extracted chefTip from modifiers for item $name: '$extractedChefTip'")
        }
        return OrderItemEntity(
            id = id,
            orderId = orderId,
            name = name,
            size = size,
            vegFlag = vegFlag,
            quantity = quantity,
            chefTip = extractedChefTip  // Extract chefTip from modifiers JSONB
        )
    }
}

/* =======================
   Time helpers (API 23+)
   ======================= */

private fun isoNowUtc(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date(System.currentTimeMillis()))
}

private fun parseIsoToMillis(value: String): Long {
    val utc = java.util.TimeZone.getTimeZone("UTC")

    fun tryPattern(p: String): Long? = try {
        val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US).apply { timeZone = utc }
        sdf.parse(value)?.time
    } catch (_: Exception) {
        null
    }

    return tryPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        ?: tryPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ?: tryPattern("yyyy-MM-dd'T'HH:mm:ssX")
        ?: System.currentTimeMillis()
}
