package com.streatfeast.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.streatfeast.app.models.OrderItem
import java.util.UUID

class OrderDraftViewModel : ViewModel() {
    
    private val _draftItems = MutableLiveData<List<OrderItem>>(emptyList())
    val draftItems: LiveData<List<OrderItem>> = _draftItems
    
    private val _itemCount = MutableLiveData<Int>(0)
    val itemCount: LiveData<Int> = _itemCount
    
    private val currentItems: MutableList<OrderItem> = mutableListOf()
    
    init {
        updateItemCount()
    }
    
    fun addDraftItem(item: OrderItem) {
        val newItem = item.copy(id = UUID.randomUUID().toString())
        currentItems.add(newItem)
        _draftItems.value = currentItems.toList()
        updateItemCount()
    }
    
    fun removeDraftItem(itemId: String) {
        currentItems.removeAll { it.id == itemId }
        _draftItems.value = currentItems.toList()
        updateItemCount()
    }
    
    fun updateDraftItem(itemId: String, patch: (OrderItem) -> OrderItem) {
        val index = currentItems.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            currentItems[index] = patch(currentItems[index])
            _draftItems.value = currentItems.toList()
            updateItemCount()
        }
    }
    
    fun getDraftItems(): List<OrderItem> {
        return currentItems.toList()
    }
    
    fun clearDraft() {
        currentItems.clear()
        _draftItems.value = emptyList()
        updateItemCount()
    }
    
    fun getDraftItemCount(): Int {
        return currentItems.sumOf { it.qty }
    }
    
    private fun updateItemCount() {
        _itemCount.value = getDraftItemCount()
    }
}

