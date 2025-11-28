package com.streatfeast.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.streatfeast.app.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * AuthGateActivity is the single router for the entire app.
 * It is the ONLY place that decides where to navigate based on auth state and user role.
 * All other activities should only guard for logout/expired sessions.
 */
class AuthGateActivity : AppCompatActivity() {

    private var routed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // IMPORTANT: avoid routing twice (config change / multiple emissions)
            if (routed) {
                android.util.Log.d("AuthGateActivity", "Already routed, skipping")
                return@launch
            }
            routed = true

            val repository = ServiceLocator.provideAuthRepository(applicationContext)
            val isLoggedIn = repository.isLoggedIn()

            if (!isLoggedIn) {
                android.util.Log.d("AuthGateActivity", "Not logged in, routing to LoginActivity")
                openAndClear(LoginActivity::class.java)
                return@launch
            }

            // Fetch user to determine role
            val user = repository.getCurrentUser()
            if (user == null) {
                android.util.Log.w("AuthGateActivity", "Session exists but user is null, routing to LoginActivity")
                openAndClear(LoginActivity::class.java)
                return@launch
            }

            if (user.role == null) {
                android.util.Log.w("AuthGateActivity", "User role is null, routing to LoginActivity")
                openAndClear(LoginActivity::class.java)
                return@launch
            }

            // Normalize role (handle "chef" vs "CHEF" casing)
            val role = user.role.name.trim().uppercase()
            android.util.Log.d("AuthGateActivity", "User logged in: ${user.id}, role: $role")

            when (role) {
                "CHEF" -> {
                    android.util.Log.d("AuthGateActivity", "Routing to ChefPageActivity")
                    openAndClear(ChefPageActivity::class.java)
                }
                "WAITER" -> {
                    android.util.Log.d("AuthGateActivity", "Routing to WaiterActivity")
                    openAndClear(WaiterActivity::class.java)
                }
                "ADMIN" -> {
                    android.util.Log.d("AuthGateActivity", "Admin user, routing to ChefPageActivity")
                    openAndClear(ChefPageActivity::class.java)
                }
                else -> {
                    android.util.Log.w("AuthGateActivity", "Unknown role: $role, routing to LoginActivity")
                    openAndClear(LoginActivity::class.java)
                }
            }
        }
    }

    private fun openAndClear(target: Class<*>) {
        val intent = Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

