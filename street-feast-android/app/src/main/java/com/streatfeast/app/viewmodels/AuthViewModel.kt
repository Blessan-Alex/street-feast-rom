package com.streatfeast.app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streatfeast.app.models.User
import com.streatfeast.app.repositories.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var userSetDirectly = false  // Flag to prevent checkAuthState from overwriting

    init {
        viewModelScope.launch { checkAuthState() }
    }

    private suspend fun checkAuthState() {
        // Don't overwrite if user was set directly
        if (userSetDirectly) {
            android.util.Log.d("AuthViewModel", "Skipping checkAuthState - user was set directly")
            return
        }
        
        val loggedIn = repository.isLoggedIn()
        _isAuthenticated.value = loggedIn
        if (loggedIn) {
            _currentUser.value = repository.getCurrentUser()
        }
    }

    fun login(email: String, password: String) {
        Log.d("AuthViewModel", "login called for: $email")
        
        if (email.isBlank() || password.isBlank()) {
            Log.w("AuthViewModel", "Login failed: Email or password is blank")
            _error.value = "Email and password are required"
            return
        }

        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting login process")
            _isLoading.value = true
            _error.value = null

            val result = repository.login(email, password)
            _isLoading.value = false

            result.onSuccess { user ->
                Log.d("AuthViewModel", "Login success - User: ${user.email}, Role: ${user.role}")
                _currentUser.value = user
                _isAuthenticated.value = true
            }.onFailure { exception ->
                Log.e("AuthViewModel", "Login failure", exception)
                Log.e("AuthViewModel", "Error message: ${exception.message}")
                Log.e("AuthViewModel", "Error type: ${exception.javaClass.simpleName}")
                _error.value = exception.message ?: "Login failed"
                _isAuthenticated.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            _isAuthenticated.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setUserDirectly(user: User) {
        userSetDirectly = true  // Set flag to prevent checkAuthState from overwriting
        _currentUser.value = user
        _isAuthenticated.value = true
        _isLoading.value = false
        android.util.Log.d("AuthViewModel", "User set directly: ${user.id}, role: ${user.role}")
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
