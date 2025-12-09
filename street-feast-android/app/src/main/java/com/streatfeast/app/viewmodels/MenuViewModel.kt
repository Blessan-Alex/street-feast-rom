package com.streatfeast.app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.streatfeast.app.models.Category
import com.streatfeast.app.models.MenuData
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.repositories.MenuRepository
import com.streatfeast.app.storage.MenuLocalDataSource
import com.streatfeast.app.storage.toCategory
import com.streatfeast.app.storage.toMenuItem
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch

class MenuViewModel(
    private val repository: MenuRepository,
    private val localDataSource: MenuLocalDataSource,
    private val storeId: String
) : ViewModel() {

    // Store the actual store ID (fetched dynamically)
    private val _actualStoreId = MutableLiveData<String>()

    // LiveData for categories - observe with actual store ID
    val categories: LiveData<List<Category>> = liveData {
        // Wait for store ID to be fetched, then observe
        val actualId = _actualStoreId.value ?: _actualStoreId.asFlow().filterNotNull().first()
        localDataSource.observeCategories(actualId).collect { entities ->
            emit(entities.map { it.toCategory() })
        }
    }

    // LiveData for items - observe with actual store ID
    val items: LiveData<List<MenuItem>> = liveData {
        // Wait for store ID to be fetched, then observe
        val actualId = _actualStoreId.value ?: _actualStoreId.asFlow().filterNotNull().first()
        localDataSource.observeItems(actualId).collect { entities ->
            emit(entities.map { it.toMenuItem() })
        }
    }

    // LiveData for frequent item IDs - observe with actual store ID
    val frequentItemIds: LiveData<List<String>> = liveData {
        // Wait for store ID to be fetched, then observe
        val actualId = _actualStoreId.value ?: _actualStoreId.asFlow().filterNotNull().first()
        localDataSource.observeFrequentItems(actualId)
            .map { entities -> entities.map { it.itemId } }
            .collect { emit(it) }
    }

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Fetch actual store ID first, then load menu and start realtime
        viewModelScope.launch {
            try {
                val actualStoreId = repository.getStoreIdForFragment()
                _actualStoreId.value = actualStoreId
                Log.d("MenuViewModel", "Using store ID: $actualStoreId")
                
                _isLoading.value = true
                repository.maybeRefreshMenu(actualStoreId)
                    .onFailure { e ->
                        Log.e("MenuViewModel", "Failed to refresh menu", e)
                        _error.value = e.message ?: "Failed to refresh menu"
                    }
                _isLoading.value = false
                
                // Start realtime subscription with actual store ID
                repository.subscribeToMenuUpdates(viewModelScope, actualStoreId) { menuData ->
                    Log.d("MenuViewModel", "Menu updated via realtime")
                    // Menu is already updated in Room cache, LiveData will automatically update
                }
            } catch (e: Exception) {
                Log.e("MenuViewModel", "Failed to fetch store ID, using default", e)
                _actualStoreId.value = storeId
                _isLoading.value = true
                repository.maybeRefreshMenu(storeId)
                    .onFailure { ex ->
                        Log.e("MenuViewModel", "Failed to refresh menu with default store ID", ex)
                        _error.value = ex.message ?: "Failed to refresh menu"
                    }
                _isLoading.value = false
            }
        }
    }

    fun loadMenu() {
        viewModelScope.launch {
            val actualStoreId = _actualStoreId.value ?: repository.getStoreIdForFragment()
            _isLoading.value = true
            _error.value = null
            
            repository.fetchMenu(actualStoreId)
                .onSuccess { menuData ->
                    Log.d("MenuViewModel", "Menu loaded successfully: ${menuData.categories.size} categories, ${menuData.items.size} items")
                    _isLoading.value = false
                }
                .onFailure { e ->
                    Log.e("MenuViewModel", "Failed to load menu", e)
                    _error.value = e.message ?: "Failed to load menu"
                    _isLoading.value = false
                }
        }
    }

    fun getItemsByCategory(categoryId: String): LiveData<List<MenuItem>> = liveData {
        localDataSource.observeItemsByCategory(categoryId).collect { entities ->
            emit(entities.map { it.toMenuItem() })
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopRealtime()
    }
}

// Factory for creating MenuViewModel
class MenuViewModelFactory(
    private val repository: MenuRepository,
    private val localDataSource: MenuLocalDataSource,
    private val storeId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MenuViewModel(repository, localDataSource, storeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


