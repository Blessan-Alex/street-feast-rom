package com.streatfeast.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.streatfeast.app.databinding.ActivityLoginBinding
import com.streatfeast.app.viewmodels.AuthViewModel

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    
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
            if (isAuthenticated) {
                println("DEBUG: LoginActivity - Navigating to MainActivity")
                // Login successful, navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
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
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                viewModel.clearError()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }
}


