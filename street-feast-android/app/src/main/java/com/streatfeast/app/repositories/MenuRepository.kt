package com.streatfeast.app.repositories

import android.content.Context
import android.util.Log
import com.streatfeast.app.models.Category
import com.streatfeast.app.models.MenuData
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.storage.CategoryEntity
import com.streatfeast.app.storage.FrequentItemEntity
import com.streatfeast.app.storage.ItemEntity
import com.streatfeast.app.storage.MenuMetadataEntity
import com.streatfeast.app.storage.MenuLocalDataSource
import com.streatfeast.app.storage.toCategory
import com.streatfeast.app.storage.toMenuItem
import com.streatfeast.app.utils.Constants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import java.util.concurrent.TimeUnit

@Serializable
data class GetMenuResponse(
    val categories: JsonObject? = null,
    val items: JsonObject? = null,
    val frequent_items: JsonObject? = null
)

class MenuRepository(
    private val client: SupabaseClient,
    private val localDataSource: MenuLocalDataSource,
    private val context: Context,
    private val storeId: String = Constants.DEFAULT_STORE_ID
) {
    private val httpClient by lazy {
        io.ktor.client.HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                }
            }
        }
    }
    
    private companion object {
        private const val MENU_TTL_MS = 10 * 60 * 1000L
    }
    
    private val realtimeMutex = Mutex()
    private var realtimeChannel: RealtimeChannel? = null
    private val fallbackRefreshEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Cache for fetched store UUID
    @Volatile
    private var cachedStoreId: String? = null

    private suspend fun fetchStoreId(): String? = runCatching<String?> {
        withContext(Dispatchers.IO) {
            val stores = client.postgrest["stores"]
                .select {
                    filter { eq("is_active", true) }
                    limit(1)
                }
                .decodeList<com.streatfeast.app.network.SupabaseStoreDto>()

            stores.firstOrNull()?.id
        }
    }.onFailure { e ->
        Log.e("MenuRepository", "Failed to fetch store ID from database", e)
    }.getOrNull()

    private suspend fun getStoreId(): String {
        if (cachedStoreId != null) {
            return cachedStoreId!!
        }
        val fetched = fetchStoreId()
        if (fetched != null) {
            cachedStoreId = fetched
            return fetched
        }
        return storeId
    }

    /**
     * Gets the store ID for fragment use.
     * Exposes the private getStoreId() method for fragments.
     */
    suspend fun getStoreIdForFragment(): String = getStoreId()

    suspend fun fetchMenu(storeId: String): Result<MenuData> = runCatching {
        withContext(Dispatchers.IO) {
            val currentStoreId = getStoreId()
            Log.d("MenuRepository", "fetchMenu() fetching menu for storeId=$currentStoreId")

            // Call get_menu RPC using HttpClient to get raw JSON
            val supabaseUrl = com.streatfeast.app.BuildConfig.SUPABASE_URL
            val anonKey = com.streatfeast.app.BuildConfig.SUPABASE_ANON_KEY
            
            val response = withRetry {
                httpClient.post("$supabaseUrl/rest/v1/rpc/get_menu") {
                    headers {
                        append(HttpHeaders.ContentType, "application/json")
                        append(HttpHeaders.Authorization, "Bearer $anonKey")
                        append("apikey", anonKey) // Required for PostgREST RPC calls
                        append(HttpHeaders.Accept, "application/json")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("p_store_id" to currentStoreId))
                }
            }
            
            // Check response status
            val statusCode = response.status.value
            val responseText = response.body<String>()
            
            Log.d("MenuRepository", "get_menu RPC response status: $statusCode")
            Log.d("MenuRepository", "get_menu RPC response body: $responseText")
            
            if (statusCode !in 200..299) {
                throw Exception("get_menu RPC failed with status $statusCode: $responseText")
            }
            
            // Parse JSON response
            val json = Json.parseToJsonElement(responseText).jsonObject
            
            // Parse categories
            val categoriesJson = json["categories"]?.jsonArray ?: emptyList()
            val categories = categoriesJson.map { cat ->
                val catObj = cat.jsonObject
                CategoryEntity(
                    id = catObj["id"]?.jsonPrimitive?.content ?: "",
                    storeId = currentStoreId,
                    name = catObj["name"]?.jsonPrimitive?.content ?: "",
                    isActive = catObj["is_active"]?.jsonPrimitive?.content?.toBoolean() ?: true,
                    createdAt = parseIsoToMillis(catObj["created_at"]?.jsonPrimitive?.content ?: ""),
                    updatedAt = parseIsoToMillis(catObj["updated_at"]?.jsonPrimitive?.content ?: "")
                )
            }

            // Parse items
            val itemsJson = json["items"]?.jsonArray ?: emptyList()
            val items = itemsJson.map { item ->
                val itemObj = item.jsonObject
                val sizesJson = itemObj["sizes"]
                val sizesString = when {
                    sizesJson == null -> "[]"
                    sizesJson is kotlinx.serialization.json.JsonNull -> "[]"
                    sizesJson is kotlinx.serialization.json.JsonArray -> {
                        // Convert JsonArray to JSON array string (not comma-separated)
                        val sizesList = sizesJson.map { it.jsonPrimitive.content }
                        Json.encodeToString(sizesList)
                    }
                    else -> "[]"
                }
                
                val flavorsJson = itemObj["flavors"]
                val flavorsString = when {
                    flavorsJson == null -> null
                    flavorsJson is kotlinx.serialization.json.JsonNull -> null
                    flavorsJson is kotlinx.serialization.json.JsonArray -> {
                        // Convert JsonArray to JSON array string (not comma-separated)
                        val flavorsList = flavorsJson.map { it.jsonPrimitive.content }
                        Json.encodeToString(flavorsList)
                    }
                    flavorsJson is kotlinx.serialization.json.JsonPrimitive -> {
                        if (flavorsJson.content.equals("null", ignoreCase = true)) {
                            null
                        } else {
                            // If it's a single string, wrap it in an array
                            Json.encodeToString(listOf(flavorsJson.content))
                        }
                    }
                    else -> null
                }
                
                ItemEntity(
                    id = itemObj["id"]?.jsonPrimitive?.content ?: "",
                    categoryId = itemObj["category_id"]?.jsonPrimitive?.content ?: "",
                    storeId = currentStoreId,
                    name = itemObj["name"]?.jsonPrimitive?.content ?: "",
                    sizes = sizesString,
                    vegFlag = itemObj["veg_flag"]?.jsonPrimitive?.content ?: "Veg",
                    flavors = flavorsString,
                    isActive = itemObj["is_active"]?.jsonPrimitive?.content?.toBoolean() ?: true,
                    createdAt = parseIsoToMillis(itemObj["created_at"]?.jsonPrimitive?.content ?: ""),
                    updatedAt = parseIsoToMillis(itemObj["updated_at"]?.jsonPrimitive?.content ?: "")
                )
            }

            // Parse frequent_items
            val frequentItemsJson = json["frequent_items"]?.jsonArray ?: emptyList()
            val frequentItems = frequentItemsJson.mapIndexed { index, fi ->
                val fiObj = fi.jsonObject
                FrequentItemEntity(
                    id = fiObj["id"]?.jsonPrimitive?.content
                        ?: "${currentStoreId}-${fiObj["item_id"]?.jsonPrimitive?.content ?: index}",
                    storeId = currentStoreId,
                    itemId = fiObj["item_id"]?.jsonPrimitive?.content ?: "",
                    orderIndex = fiObj["order_index"]?.jsonPrimitive?.content?.toInt() ?: index
                )
            }

            // Save to Room cache
            localDataSource.replaceMenuData(currentStoreId, categories, items, frequentItems)
            localDataSource.upsertMetadata(
                MenuMetadataEntity(
                    storeId = currentStoreId,
                    lastUpdatedAt = System.currentTimeMillis(),
                    dataHash = null
                )
            )

            Log.d("MenuRepository", "fetchMenu() completed - stored ${categories.size} categories, ${items.size} items, ${frequentItems.size} frequent items")

            // Convert to domain models
            val categoryModels = categories.map<CategoryEntity, Category> { it.toCategory() }
            val itemModels = items.map<ItemEntity, MenuItem> { it.toMenuItem() }
            val frequentItemIds = frequentItems.map { it.itemId }

            MenuData(
                categories = categoryModels,
                items = itemModels,
                frequentItemIds = frequentItemIds
            )
        }
    }.onFailure { e ->
        Log.e("MenuRepository", "Failed to fetch menu", e)
    }
    
    private suspend fun latestMenuTimestamp(storeId: String): Long {
        val meta = localDataSource.getMetadata(storeId)
        val metaTs = meta?.lastUpdatedAt ?: 0L
        val categoriesMax = localDataSource.getMaxUpdatedAtForCategories(storeId) ?: 0L
        val itemsMax = localDataSource.getMaxUpdatedAtForItems(storeId) ?: 0L
        return maxOf(metaTs, categoriesMax, itemsMax)
    }
    
    suspend fun maybeRefreshMenu(storeId: String): Result<MenuData?> = runCatching {
        val currentStoreId = getStoreId()
        val lastUpdated = latestMenuTimestamp(currentStoreId)
        val now = System.currentTimeMillis()
        if (now - lastUpdated < MENU_TTL_MS) {
            Log.d("MenuRepository", "Menu fresh (lastUpdated=$lastUpdated); skipping fetch")
            return@runCatching null
        }
        Log.d("MenuRepository", "Menu stale; fetching for storeId=$currentStoreId")
        fetchMenu(currentStoreId).getOrThrow()
    }

    fun subscribeToMenuUpdates(
        scope: CoroutineScope,
        storeId: String,
        callback: (MenuData) -> Unit
    ) {
        scope.launch {
            try {
                realtimeMutex.withLock {
                    // Clean up existing subscription if any
                    realtimeChannel?.let { channel ->
                        Log.d("MenuRepository", "Cleaning up existing menu realtime subscription")
                        runCatching { client.realtime?.removeChannel(channel) }
                            .onFailure { Log.e("MenuRepository", "Error removing realtime channel", it) }
                        realtimeChannel = null
                    }

                    val currentStoreId = getStoreId()
                    Log.d("MenuRepository", "Starting menu realtime subscription for storeId=$currentStoreId")

                    val realtime = client.realtime ?: run {
                        Log.e("MenuRepository", "Realtime is not available")
                        return@withLock
                    }

                    val channel = realtime.channel("menu-updates-$currentStoreId-${System.currentTimeMillis()}")

                    // Subscribe to categories changes
                    val categoriesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "categories"
                        filter("store_id", FilterOperator.EQ, currentStoreId)
                    }

                    // Subscribe to items changes
                    val itemsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "items"
                        filter("store_id", FilterOperator.EQ, currentStoreId)
                    }

                    // Subscribe to frequent_items changes
                    val frequentItemsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "frequent_items"
                        filter("store_id", FilterOperator.EQ, currentStoreId)
                    }

                    // Fallback refresh throttled to avoid bursts
                    scope.launch {
                        fallbackRefreshEvents
                            .debounce(750)
                            .collect {
                                Log.w("MenuRepository", "Fallback refresh triggered after realtime error/burst")
                                maybeRefreshMenu(currentStoreId)
                            }
                    }

                    // Collect changes and apply incrementally
                    scope.launch {
                        categoriesFlow
                            .catch { e ->
                                Log.e("MenuRepository", "Categories realtime error; scheduling fallback", e)
                                fallbackRefreshEvents.tryEmit(Unit)
                            }
                            .collect { action ->
                                handleCategoryAction(action, currentStoreId)
                            }
                    }

                    scope.launch {
                        itemsFlow
                            .catch { e ->
                                Log.e("MenuRepository", "Items realtime error; scheduling fallback", e)
                                fallbackRefreshEvents.tryEmit(Unit)
                            }
                            .collect { action ->
                                handleItemAction(action, currentStoreId)
                            }
                    }

                    scope.launch {
                        frequentItemsFlow
                            .catch { e ->
                                Log.e("MenuRepository", "Frequent items realtime error; scheduling fallback", e)
                                fallbackRefreshEvents.tryEmit(Unit)
                            }
                            .collect { action ->
                                handleFrequentItemAction(action, currentStoreId)
                            }
                    }

                    channel.subscribe()
                    realtimeChannel = channel

                    Log.d("MenuRepository", "Menu realtime subscription initialized")
                }
            } catch (e: Exception) {
                Log.e("MenuRepository", "Error in subscribeToMenuUpdates", e)
            }
        }
    }

    private suspend fun handleCategoryAction(action: PostgresAction, storeId: String) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = action.record ?: return
                    val id = record.getString("id") ?: return
                    val entity = CategoryEntity(
                        id = id,
                        storeId = record.getString("store_id") ?: storeId,
                        name = record.getString("name") ?: "",
                        isActive = record.getBoolean("is_active", true),
                        createdAt = parseTimestamp(record["created_at"]),
                        updatedAt = parseTimestamp(record["updated_at"])
                    )
                    localDataSource.upsertCategory(entity)
                }
                is PostgresAction.Delete -> {
                    val record = action.oldRecord ?: return
                    val id = record.getString("id") ?: return
                    localDataSource.deleteCategory(id, storeId)
                }
                else -> Unit
            }
        } catch (e: Exception) {
            Log.e("MenuRepository", "Failed to handle category realtime action; scheduling fallback", e)
            fallbackRefreshEvents.tryEmit(Unit)
        }
    }
    
    private suspend fun handleItemAction(action: PostgresAction, storeId: String) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = action.record ?: return
                    val id = record.getString("id") ?: return
                    val entity = ItemEntity(
                        id = id,
                        categoryId = record.getString("category_id") ?: "",
                        storeId = record.getString("store_id") ?: storeId,
                        name = record.getString("name") ?: "",
                        sizes = toJsonArrayString(record["sizes"]),
                        vegFlag = record.getString("veg_flag") ?: "Veg",
                        flavors = toNullableJsonArrayString(record["flavors"]),
                        isActive = record.getBoolean("is_active", true),
                        createdAt = parseTimestamp(record["created_at"]),
                        updatedAt = parseTimestamp(record["updated_at"])
                    )
                    localDataSource.upsertItem(entity)
                }
                is PostgresAction.Delete -> {
                    val record = action.oldRecord ?: return
                    val id = record.getString("id") ?: return
                    localDataSource.deleteItem(id, storeId)
                }
                else -> Unit
            }
        } catch (e: Exception) {
            Log.e("MenuRepository", "Failed to handle item realtime action; scheduling fallback", e)
            fallbackRefreshEvents.tryEmit(Unit)
        }
    }
    
    private suspend fun handleFrequentItemAction(action: PostgresAction, storeId: String) {
        try {
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val record = action.record ?: return
                    val itemId = record.getString("item_id") ?: return
                    val entityId = record.getString("id") ?: "$storeId-$itemId"
                    val orderIndex = record["order_index"]?.toString()?.toIntOrNull() ?: 0
                    val entity = FrequentItemEntity(
                        id = entityId,
                        storeId = record.getString("store_id") ?: storeId,
                        itemId = itemId,
                        orderIndex = orderIndex
                    )
                    localDataSource.upsertFrequentItem(entity)
                }
                is PostgresAction.Delete -> {
                    val record = action.oldRecord ?: return
                    val itemId = record.getString("item_id") ?: record.getString("id") ?: return
                    localDataSource.deleteFrequentItem(itemId, storeId)
                }
                else -> Unit
            }
        } catch (e: Exception) {
            Log.e("MenuRepository", "Failed to handle frequent_items realtime action; scheduling fallback", e)
            fallbackRefreshEvents.tryEmit(Unit)
        }
    }
    
    fun stopRealtime() {
        realtimeChannel?.let { channel ->
            Log.d("MenuRepository", "Stopping menu realtime subscription")
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    realtimeMutex.withLock {
                        runCatching { client.realtime?.removeChannel(channel) }
                            .onFailure { Log.e("MenuRepository", "Error stopping realtime subscription", it) }
                        realtimeChannel = null
                    }
                }
            } catch (e: Exception) {
                Log.e("MenuRepository", "Error stopping realtime subscription", e)
            }
        }
    }
}

// Helper function to parse ISO timestamp to milliseconds
private fun parseIsoToMillis(isoString: String): Long {
    return try {
        java.time.Instant.parse(isoString).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

// Simple retry with exponential backoff for network calls
private suspend fun <T> withRetry(
    attempts: Int = 3,
    initialDelayMs: Long = 300,
    block: suspend () -> T
): T {
    var delayMs = initialDelayMs
    repeat(attempts - 1) { attempt ->
        runCatching { return block() }
            .onFailure { Log.w("MenuRepository", "Retrying network call (attempt ${attempt + 2}/$attempts)", it) }
        delay(delayMs)
        delayMs *= 2
    }
    return block()
}

private fun Map<String, Any?>.getString(key: String): String? = this[key]?.toString()

private fun Map<String, Any?>.getBoolean(key: String, default: Boolean = true): Boolean =
    this[key]?.toString()?.toBoolean() ?: default

private fun parseTimestamp(value: Any?): Long {
    return when (value) {
        is Number -> value.toLong()
        is String -> parseIsoToMillis(value)
        else -> System.currentTimeMillis()
    }
}

private fun toJsonArrayString(value: Any?): String {
    return when (value) {
        null -> "[]"
        is kotlinx.serialization.json.JsonArray -> Json.encodeToString(value.map { it.jsonPrimitive.content })
        is List<*> -> Json.encodeToString(value.filterNotNull().map { it.toString() })
        is String -> {
            if (value.trim().startsWith("[")) value else Json.encodeToString(listOf(value))
        }
        else -> "[]"
    }
}

private fun toNullableJsonArrayString(value: Any?): String? {
    return when (value) {
        null -> null
        is kotlinx.serialization.json.JsonArray -> Json.encodeToString(value.map { it.jsonPrimitive.content })
        is List<*> -> Json.encodeToString(value.filterNotNull().map { it.toString() })
        is String -> {
            if (value.equals("null", ignoreCase = true)) null
            else if (value.trim().startsWith("[")) value else Json.encodeToString(listOf(value))
        }
        else -> null
    }
}

