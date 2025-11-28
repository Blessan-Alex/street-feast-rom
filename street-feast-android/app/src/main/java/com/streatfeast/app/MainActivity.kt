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
import androidx.lifecycle.lifecycleScope
import com.streatfeast.app.R
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.databinding.ActivityMainBinding
import com.streatfeast.app.models.UserRole
import com.streatfeast.app.utils.NotificationHelper
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.AuthViewModelFactory
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickListener
import android.widget.Toast
import com.onesignal.notifications.INotificationClickEvent
import kotlinx.coroutines.launch
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(ServiceLocator.provideAuthRepository(applicationContext))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted via system API")
            // After permission is granted, request OneSignal permission and check subscription
            lifecycleScope.launch {
                requestOneSignalPermissionAndRegister()
            }
        } else {
            android.util.Log.w("MainActivity", "Notification permission denied via system API")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        setupOneSignalNotificationHandler()

        // Set toolbar as action bar
        setSupportActionBar(binding.toolbar)

        setupRoleBasedUI()
        setupNavigation()
    }

    /**
     * Setup OneSignal notification click handler (v5 API)
     */
    private fun setupOneSignalNotificationHandler() {
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                android.util.Log.d("MainActivity", "OneSignal notification clicked")

                val additionalData = event.notification.additionalData
                val orderId = additionalData?.get("orderId")?.toString()
                val orderNumber = (additionalData?.get("number") as? Number)?.toInt()

                if (orderNumber != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "New order #$orderNumber received!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "New order received!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                val navController = navHostFragment?.navController

                navController?.let { controller ->
                    val currentDestination = controller.currentDestination?.id
                    if (currentDestination == R.id.chefPageFragment) {
                        // If already on chef page, trigger refresh
                        lifecycleScope.launch {
                            val repository = ServiceLocator.provideOrderRepository(applicationContext)
                            repository.refresh()
                        }
                    } else {
                        navigateToFragment(R.id.chefPageFragment)
                    }
                }
            }
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "Notification permission already granted")
                    // Permission already granted, check if OneSignal subscription is ready
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(2000) // Wait a bit for subscription
                        waitForSubscriptionAndRegister()
                    }
                }
                else -> {
                    android.util.Log.d("MainActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For older Android versions, just check subscription
            android.util.Log.d("MainActivity", "Android version < TIRAMISU - checking subscription directly")
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000)
                waitForSubscriptionAndRegister()
            }
        }
    }

    private suspend fun requestOneSignalPermissionAndRegister() {
        android.util.Log.d("MainActivity", "Requesting OneSignal notification permission")
        try {
            val accepted = OneSignal.Notifications.requestPermission(fallbackToSettings = true)
            android.util.Log.d("MainActivity", "OneSignal notification permission accepted=$accepted")
            
            if (accepted) {
                // Permission granted, but only register if user is logged in
                // waitForSubscriptionAndRegister() will check if user is logged in before registering
                waitForSubscriptionAndRegister()
            } else {
                android.util.Log.w("MainActivity", "OneSignal notification permission denied")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error requesting OneSignal permission: ${e.message}", e)
        }
    }

    private suspend fun waitForSubscriptionAndRegister() {
        // Only register device if user is logged in (OneSignal.login() has been called)
        // Check if user is logged in by checking currentUser
        val currentUser = authViewModel.currentUser.value
        if (currentUser == null) {
            android.util.Log.d("MainActivity", "User not logged in yet - skipping device registration. Will register after login.")
            return
        }
        
        android.util.Log.d("MainActivity", "Waiting for subscription ID to become available (user logged in: ${currentUser.id})")
        
        var attempts = 0
        val maxAttempts = 8
        val delayBetweenAttempts = 1000L // 1 second
        
        while (attempts < maxAttempts) {
            val sub = OneSignal.User.pushSubscription
            val subscriptionId = sub.id
            val optedIn = sub.optedIn
            
            android.util.Log.d(
                "MainActivity",
                "Subscription check attempt ${attempts + 1}/$maxAttempts: id=$subscriptionId, optedIn=$optedIn"
            )
            
            if (!subscriptionId.isNullOrBlank() && optedIn) {
                android.util.Log.d("MainActivity", "Subscription ID available: $subscriptionId, registering device for user ${currentUser.id}")
                runCatching {
                    ServiceLocator.provideOrderRepository(applicationContext).registerDevice(subscriptionId)
                }.onSuccess {
                    android.util.Log.d("MainActivity", "Device registered successfully: $subscriptionId")
                }.onFailure { e ->
                    android.util.Log.w("MainActivity", "Failed to register device: ${e.message}")
                }
                return
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(delayBetweenAttempts)
            }
        }
        
        android.util.Log.w("MainActivity", "Subscription ID check exhausted all retries. Will retry after login.")
    }

    private fun setupRoleBasedUI() {
        // Observe changes to user state
        authViewModel.currentUser.observe(this) { user ->
            if (user != null) {
                android.util.Log.d("MainActivity", "User logged in: ${user.id}, logging into OneSignal")
                OneSignal.login(user.id)
                
                // After login, wait for subscription ID with retry logic
                // Subscription ID may not be immediately available after login
                lifecycleScope.launch {
                    waitForSubscriptionAfterLogin()
                }
                
                // Navigate based on role
                navigateToRoleBasedFragment(user.role)
            } else {
                android.util.Log.d("MainActivity", "User logged out, logging out of OneSignal")
                OneSignal.logout()
                
                // Navigate to LoginActivity when user logs out
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        
        // Check initial state - if no user after a brief delay, navigate to login
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500) // Wait for checkAuthState to complete
            val currentUser = authViewModel.currentUser.value
            if (currentUser == null) {
                // No user logged in, navigate to login
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun navigateToRoleBasedFragment(role: UserRole) {
        when (role) {
            UserRole.CHEF -> {
                binding.bottomNavigation.menu.clear()
                binding.bottomNavigation.inflateMenu(R.menu.chef_nav_menu)
                navigateToFragment(R.id.chefPageFragment)
            }
            UserRole.WAITER -> {
                binding.bottomNavigation.menu.clear()
                binding.bottomNavigation.inflateMenu(R.menu.waiter_nav_menu)
                navigateToFragment(R.id.orderTypeFragment)
            }
            UserRole.ADMIN -> {
                binding.bottomNavigation.menu.clear()
                binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
                navigateToFragment(R.id.chefPageFragment)
            }
        }
    }

    private suspend fun waitForSubscriptionAfterLogin() {
        android.util.Log.d("MainActivity", "Waiting for subscription ID after login")
        
        // Wait a bit after login for OneSignal to process
        kotlinx.coroutines.delay(1500)
        
        var attempts = 0
        val maxAttempts = 10
        val delayBetweenAttempts = 1000L // 1 second
        
        while (attempts < maxAttempts) {
            val sub = OneSignal.User.pushSubscription
            val subscriptionId = sub.id
            val optedIn = sub.optedIn
            
            android.util.Log.d(
                "MainActivity",
                "Post-login subscription check attempt ${attempts + 1}/$maxAttempts: id=$subscriptionId, optedIn=$optedIn"
            )
            
            if (!subscriptionId.isNullOrBlank() && optedIn) {
                android.util.Log.d("MainActivity", "Subscription ID available after login: $subscriptionId, registering device")
                runCatching {
                    ServiceLocator.provideOrderRepository(applicationContext).registerDevice(subscriptionId)
                }.onSuccess {
                    android.util.Log.d("MainActivity", "Device registered successfully after login: $subscriptionId")
                }.onFailure { e ->
                    android.util.Log.w("MainActivity", "Failed to register device after login: ${e.message}")
                }
                return
            } else if (subscriptionId != null && !optedIn) {
                android.util.Log.w("MainActivity", "Subscription ID exists but not opted in: $subscriptionId")
                // Continue checking - might become opted in
            } else {
                android.util.Log.d("MainActivity", "Subscription ID not yet available after login, retrying...")
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(delayBetweenAttempts)
            }
        }
        
        android.util.Log.w("MainActivity", "Subscription ID check after login exhausted all retries. Device may register later when subscription becomes available.")
    }

    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(fragmentId)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Store reference to listener so we can temporarily remove it
        val itemSelectedListener = com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_orders -> {
                    navController.navigate(R.id.chefPageFragment)
                    true
                }
                R.id.nav_preparing -> {
                    navController.navigate(R.id.chefPageFragment)
                    true
                }
                R.id.nav_ready -> {
                    navController.navigate(R.id.readyOrderFragment)
                    true
                }
                R.id.nav_new_order -> {
                    navController.navigate(R.id.orderTypeFragment)
                    true
                }
                R.id.nav_ready_order -> {
                    navController.navigate(R.id.readyOrderFragment)
                    true
                }
                R.id.nav_given_order -> {
                    navController.navigate(R.id.givenOrderFragment)
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener(itemSelectedListener)

        // Manually sync bottom navigation selection when destination changes programmatically
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Temporarily remove listener to prevent infinite loop when setting selectedItemId
            binding.bottomNavigation.setOnItemSelectedListener(null)
            
            // Hide/show toolbar and bottom nav based on destination
            when (destination.id) {
                R.id.chefPageFragment -> {
                    // Hide old UI elements - chef page has its own top bar and bottom buttons
                    binding.toolbar.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.GONE
                    binding.bottomNavigation.selectedItemId = R.id.nav_new_orders
                }
                R.id.orderTypeFragment, R.id.orderWhereFragment, R.id.orderItemFragment, R.id.orderCategoryItemsFragment, R.id.waiterReadyFragment, R.id.previewOrderFragment, R.id.previewOrderHeaderFragment, R.id.givenOrderFragment, R.id.readyOrderFragment -> {
                    // Hide old UI elements - waiter fragments have their own UI
                    binding.toolbar.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.GONE
                    when (destination.id) {
                        R.id.orderTypeFragment -> {
                            // New Order tab is selected (handled by fragment's bottom nav)
                        }
                        R.id.orderWhereFragment -> {
                            // Table selection screen (handled by fragment's bottom nav)
                        }
                        R.id.orderItemFragment -> {
                            // Item selection screen (handled by fragment's bottom nav)
                        }
                        R.id.orderCategoryItemsFragment -> {
                            // Category items screen (handled by fragment's bottom nav)
                        }
                        R.id.waiterReadyFragment -> {
                            // Ready Order tab is selected (handled by fragment's bottom nav)
                        }
                        R.id.previewOrderFragment -> {
                            // Preview order screen (compact) - handled by fragment's bottom nav
                        }
                        R.id.previewOrderHeaderFragment -> {
                            // Preview order screen (with header) - handled by fragment's bottom nav
                        }
                        R.id.givenOrderFragment -> {
                            // Given Order tab is selected (handled by fragment's bottom nav)
                        }
                        R.id.readyOrderFragment -> {
                            // Ready Order tab is selected (handled by fragment's bottom nav)
                        }
                    }
                }
                else -> {
                    // Show old UI elements for other fragments
                    binding.toolbar.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    when (destination.id) {
                        R.id.chefPreparingFragment -> {
                            binding.bottomNavigation.selectedItemId = R.id.nav_preparing
                        }
                    }
                }
            }
            
            // Re-add listener after setting selected item
            binding.bottomNavigation.setOnItemSelectedListener(itemSelectedListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                handleLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleLogout() {
        lifecycleScope.launch {
            try {
                // Stop realtime subscription to prevent memory leaks
                ServiceLocator.provideOrderRepository(applicationContext).stopRealtime()
                
                // Logout from AuthViewModel
                authViewModel.logout()
                
                // Clear ServiceLocator to reset repositories
                ServiceLocator.clear()
                
                // Navigate to LoginActivity with flags to clear back stack
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error during logout", e)
                // Even if there's an error, try to navigate to login
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
