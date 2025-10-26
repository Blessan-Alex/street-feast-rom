package com.streatfeast.app.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.streatfeast.app.R
import com.streatfeast.app.databinding.ActivityMainBinding
import com.streatfeast.app.models.UserRole
import com.streatfeast.app.utils.NotificationHelper
import com.streatfeast.app.viewmodels.AuthViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
        
        setupRoleBasedUI()
        setupNavigation()
    }
    
    private fun setupRoleBasedUI() {
        authViewModel.currentUser.observe(this) { user ->
            user?.let {
                when (it.role) {
                    UserRole.CHEF -> {
                        // Show Chef tabs: New Orders, Preparing
                        binding.bottomNavigation.menu.clear()
                        binding.bottomNavigation.inflateMenu(R.menu.chef_nav_menu)
                        // Navigate to New Orders by default
                        navigateToFragment(R.id.chefNewOrdersFragment)
                    }
                    UserRole.WAITER -> {
                        // Show Waiter tabs: Ready Orders only
                        binding.bottomNavigation.menu.clear()
                        binding.bottomNavigation.inflateMenu(R.menu.waiter_nav_menu)
                        // Navigate to Ready Orders by default
                        navigateToFragment(R.id.waiterReadyFragment)
                    }
                    UserRole.ADMIN -> {
                        // Show all tabs
                        binding.bottomNavigation.menu.clear()
                        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
                        // Navigate to New Orders by default
                        navigateToFragment(R.id.chefNewOrdersFragment)
                    }
                }
            }
        }
    }
    
    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(fragmentId)
    }
    
    private fun setupNavigation() {
        // Get NavHostFragment and NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Setup bottom navigation with nav controller
        binding.bottomNavigation.setupWithNavController(navController)
        
        // Map bottom nav IDs to fragment destinations
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_orders -> {
                    navController.navigate(R.id.chefNewOrdersFragment)
                    true
                }
                R.id.nav_preparing -> {
                    navController.navigate(R.id.chefPreparingFragment)
                    true
                }
                R.id.nav_ready -> {
                    navController.navigate(R.id.waiterReadyFragment)
                    true
                }
                else -> false
            }
        }
    }
}