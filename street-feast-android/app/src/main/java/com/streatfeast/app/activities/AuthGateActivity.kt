package com.streatfeast.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.streatfeast.app.R
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.di.SupabaseModule
import com.streatfeast.app.viewmodels.SessionUiState
import com.streatfeast.app.viewmodels.SessionViewModel
import com.streatfeast.app.viewmodels.SessionViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AuthGateActivity is the single router for the entire app.
 * It is the ONLY place that decides where to navigate based on auth state and user role.
 * All other activities should only guard for logout/expired sessions.
 */
class AuthGateActivity : AppCompatActivity() {

    private var hasNavigated = false
    private var isNavigating = false
    
    private val sessionViewModel: SessionViewModel by viewModels {
        val supabase = SupabaseModule.provideClient(applicationContext)
        SessionViewModelFactory(supabase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_gate)
        
        observeSessionState()
    }
    
    override fun onResume() {
        super.onResume()
        // Re-check auth state when app comes back from background
        // Allow re-check even if we've navigated before (for session expiration detection)
        if (!isNavigating) {
            checkAndRoute()
        }
    }
    
    private fun observeSessionState() {
        lifecycleScope.launch {
            sessionViewModel.uiState.collectLatest { state ->
                android.util.Log.d("AuthGateActivity", "Session state: isLoading=${state.isLoading}, isLoggedIn=${state.isLoggedIn}")
                
                // Wait for initialization to complete
                if (state.isLoading) {
                    android.util.Log.d("AuthGateActivity", "Waiting for auth initialization...")
                    return@collectLatest
                }
                
                // Now check auth state and route
                checkAndRoute(state.isLoggedIn)
            }
        }
    }
    
    private fun checkAndRoute(isLoggedIn: Boolean? = null) {
        lifecycleScope.launch {
            // Prevent concurrent navigation attempts
            if (isNavigating) {
                android.util.Log.d("AuthGateActivity", "Navigation in progress, skipping")
                return@launch
            }
            
            // For initial navigation, only navigate once
            if (hasNavigated && isLoggedIn != null) {
                android.util.Log.d("AuthGateActivity", "Already navigated initially, skipping duplicate")
                return@launch
            }
            
            isNavigating = true
            try {
                // Use provided isLoggedIn or check from repository
                val loggedIn = isLoggedIn ?: run {
                    val repository = ServiceLocator.provideAuthRepository(applicationContext)
                    repository.isLoggedIn()
                }

                if (!loggedIn) {
                    android.util.Log.d("AuthGateActivity", "Not logged in, routing to LoginActivity")
                    hasNavigated = true
                    openAndClear(LoginActivity::class.java)
                    return@launch
                }

                // Fetch user to determine role
                val repository = ServiceLocator.provideAuthRepository(applicationContext)
                val user = repository.getCurrentUser()
                if (user == null) {
                    android.util.Log.w("AuthGateActivity", "Session exists but user is null, routing to LoginActivity")
                    hasNavigated = true
                    openAndClear(LoginActivity::class.java)
                    return@launch
                }

                if (user.role == null) {
                    android.util.Log.w("AuthGateActivity", "User role is null, routing to LoginActivity")
                    hasNavigated = true
                    openAndClear(LoginActivity::class.java)
                    return@launch
                }

                // Normalize role (handle "chef" vs "CHEF" casing)
                val role = user.role.name.trim().uppercase()
                android.util.Log.d("AuthGateActivity", "User logged in: ${user.id}, role: $role")

                hasNavigated = true
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
            } finally {
                isNavigating = false
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

