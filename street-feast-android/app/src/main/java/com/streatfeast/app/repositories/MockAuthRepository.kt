package com.streatfeast.app.repositories

import com.streatfeast.app.models.User
import com.streatfeast.app.models.UserRole

class MockAuthRepository {
    
    private var currentUser: User? = null
    
    /**
     * Mock login that accepts chef@streetfeast.com and waiter@streetfeast.com
     * Any password is accepted for prototype purposes
     */
    fun login(email: String, password: String): Result<User> {
        return when (email.lowercase().trim()) {
            "chef@streetfeast.com" -> {
                currentUser = User(
                    id = "mock_chef_001",
                    email = "chef@streetfeast.com",
                    displayName = "Test Chef",
                    role = UserRole.CHEF,
                    deviceTokens = emptyList()
                )
                Result.success(currentUser!!)
            }
            "waiter@streetfeast.com" -> {
                currentUser = User(
                    id = "mock_waiter_001",
                    email = "waiter@streetfeast.com", 
                    displayName = "Test Waiter",
                    role = UserRole.WAITER,
                    deviceTokens = emptyList()
                )
                Result.success(currentUser!!)
            }
            else -> {
                Result.failure(Exception("Invalid credentials. Use chef@streetfeast.com or waiter@streetfeast.com"))
            }
        }
    }
    
    /**
     * Get current logged in user
     */
    fun getCurrentUser(): User? = currentUser
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = currentUser != null
    
    /**
     * Logout current user
     */
    fun logout() {
        currentUser = null
    }
}
