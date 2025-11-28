package com.streatfeast.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.utils.NotificationHelper
import kotlinx.coroutines.launch

/**
 * MainActivity is now a simple activity that only guards for session.
 * All routing is handled by AuthGateActivity.
 * This activity may be used for backward compatibility with deep links.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel (needed for notifications)
        NotificationHelper.createNotificationChannel(this)
        
        // Handle deep links - redirect to WaiterActivity if session is valid
        if (intent?.action == "OPEN_ORDER") {
            lifecycleScope.launch {
                val repository = ServiceLocator.provideAuthRepository(applicationContext)
                if (repository.isLoggedIn()) {
                    // Session valid, redirect to WaiterActivity with deep link
                    val waiterIntent = Intent(this@MainActivity, WaiterActivity::class.java).apply {
                        action = "OPEN_ORDER"
                        intent?.extras?.let { putExtras(it) }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(waiterIntent)
                } else {
                    // No session, redirect to login
                    redirectToLogin()
                }
                finish()
            }
        } else {
            // No deep link, just redirect to AuthGateActivity
            lifecycleScope.launch {
                redirectToAuthGate()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Simple session guard - if session becomes null, redirect to login
        lifecycleScope.launch {
            val repository = ServiceLocator.provideAuthRepository(applicationContext)
            if (!repository.isLoggedIn()) {
                redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun redirectToAuthGate() {
        val intent = Intent(this, AuthGateActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
