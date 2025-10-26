package com.streatfeast.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streatfeast.app.models.User
import com.streatfeast.app.repositories.MockAuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val repository = MockAuthRepository()
    
    // Current user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    // Authentication state
    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        checkAuthState()
    }
    
    /**
     * Check if user is already logged in
     */
    private fun checkAuthState() {
        _isAuthenticated.value = repository.isLoggedIn()
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password are required"
            return
        }
        
        println("DEBUG: AuthViewModel - Starting login process")
        
        _isLoading.value = true
        _error.value = null
        
        println("DEBUG: AuthViewModel - Calling repository.login()")
        val result = repository.login(email, password)
        _isLoading.value = false
        
        result.onSuccess { user ->
            println("DEBUG: AuthViewModel - Login successful, user: $user")
            _currentUser.value = user
            _isAuthenticated.value = true
            println("DEBUG: AuthViewModel - isAuthenticated set to true")
        }.onFailure { exception ->
            println("DEBUG: AuthViewModel - Login failed: ${exception.message}")
            _error.value = exception.message ?: "Login failed"
            _isAuthenticated.value = false
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        repository.logout()
        _currentUser.value = null
        _isAuthenticated.value = false
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}


