package com.streatfeast.app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.repositories.SupabaseOrderRepository
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val repository: SupabaseOrderRepository
) : ViewModel() {

    val newOrders: LiveData<List<Order>> =
        repository.observeOrders(OrderStatus.CREATED).asLiveData()

    val preparingOrders: LiveData<List<Order>> =
        repository.observeOrders(OrderStatus.IN_KITCHEN).asLiveData()

    val readyOrders: LiveData<List<Order>> =
        repository.observeOrders(OrderStatus.PREPARED).asLiveData()

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
