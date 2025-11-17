package com.streatfeast.app.activities

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Android 13+ notification permission gate
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Check auth state after a short delay
        lifecycleScope.launch {
            delay(1500)
            checkAuthState()
        }
    }
    
    private suspend fun checkAuthState() {
        val repository = ServiceLocator.provideAuthRepository(applicationContext)
        val isLoggedIn = repository.isLoggedIn()
        val destination = if (isLoggedIn) MainActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, destination))
        finish()
    }
}


