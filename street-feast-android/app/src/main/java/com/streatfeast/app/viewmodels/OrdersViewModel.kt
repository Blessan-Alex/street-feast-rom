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
import kotlinx.coroutines.launch

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
            
            _isLoading.value = false
            
            result.onSuccess { orderId ->
                _successMessage.value = "Order created successfully"
                Log.d("OrdersViewModel", "Order created: $orderId")
                // Refresh orders to show the new order
                refresh()
            }.onFailure { e ->
                val errorMessage = e.message ?: "Failed to create order"
                _error.value = errorMessage
                Log.e("OrdersViewModel", "Failed to create order", e)
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

    fun alterOrder(
        orderId: String,
        items: List<OrderItem>,
        chefTip: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.alterOrder(orderId, items, chefTip)
            
            _isLoading.value = false
            
            result.onSuccess {
                _successMessage.value = "Order altered successfully"
                Log.d("OrdersViewModel", "Order altered: $orderId")
                // Refresh orders to show the updated order
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
