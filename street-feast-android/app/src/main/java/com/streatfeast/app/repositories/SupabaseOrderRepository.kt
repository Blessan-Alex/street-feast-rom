package com.streatfeast.app.repositories

import android.content.Context
import android.util.Log
import com.streatfeast.app.BuildConfig
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.network.OccupiedTableResult
import com.streatfeast.app.network.SupabaseOrderDto
import com.streatfeast.app.network.SupabaseOrderItemDto
import com.streatfeast.app.network.SupabaseStoreDto
import com.streatfeast.app.storage.OrderEntity
import com.streatfeast.app.storage.OrderItemEntity
import com.streatfeast.app.storage.OrderLocalDataSource
import com.google.gson.Gson
import com.streatfeast.app.utils.Constants
import com.streatfeast.app.utils.NotificationHelper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
import kotlinx.coroutines.SupervisorJob
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
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repoDispatcher = repoScope.coroutineContext
    private var realtimeChannel: RealtimeChannel? = null
    private val realtimeMutex = Mutex()

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
        withContext(repoDispatcher) {
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

    fun observeOrdersByType(status: OrderStatus, type: OrderType): Flow<List<Order>> = flow {
        val currentStoreId = getStoreId()
        Log.d("SupabaseOrderRepository", "observeOrdersByType: storeId=$currentStoreId, status=${status.toRemoteValue()}, type=${type.toRemoteValue()}")
        emitAll(localDataSource.observeOrdersByType(currentStoreId, status, type))
    }

    fun observeEditableOrders(): Flow<List<Order>> = flow {
        val currentStoreId = getStoreId()
        val editableStatuses = listOf(
            OrderStatus.CREATED,
            OrderStatus.ACCEPTED,
            OrderStatus.IN_KITCHEN,
            OrderStatus.PREPARED
        )
        Log.d("SupabaseOrderRepository", "observeEditableOrders: storeId=$currentStoreId, statuses=${editableStatuses.map { it.toRemoteValue() }}")
        emitAll(localDataSource.observeOrdersByStatuses(currentStoreId, editableStatuses))
    }

    suspend fun refresh() {
        Log.d("SupabaseOrderRepository", "refresh() called")
        runCatching {
            withContext(repoDispatcher) {
                val currentStoreId = getStoreId()
                Log.d("SupabaseOrderRepository", "refresh() fetching orders for storeId=$currentStoreId")

                val orders = client.postgrest["orders"]
                    .select {
                        filter { eq("store_id", currentStoreId) }
                        order(column = "created_at", order = PgOrder.DESCENDING)
                    }
                    .decodeList<SupabaseOrderDto>()

                Log.d("SupabaseOrderRepository", "refresh() fetched ${orders.size} orders")

                // Log order status breakdown for debugging
                val statusBreakdown = orders.groupBy { it.status }.mapValues { it.value.size }
                Log.d("SupabaseOrderRepository", "Order status breakdown: $statusBreakdown")
                
                // Log order numbers and statuses
                val orderDetails = orders.take(10).map { "${it.number}(${it.status})" }
                Log.d("SupabaseOrderRepository", "Sample orders: ${orderDetails.joinToString(", ")}")

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
                // Log order types for debugging
                val orderTypes = orders.groupBy { it.type }.mapKeys { it.key ?: "null" }
                Log.d("SupabaseOrderRepository", "Order types in refresh: $orderTypes")

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
            if (callbacks.contains(callback)) {
                Log.d("SupabaseOrderRepository", "Callback already registered; skipping")
                return
            }
            callbacks.add(callback)
            Log.d("SupabaseOrderRepository", "Callback registered. Total callbacks: ${callbacks.size}")
        }
    }

    suspend fun removeCallback(callback: (orderId: String, orderNumber: Int?) -> Unit) {
        callbacksMutex.withLock {
            callbacks.remove(callback)
            Log.d("SupabaseOrderRepository", "Callback removed. Total callbacks: ${callbacks.size}")
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
        onNewOrder?.let { callback -> addCallback(callback) }

        val currentStoreId = getStoreId()
        Log.d("SupabaseOrderRepository", "Starting realtime subscription for storeId=$currentStoreId")

        runCatching {
            realtimeMutex.withLock {
                // If channel already exists, skip rejoin
                if (realtimeChannel != null) {
                    Log.d("SupabaseOrderRepository", "Realtime channel already exists. Callback registered.")
                    return@withLock
                }

                // Clean up any lingering channel
                realtimeChannel?.let { existing ->
                    runCatching { client.realtime.removeChannel(existing) }
                        .onFailure { Log.e("SupabaseOrderRepository", "Error removing old channel", it) }
                    realtimeChannel = null
                }

                val channel = client.realtime.channel("realtime:public:orders-${currentStoreId}-${System.currentTimeMillis()}")

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "orders"
                    filter("store_id", FilterOperator.EQ, currentStoreId)
                }

                repoScope.launch {
                    changeFlow.collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                val orderId = action.record["id"]?.toString() ?: ""
                                val orderNumber = (action.record["number"] as? Number)?.toInt()
                                val orderType = action.record["type"]?.toString() ?: "Unknown"

                                val isNewOrder = orderIdsMutex.withLock {
                                    if (orderId.isNotEmpty() && orderId !in previousOrderIds) {
                                        previousOrderIds.add(orderId)
                                        true
                                    } else {
                                        false
                                    }
                                }

                                if (isNewOrder) {
                                    if (orderNumber != null) {
                                        NotificationHelper.showNewOrderNotification(context, orderNumber)
                                    }
                                    callbacksMutex.withLock {
                                        callbacks.forEach { callback ->
                                            runCatching { callback(orderId, orderNumber) }
                                                .onFailure { Log.e("SupabaseOrderRepository", "Error invoking callback", it) }
                                        }
                                    }
                                }

                                Log.d("SupabaseOrderRepository", "Calling refresh() after realtime INSERT event for order type: $orderType")
                                refresh()
                                Log.d("SupabaseOrderRepository", "Refresh() call completed for INSERT event")
                            }
                            is PostgresAction.Update -> {
                                val record = action.record
                                val orderId = record?.get("id")?.toString()
                                val orderNumber = (record?.get("number") as? Number)?.toInt()
                                val newStatus = record?.get("status")?.toString()
                                Log.d("SupabaseOrderRepository", "Realtime UPDATE event - Order #$orderNumber (ID: $orderId), Status: $newStatus")
                                Log.d("SupabaseOrderRepository", "Calling refresh() after realtime UPDATE event")
                                refresh()
                                Log.d("SupabaseOrderRepository", "Refresh() call completed for UPDATE event")
                            }
                            is PostgresAction.Delete -> {
                                Log.d("SupabaseOrderRepository", "Realtime DELETE event received")
                                Log.d("SupabaseOrderRepository", "Calling refresh() after realtime DELETE event")
                                refresh()
                                Log.d("SupabaseOrderRepository", "Refresh() call completed for DELETE event")
                            }
                            is PostgresAction.Select -> {
                                Log.d("SupabaseOrderRepository", "Realtime select: ${action.record}")
                            }
                        }
                    }
                }

                channel.subscribe()
                Log.d("SupabaseOrderRepository", "Realtime channel subscribed successfully")
                realtimeChannel = channel

                repoScope.launch {
                    Log.d("SupabaseOrderRepository", "Performing initial sync after realtime subscription")
                    refresh()
                }
            }
        }.onFailure { e ->
            Log.e("SupabaseOrderRepository", "Failed to start realtime subscription", e)
        }
    }

    suspend fun stopRealtime() {
        realtimeChannel?.let { ch ->
            Log.d("SupabaseOrderRepository", "Stopping realtime subscription")
            repoScope.launch {
                realtimeMutex.withLock {
                    runCatching { ch.unsubscribe() }
                        .onFailure { Log.e("SupabaseOrderRepository", "Failed to unsubscribe realtime", it) }
                    runCatching { client.realtime.removeChannel(ch) }
                        .onFailure { Log.e("SupabaseOrderRepository", "Failed to remove realtime channel", it) }
                    realtimeChannel = null
                    callbacksMutex.withLock {
                        callbacks.clear()
                        Log.d("SupabaseOrderRepository", "Callbacks cleared")
                    }
                }
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
        withContext(repoDispatcher) {
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
        withContext(repoDispatcher) {
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
            withContext(repoDispatcher) {
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

    suspend fun registerDevice(playerId: String) = withContext(repoDispatcher) {
        client.postgrest.rpc("register_device", mapOf("player_id" to playerId))
    }

    /**
     * Creates a new order via orders_upsert RPC.
     * Validates table availability for DINE_IN orders and license plate format for EAT_AWAY orders.
     */
    suspend fun createOrder(
        orderType: OrderType,
        items: List<com.streatfeast.app.models.OrderItem>,
        tableNumber: Int? = null,
        licensePlate: String? = null,
        chefTip: String = "",
        isEdit: Boolean = false
    ): Result<String> = runCatching {
        withContext(repoDispatcher) {
            val currentStoreId = getStoreId()
            Log.d("SupabaseOrderRepository", "createOrder: type=${orderType.toRemoteValue()}, table=$tableNumber, license=$licensePlate, items=${items.size}")

            // Get current user ID
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                error("User not authenticated")
            }

            var sanitizedLicensePlate = licensePlate
                ?.filter { it.isDigit() }
                ?.take(4)
            // Validate based on order type
            when (orderType) {
                OrderType.DINE_IN -> {
                    if (tableNumber == null || tableNumber !in 1..7) {
                        error("DINE_IN orders require a table number between 1 and 7")
                    }
                    if (!isEdit) {
                        // Check table availability only for new orders
                        val isAvailable = checkTableAvailability(tableNumber, currentStoreId)
                        if (!isAvailable) {
                            error("Table $tableNumber is already occupied")
                        }
                    }
                }
                OrderType.EAT_AWAY -> {
                    if (sanitizedLicensePlate.isNullOrEmpty() || !sanitizedLicensePlate.matches(Regex("^\\d{4}$"))) {
                        error("EAT_AWAY orders require a 4-digit license plate")
                    }
                }
                OrderType.PARCEL -> {
                    // No validation needed for PARCEL
                }
            }

            // Prepare order JSONB
            val now = isoNowUtc()
            val orderJson = buildMap<String, Any?> {
                put("type", orderType.toRemoteValue())
                put("chef_tip", chefTip.trim())
                put("status", "Created")
                put("created_by", userId)
                put("parent_order_id", null)
                put("created_at", now)
                put("updated_at", now)
                
                // Add table_number or license_plate based on order type
                if (orderType == OrderType.DINE_IN && tableNumber != null) {
                    put("table_number", tableNumber)
                } else if (orderType == OrderType.EAT_AWAY && sanitizedLicensePlate != null) {
                    put("license_plate", sanitizedLicensePlate)
                }
            }

            // Prepare items JSONB array
            val itemsJson = items.map { item ->
                buildMap<String, Any?> {
                    put("id", item.id.ifEmpty { java.util.UUID.randomUUID().toString() })
                    put("sku", item.itemId)
                    put("name", item.nameSnapshot)
                    put("size", item.size)
                    put("veg_flag", item.vegFlagSnapshot)
                    put("quantity", item.qty)
                    put("modifiers", mapOf("chefTip" to item.chefTip))
                }
            }

            // Call orders_upsert RPC using HttpClient for JSONB support
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            
            val httpClient = HttpClient(OkHttp) {
                // Remove ContentNegotiation - we'll serialize manually with Gson
            }
            
            val gson = Gson()
            val payload = mapOf(
                "p_store_id" to currentStoreId,
                "p_order" to orderJson,
                "p_items" to itemsJson,
                "p_actor_id" to userId
            )
            
            // Serialize payload to JSON string manually
            val payloadJson = gson.toJson(payload)
            
            val response = httpClient.post("$supabaseUrl/rest/v1/rpc/orders_upsert") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer $anonKey")
                    append("apikey", anonKey)
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(payloadJson) // Send as String instead of object
            }
            
            val orderId = response.body<String>()
            httpClient.close()

            Log.d("SupabaseOrderRepository", "createOrder: Successfully created order $orderId")
            
            // Refresh local data
            refresh()
            
            orderId
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "createOrder failed", e)
    }

    /**
     * Checks if a table is available for a DINE_IN order.
     * Uses get_occupied_tables() for consistency with UI display.
     */
    private suspend fun checkTableAvailability(tableNumber: Int, storeId: String): Boolean = runCatching {
        withContext(repoDispatcher) {
            val occupiedTables = client.postgrest.rpc(
                "get_occupied_tables",
                mapOf("p_store_id" to storeId)
            ).decodeList<OccupiedTableResult>()
            
            val isOccupied = occupiedTables.any { it.tableNumber == tableNumber }
            val isAvailable = !isOccupied
            
            Log.d("SupabaseOrderRepository", "checkTableAvailability: table=$tableNumber, available=$isAvailable, storeId=$storeId, occupiedTables=${occupiedTables.map { it.tableNumber }}")
            isAvailable
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "checkTableAvailability failed for table=$tableNumber", e)
    }.getOrElse { e ->
        if (e is kotlinx.coroutines.CancellationException) {
            Log.w("SupabaseOrderRepository", "checkTableAvailability cancellation; treating as available to avoid false block")
            true
        } else {
            Log.w("SupabaseOrderRepository", "checkTableAvailability defaulting to false (occupied) due to error")
            false
        }
    }

    /**
     * Gets list of occupied tables for a store.
     * Returns tables with status IN ('Created', 'Accepted', 'InKitchen', 'Prepared').
     */
    suspend fun getOccupiedTables(storeId: String): Result<List<Int>> = runCatching {
        withContext(repoDispatcher) {
            val result = client.postgrest.rpc(
                "get_occupied_tables",
                mapOf("p_store_id" to storeId)
            ).decodeList<OccupiedTableResult>()
            
            val occupiedTables = result.map { it.tableNumber }
            
            Log.d("SupabaseOrderRepository", "getOccupiedTables: $occupiedTables")
            occupiedTables
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "Failed to fetch occupied tables", e)
    }

    /**
     * Gets the store ID for fragment use.
     * Exposes the private getStoreId() method for fragments.
     */
    suspend fun getStoreIdForFragment(): String = getStoreId()

    /**
     * Adds items to an existing order by creating a child order with parent_order_id.
     * Fetches the parent order to get table/license information.
     */
    suspend fun addItemsToOrder(
        parentOrderId: String,
        items: List<com.streatfeast.app.models.OrderItem>
    ): Result<String> = runCatching {
        withContext(repoDispatcher) {
            val currentStoreId = getStoreId()
            Log.d("SupabaseOrderRepository", "addItemsToOrder: parentOrderId=$parentOrderId, items=${items.size}")

            // Get current user ID
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                error("User not authenticated")
            }

            // Fetch parent order to get table/license
            val parentOrder = client.postgrest["orders"]
                .select {
                    filter { eq("id", parentOrderId) }
                }
                .decodeSingle<SupabaseOrderDto>()

            if (parentOrder.storeId != currentStoreId) {
                error("Parent order does not belong to current store")
            }

            val orderType = parentOrder.type?.let { 
                try {
                    OrderType.fromString(it)
                } catch (e: Exception) {
                    null
                }
            } ?: OrderType.DINE_IN // Default fallback

            // Prepare order JSONB
            val now = isoNowUtc()
            val orderJson = buildMap<String, Any?> {
                put("type", orderType.toRemoteValue())
                put("chef_tip", parentOrder.chefTip ?: "")
                put("status", "Created")
                put("created_by", userId)
                put("parent_order_id", parentOrderId)
                put("created_at", now)
                put("updated_at", now)
                
                // Copy table_number or license_plate from parent order
                parentOrder.tableNumber?.let { put("table_number", it) }
                parentOrder.licensePlate?.let { put("license_plate", it) }
            }

            // Prepare items JSONB array
            val itemsJson = items.map { item ->
                buildMap<String, Any?> {
                    put("id", item.id.ifEmpty { java.util.UUID.randomUUID().toString() })
                    put("sku", item.itemId)
                    put("name", item.nameSnapshot)
                    put("size", item.size)
                    put("veg_flag", item.vegFlagSnapshot)
                    put("quantity", item.qty)
                    put("modifiers", mapOf("chefTip" to item.chefTip))
                }
            }

            // Call orders_upsert RPC using HttpClient for JSONB support
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            
            val httpClient = HttpClient(OkHttp) {
                // Remove ContentNegotiation - we'll serialize manually with Gson
            }
            
            val gson = Gson()
            val payload = mapOf(
                "p_store_id" to currentStoreId,
                "p_order" to orderJson,
                "p_items" to itemsJson,
                "p_actor_id" to userId
            )
            
            // Serialize payload to JSON string manually
            val payloadJson = gson.toJson(payload)
            
            val response = httpClient.post("$supabaseUrl/rest/v1/rpc/orders_upsert") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer $anonKey")
                    append("apikey", anonKey)
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(payloadJson) // Send as String instead of object
            }
            
            val orderId = response.body<String>()
            httpClient.close()

            Log.d("SupabaseOrderRepository", "addItemsToOrder: Successfully created child order $orderId")
            
            // Refresh local data
            refresh()
            
            orderId
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "addItemsToOrder failed", e)
    }

    /**
     * Alters an existing order by replacing its items.
     * Validates order status and user permissions.
     */
    suspend fun alterOrder(
        orderId: String,
        items: List<com.streatfeast.app.models.OrderItem>,
        chefTip: String? = null
    ): Result<Unit> = runCatching {
        withContext(repoDispatcher) {
            Log.d("SupabaseOrderRepository", "alterOrder: orderId=$orderId, items=${items.size}, chefTip=${chefTip?.take(20)}")

            // Get current user ID
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                error("User not authenticated")
            }

            // Prepare items JSONB array
            val itemsJson = items.map { item ->
                buildMap<String, Any?> {
                    put("id", item.id.ifEmpty { java.util.UUID.randomUUID().toString() })
                    put("sku", item.itemId)
                    put("name", item.nameSnapshot)
                    put("size", item.size)
                    put("veg_flag", item.vegFlagSnapshot)
                    put("quantity", item.qty)
                    put("modifiers", mapOf("chefTip" to item.chefTip))
                }
            }

            // Call alter_order RPC using HttpClient for JSONB support
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            
            val httpClient = HttpClient(OkHttp) {
                // Remove ContentNegotiation - we'll serialize manually with Gson
            }
            
            val gson = Gson()
            val payload = buildMap<String, Any?> {
                put("p_order_id", orderId)
                put("p_items", itemsJson)
                put("p_actor_id", userId)
                if (chefTip != null) {
                    put("p_chef_tip", chefTip)
                }
            }
            
            // Serialize payload to JSON string manually
            val payloadJson = gson.toJson(payload)
            
            val response = httpClient.post("$supabaseUrl/rest/v1/rpc/alter_order") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer $anonKey")
                    append("apikey", anonKey)
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(payloadJson) // Send as String instead of object
            }
            
            // Response should be the order ID (UUID as string)
            val resultOrderId = response.body<String>()
            httpClient.close()

            Log.d("SupabaseOrderRepository", "alterOrder: Successfully altered order $resultOrderId")
            
            // Refresh local data
            refresh()
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "alterOrder failed", e)
        // Improve error messages to be status-specific
        val errorMessage = when {
            e.message?.contains("Cannot alter order with status: Delivered") == true -> 
                "Cannot modify a delivered order"
            e.message?.contains("Cannot alter order with status: Closed") == true -> 
                "Cannot modify a closed order"
            e.message?.contains("Cannot alter order with status: Canceled") == true -> 
                "Cannot modify a canceled order"
            e.message?.contains("does not exist") == true -> 
                "Order not found"
            e.message?.contains("permission") == true -> 
                "You do not have permission to modify this order"
            else -> e.message ?: "Failed to alter order"
        }
        throw Exception(errorMessage, e)
    }

    /**
     * Updates an individual order item (quantity, size, chef tip).
     * Validates order status and user permissions.
     */
    suspend fun updateOrderItem(
        itemId: String,
        quantity: Int? = null,
        size: String? = null,
        chefTip: String? = null
    ): Result<Unit> = runCatching {
        withContext(repoDispatcher) {
            Log.d("SupabaseOrderRepository", "updateOrderItem: itemId=$itemId, quantity=$quantity, size=$size, chefTip=${chefTip?.take(20)}")

            // Get current user ID
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                error("User not authenticated")
            }

            // Call update_order_item RPC using HttpClient
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            
            val httpClient = HttpClient(OkHttp) {
                // Remove ContentNegotiation - we'll serialize manually with Gson
            }
            
            val gson = Gson()
            val payload = buildMap<String, Any?> {
                put("p_item_id", itemId)
                put("p_actor_id", userId)
                if (quantity != null) {
                    put("p_quantity", quantity)
                }
                if (size != null) {
                    put("p_size", size)
                }
                if (chefTip != null) {
                    put("p_chef_tip", chefTip)
                }
            }
            
            // Serialize payload to JSON string manually
            val payloadJson = gson.toJson(payload)
            
            val response = httpClient.post("$supabaseUrl/rest/v1/rpc/update_order_item") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer $anonKey")
                    append("apikey", anonKey)
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(payloadJson) // Send as String instead of object
            }
            
            httpClient.close()

            Log.d("SupabaseOrderRepository", "updateOrderItem: Successfully updated item $itemId")
            
            // Refresh local data
            refresh()
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "updateOrderItem failed", e)
        // Improve error messages
        val errorMessage = when {
            e.message?.contains("Cannot modify item in order with status: Delivered") == true -> 
                "Cannot modify item in a delivered order"
            e.message?.contains("Cannot modify item in order with status: Closed") == true -> 
                "Cannot modify item in a closed order"
            e.message?.contains("Cannot modify item in order with status: Canceled") == true -> 
                "Cannot modify item in a canceled order"
            e.message?.contains("not found") == true -> 
                "Order item not found"
            e.message?.contains("permission") == true -> 
                "You do not have permission to modify this order item"
            else -> e.message ?: "Failed to update order item"
        }
        throw Exception(errorMessage, e)
    }

    /**
     * Deletes an individual order item.
     * Validates order status and user permissions.
     */
    suspend fun deleteOrderItem(
        itemId: String
    ): Result<Unit> = runCatching {
        withContext(repoDispatcher) {
            Log.d("SupabaseOrderRepository", "deleteOrderItem: itemId=$itemId")

            // Get current user ID
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                error("User not authenticated")
            }

            // Call delete_order_item RPC using HttpClient
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            
            val httpClient = HttpClient(OkHttp) {
                // Remove ContentNegotiation - we'll serialize manually with Gson
            }
            
            val gson = Gson()
            val payload = mapOf(
                "p_item_id" to itemId,
                "p_actor_id" to userId
            )
            
            // Serialize payload to JSON string manually
            val payloadJson = gson.toJson(payload)
            
            val response = httpClient.post("$supabaseUrl/rest/v1/rpc/delete_order_item") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer $anonKey")
                    append("apikey", anonKey)
                    append(HttpHeaders.Accept, "application/json")
                }
                contentType(ContentType.Application.Json)
                setBody(payloadJson) // Send as String instead of object
            }
            
            httpClient.close()

            Log.d("SupabaseOrderRepository", "deleteOrderItem: Successfully deleted item $itemId")
            
            // Refresh local data
            refresh()
        }
    }.onFailure { e ->
        Log.e("SupabaseOrderRepository", "deleteOrderItem failed", e)
        // Improve error messages
        val errorMessage = when {
            e.message?.contains("Cannot delete item from order with status: Delivered") == true -> 
                "Cannot delete item from a delivered order"
            e.message?.contains("Cannot delete item from order with status: Closed") == true -> 
                "Cannot delete item from a closed order"
            e.message?.contains("Cannot delete item from order with status: Canceled") == true -> 
                "Cannot delete item from a canceled order"
            e.message?.contains("Cannot delete the last item") == true -> 
                "Cannot delete the last item in an order"
            e.message?.contains("not found") == true -> 
                "Order item not found"
            e.message?.contains("permission") == true -> 
                "You do not have permission to delete this order item"
            else -> e.message ?: "Failed to delete order item"
        }
        throw Exception(errorMessage, e)
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
        parentOrderId = parentOrderId,
        tableNumber = tableNumber,
        licensePlate = licensePlate
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
