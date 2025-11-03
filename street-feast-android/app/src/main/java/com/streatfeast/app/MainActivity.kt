package com.streatfeast.app.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    
    // Permission launcher for notification permission (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.w("MainActivity", "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
        
        // Request notification permission for Android 13+ (API 33+)
        requestNotificationPermissionIfNeeded()
        
        setupRoleBasedUI()
        setupNavigation()
    }
    
    /**
     * Request POST_NOTIFICATIONS permission for Android 13+ (API 33+)
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    android.util.Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
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