package com.streatfeast.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.streatfeast.app.databinding.ActivityLoginBinding
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.AuthViewModelFactory
import com.streatfeast.app.di.ServiceLocator

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(ServiceLocator.provideAuthRepository(applicationContext))
    }
    
    private var hasNavigated = false  // Flag to prevent multiple navigations
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeAuthState()
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }
    }
    
    private fun observeAuthState() {
        // Observe authentication state
        viewModel.isAuthenticated.observe(this) { isAuthenticated ->
            println("DEBUG: LoginActivity - isAuthenticated changed to: $isAuthenticated")
            if (isAuthenticated && !hasNavigated) {
                hasNavigated = true  // Set flag to prevent multiple navigations
                println("DEBUG: LoginActivity - Navigating to AuthGateActivity")
                // Login successful, navigate to AuthGateActivity (single router)
                startActivity(Intent(this, AuthGateActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.btnLogin.isEnabled = false
                binding.btnLogin.text = ""
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Login"
                binding.progressBar.visibility = View.GONE
            }
        }
        
        // Observe errors
        viewModel.error.observe(this) { error ->
            if (error != null) {
                // Friendly snackbar + shake inputs
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                playShake(binding.etEmail)
                playShake(binding.etPassword)
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Login"
                binding.progressBar.visibility = View.GONE
                viewModel.clearError()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }

    private fun playShake(target: View) {
        val shake: Animation = TranslateAnimation(0f, 12f, 0f, 0f).apply {
            duration = 150
            repeatCount = 2
            repeatMode = Animation.REVERSE
        }
        target.startAnimation(shake)
    }
}


