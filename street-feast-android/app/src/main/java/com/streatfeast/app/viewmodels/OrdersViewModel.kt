package com.streatfeast.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streatfeast.app.models.Order
import com.streatfeast.app.repositories.MockOrderRepository
import kotlinx.coroutines.launch

class OrdersViewModel : ViewModel() {
    
    private val repository = MockOrderRepository()
    
    // LiveData for new orders
    private val _newOrders = MutableLiveData<List<Order>>()
    val newOrders: LiveData<List<Order>> = _newOrders
    
    // LiveData for preparing orders
    private val _preparingOrders = MutableLiveData<List<Order>>()
    val preparingOrders: LiveData<List<Order>> = _preparingOrders
    
    // LiveData for ready orders
    private val _readyOrders = MutableLiveData<List<Order>>()
    val readyOrders: LiveData<List<Order>> = _readyOrders
    
    // Loading states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error states
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Success messages
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    /**
     * Load new orders (Created status)
     */
    fun loadNewOrders() {
        viewModelScope.launch {
            try {
                val orders = repository.getNewOrders()
                _newOrders.value = orders
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load new orders"
            }
        }
    }
    
    /**
     * Load preparing orders (InKitchen status)
     */
    fun loadPreparingOrders() {
        viewModelScope.launch {
            try {
                val orders = repository.getPreparingOrders()
                _preparingOrders.value = orders
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load preparing orders"
            }
        }
    }
    
    /**
     * Load ready orders (Prepared status)
     */
    fun loadReadyOrders() {
        viewModelScope.launch {
            try {
                val orders = repository.getReadyOrders()
                _readyOrders.value = orders
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load ready orders"
            }
        }
    }
    
    /**
     * Accept an order
     */
    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.acceptOrder(orderId)
            _isLoading.value = false
            
            result.onSuccess {
                _successMessage.value = "Order accepted"
                // Refresh data after successful action
                loadNewOrders()
                loadPreparingOrders()
            }.onFailure {
                _error.value = it.message ?: "Failed to accept order"
            }
        }
    }
    
    /**
     * Mark order as prepared
     */
    fun markPrepared(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.markPrepared(orderId)
            _isLoading.value = false
            
            result.onSuccess {
                _successMessage.value = "Order marked as prepared"
                // Refresh data after successful action
                loadPreparingOrders()
                loadReadyOrders()
            }.onFailure {
                _error.value = it.message ?: "Failed to mark order as prepared"
            }
        }
    }
    
    /**
     * Mark order as delivered
     */
    fun markDelivered(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.markDelivered(orderId)
            _isLoading.value = false
            
            result.onSuccess {
                _successMessage.value = "Order marked as delivered"
                // Refresh data after successful action
                loadReadyOrders()
            }.onFailure {
                _error.value = it.message ?: "Failed to mark order as delivered"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}


