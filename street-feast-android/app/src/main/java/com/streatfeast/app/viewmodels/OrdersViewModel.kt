package com.streatfeast.app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.repositories.SupabaseOrderRepository
import com.streatfeast.app.repositories.MarkItemPreparedResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class OrdersViewModel(
    private val repository: SupabaseOrderRepository
) : ViewModel() {

    private val _selectedOrderTypeFilter = MutableLiveData<OrderType?>(OrderType.DINE_IN)
    val selectedOrderTypeFilter: LiveData<OrderType?> = _selectedOrderTypeFilter

    val newOrders: LiveData<List<Order>> = _selectedOrderTypeFilter.switchMap { filter ->
        Log.d("OrdersViewModel", "Filter changed for newOrders: $filter")
        if (filter != null) {
            Log.d("OrdersViewModel", "Observing filtered orders: status=CREATED, type=$filter")
            repository.observeOrdersByType(OrderStatus.CREATED, filter).asLiveData()
        } else {
            Log.d("OrdersViewModel", "Observing all orders: status=CREATED")
            repository.observeOrders(OrderStatus.CREATED).asLiveData()
        }
    }

    val preparingOrders: LiveData<List<Order>> = _selectedOrderTypeFilter.switchMap { filter ->
        Log.d("OrdersViewModel", "Filter changed for preparingOrders: $filter")
        if (filter != null) {
            Log.d("OrdersViewModel", "Observing filtered orders: status=IN_KITCHEN, type=$filter")
            repository.observeOrdersByType(OrderStatus.IN_KITCHEN, filter).asLiveData()
        } else {
            Log.d("OrdersViewModel", "Observing all orders: status=IN_KITCHEN")
            repository.observeOrders(OrderStatus.IN_KITCHEN).asLiveData()
        }
    }

    val readyOrders: LiveData<List<Order>> =
        repository.observeOrders(OrderStatus.PREPARED).asLiveData()

    val deliveredOrders: LiveData<List<Order>> =
        repository.observeOrders(OrderStatus.DELIVERED).asLiveData()

    // Editable orders: Created, Accepted, InKitchen, Prepared (can be modified)
    val editableOrders: LiveData<List<Order>> =
        repository.observeEditableOrders().asLiveData()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _newOrderDetected = MutableLiveData<Pair<String, Int?>>()
    val newOrderDetected: LiveData<Pair<String, Int?>> = _newOrderDetected

    private val _orderAccepted = MutableLiveData<String>()   // order ID
    val orderAccepted: LiveData<String> get() = _orderAccepted

    init {
        // Start realtime subscription with callback for new orders
        Log.d("OrdersViewModel", "Initializing OrdersViewModel - starting realtime subscription")
        viewModelScope.launch {
            repository.startRealtime(viewModelScope) { orderId, orderNumber ->
                Log.d("OrdersViewModel", "New order callback invoked: orderId=$orderId, orderNumber=$orderNumber")
                _newOrderDetected.postValue(Pair(orderId, orderNumber))
            }
            Log.d("OrdersViewModel", "Realtime subscription started with callback")
        }
    }

    fun setOrderTypeFilter(type: OrderType?) {
        _selectedOrderTypeFilter.value = type
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.refresh() }
                .onFailure { _error.value = it.message ?: "Failed to refresh orders" }
            _isLoading.value = false
        }
    }

    fun acceptOrder(orderId: String) {
        performAction("Order accepted") {
            val result = repository.acceptOrder(orderId)
            result.onSuccess {
                _orderAccepted.postValue(orderId)
            }
            result
        }
    }

    fun markPrepared(orderId: String) {
        performAction("Order marked as prepared") { repository.markPrepared(orderId) }
    }

    fun markDelivered(orderId: String) {
        performAction("Order marked as delivered") { repository.markDelivered(orderId) }
    }

    fun acceptAllOrders() {
        performBulkAction("All orders accepted") {
            repository.acceptAllOrders()
        }
    }

    fun markAllPrepared() {
        performBulkAction("All orders marked as prepared") {
            repository.markAllPrepared()
        }
    }

    fun markAllDelivered() {
        performBulkAction("All orders marked as delivered") {
            repository.markAllDelivered()
        }
    }

    suspend fun getActiveOrderForTable(tableNumber: Int): Order? =
        repository.getActiveOrderForTable(tableNumber)

    fun markItemPrepared(
        orderId: String,
        itemId: String,
        onComplete: (Result<MarkItemPreparedResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.markItemPrepared(orderId, itemId)
            result.onFailure { _error.postValue(it.message ?: "Failed to mark item prepared") }
            onComplete(result)
        }
    }

    /**
     * Creates a brand-new order. Do not use for add/alter flows.
     * The isEdit flag is deprecated and ignored for behaviour selection.
     */
    fun createOrder(
        orderType: OrderType,
        items: List<OrderItem>,
        tableNumber: Int? = null,
        licensePlate: String? = null,
        chefTip: String = "",
        isEdit: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.createOrder(orderType, items, tableNumber, licensePlate, chefTip, isEdit)
            
            result.onSuccess { orderId ->
                _successMessage.value = "Order created successfully"
                Log.d("OrdersViewModel", "Order created: $orderId")
                // Refresh orders to show the new order
                refresh()
                _isLoading.value = false
            }.onFailure { e ->
                val occupiedDineIn = orderType == OrderType.DINE_IN &&
                    tableNumber != null &&
                    e.message?.contains("already occupied", ignoreCase = true) == true

                if (occupiedDineIn) {
                    val errorMessage = "Table $tableNumber already has an active order. Use Add Items or Alter Order."
                    _error.value = errorMessage
                    Log.e("OrdersViewModel", errorMessage, e)
                } else {
                    val errorMessage = e.message ?: "Failed to create order"
                    _error.value = errorMessage
                    Log.e("OrdersViewModel", "Failed to create order", e)
                }
                _isLoading.value = false
            }
        }
    }

    fun addItemsToOrder(
        parentOrderId: String,
        items: List<OrderItem>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.addItemsToOrder(parentOrderId, items)
            
            _isLoading.value = false
            
            result.onSuccess { orderId ->
                _successMessage.value = "Items added to order successfully"
                Log.d("OrdersViewModel", "Items added to order: $orderId")
                // Refresh orders to show the updated order
                refresh()
            }.onFailure { e ->
                val errorMessage = e.message ?: "Failed to add items to order"
                _error.value = errorMessage
                Log.e("OrdersViewModel", "Failed to add items to order", e)
            }
        }
    }

    /**
     * Add items by reusing the alter flow (alterOrderV2).
     * This replaces the order (cancel + recreate) and avoids table-occupied checks.
     */
    fun addItemsByAlterFlow(
        parentOrderId: String,
        baseItems: List<OrderItem>,
        newItems: List<OrderItem>,
        chefTip: String?
    ) {
        val combined = baseItems + newItems
        alterOrder(parentOrderId, combined, chefTip)
    }

    fun alterOrder(
        orderId: String,
        items: List<OrderItem>,
        chefTip: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Use alterOrderV2 which cancels old order and creates new one (atomic operation)
            val result = repository.alterOrderV2(orderId, items, chefTip)
            
            _isLoading.value = false
            
            result.onSuccess { (newOrderId, newOrderNumber) ->
                _successMessage.value = "Order #$newOrderNumber updated successfully"
                Log.d("OrdersViewModel", "Order altered: old=$orderId, new=$newOrderId (#$newOrderNumber)")
                
                // Force refresh all order lists to ensure canceled orders are filtered out
                // The repository should already filter canceled orders, but force refresh
                refresh()
                
                // Also trigger a manual refresh after a small delay to ensure DB transaction completes
                kotlinx.coroutines.delay(500)
                refresh()
            }.onFailure { e ->
                val errorMessage = e.message ?: "Failed to alter order"
                _error.value = errorMessage
                Log.e("OrdersViewModel", "Failed to alter order", e)
            }
        }
    }

    fun updateOrderItem(
        itemId: String,
        quantity: Int? = null,
        size: String? = null,
        chefTip: String? = null
    ) {
        performAction("Order item updated successfully") {
            repository.updateOrderItem(itemId, quantity, size, chefTip)
        }
    }

    fun deleteOrderItem(itemId: String) {
        performAction("Order item deleted successfully") {
            repository.deleteOrderItem(itemId)
        }
    }

    private fun performAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = block()
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = successMessage
            }.onFailure {
                _error.value = it.message ?: "Operation failed"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun performBulkAction(successMessage: String, block: suspend () -> Result<Int>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = block()
            _isLoading.value = false

            result.onSuccess { count ->
                if (count > 0) {
                    _successMessage.value = successMessage
                } else {
                    _error.value = "No orders to update"
                }
            }.onFailure {
                _error.value = it.message ?: "Operation failed"
            }
        }
    }
}

class OrdersViewModelFactory(
    private val repository: SupabaseOrderRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
            return OrdersViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
