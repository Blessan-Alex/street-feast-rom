package com.streatfeast.app.activities

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.streatfeast.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    private val auth = FirebaseAuth.getInstance()
    
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
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 1500) // 1.5 seconds splash delay
    }
    
    private fun checkAuthState() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // User is logged in, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User not logged in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        finish()
    }
}


