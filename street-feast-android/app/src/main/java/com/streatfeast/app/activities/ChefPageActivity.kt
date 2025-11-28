package com.streatfeast.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.streatfeast.app.R
import com.streatfeast.app.databinding.ActivityChefPageBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.UserRole
import com.streatfeast.app.utils.NotificationHelper
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.launch

class ChefPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChefPageBinding
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(ServiceLocator.provideAuthRepository(applicationContext))
    }

    private var isUserValidated = false
    private var hasSetupOneSignal = false
    private var isRedirecting = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("ChefPageActivity", "Notification permission granted via system API")
            lifecycleScope.launch {
                requestOneSignalPermissionAndRegister()
            }
        } else {
            android.util.Log.w("ChefPageActivity", "Notification permission denied via system API")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChefPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        setupOneSignalNotificationHandler()
        setupUserValidationAndObserver()
        handleDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        // Simple session guard - if session becomes null, redirect to login
        lifecycleScope.launch {
            val repository = ServiceLocator.provideAuthRepository(applicationContext)
            if (!repository.isLoggedIn()) {
                android.util.Log.w("ChefPageActivity", "Session expired, redirecting to login")
                redirectToLogin()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun setupUserValidationAndObserver() {
        authViewModel.currentUser.observe(this) { user ->
            // Prevent multiple redirect attempts
            if (isRedirecting || isFinishing) {
                return@observe
            }

            if (user == null) {
                // User is null - might be loading or logged out
                if (!isUserValidated) {
                    // Wait a bit for async load
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(2000)
                        
                        if (isFinishing || isRedirecting) {
                            return@launch
                        }
                        
                        val currentUser = authViewModel.currentUser.value
                        if (currentUser == null) {
                            android.util.Log.w("ChefPageActivity", "No user after delay, redirecting")
                            redirectToLogin()
                        }
                    }
                } else {
                    // User was validated but now null - logged out
                    redirectToLogin()
                }
                return@observe
            }

            // Validate role
            if (user.role != UserRole.CHEF && user.role != UserRole.ADMIN) {
                android.util.Log.e("ChefPageActivity", "Invalid role: ${user.role}")
                redirectToLogin()
                return@observe
            }

            // Mark as validated
            if (!isUserValidated) {
                isUserValidated = true
                android.util.Log.d("ChefPageActivity", "User validated: ${user.id}, role: ${user.role}")
            }

            // Setup OneSignal only once
            if (!hasSetupOneSignal) {
                hasSetupOneSignal = true
                android.util.Log.d("ChefPageActivity", "Setting up OneSignal for user: ${user.id}")
                OneSignal.login(user.id)
                lifecycleScope.launch {
                    waitForSubscriptionAfterLogin()
                }
            }
        }
    }

    private fun setupOneSignalNotificationHandler() {
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                android.util.Log.d("ChefPageActivity", "OneSignal notification clicked")

                val additionalData = event.notification.additionalData
                val orderId = additionalData?.get("orderId")?.toString()
                val orderNumber = (additionalData?.get("number") as? Number)?.toInt()

                if (orderNumber != null) {
                    Toast.makeText(
                        this@ChefPageActivity,
                        "New order #$orderNumber received!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ChefPageActivity,
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

    private fun handleDeepLink(intent: Intent) {
        if (intent.action == "OPEN_ORDER") {
            android.util.Log.d("ChefPageActivity", "Deep link received: OPEN_ORDER")
            
            // Navigate to chef page and refresh
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
            val navController = navHostFragment?.navController

            navController?.let { controller ->
                navigateToFragment(R.id.chefPageFragment)
                lifecycleScope.launch {
                    val repository = ServiceLocator.provideOrderRepository(applicationContext)
                    repository.refresh()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("ChefPageActivity", "Notification permission already granted")
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(2000)
                        waitForSubscriptionAndRegister()
                    }
                }
                else -> {
                    android.util.Log.d("ChefPageActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            android.util.Log.d("ChefPageActivity", "Android version < TIRAMISU - checking subscription directly")
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000)
                waitForSubscriptionAndRegister()
            }
        }
    }

    private suspend fun requestOneSignalPermissionAndRegister() {
        android.util.Log.d("ChefPageActivity", "Requesting OneSignal notification permission")
        try {
            val accepted = OneSignal.Notifications.requestPermission(fallbackToSettings = true)
            android.util.Log.d("ChefPageActivity", "OneSignal notification permission accepted=$accepted")
            
            if (accepted) {
                waitForSubscriptionAndRegister()
            } else {
                android.util.Log.w("ChefPageActivity", "OneSignal notification permission denied")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChefPageActivity", "Error requesting OneSignal permission: ${e.message}", e)
        }
    }

    private suspend fun waitForSubscriptionAndRegister() {
        val currentUser = authViewModel.currentUser.value
        if (currentUser == null) {
            android.util.Log.d("ChefPageActivity", "User not logged in yet - skipping device registration")
            return
        }
        
        android.util.Log.d("ChefPageActivity", "Waiting for subscription ID to become available (user logged in: ${currentUser.id})")
        
        var attempts = 0
        val maxAttempts = 8
        val delayBetweenAttempts = 1000L
        
        while (attempts < maxAttempts) {
            val sub = OneSignal.User.pushSubscription
            val subscriptionId = sub.id
            val optedIn = sub.optedIn
            
            android.util.Log.d(
                "ChefPageActivity",
                "Subscription check attempt ${attempts + 1}/$maxAttempts: id=$subscriptionId, optedIn=$optedIn"
            )
            
            if (!subscriptionId.isNullOrBlank() && optedIn) {
                android.util.Log.d("ChefPageActivity", "Subscription ID available: $subscriptionId, registering device for user ${currentUser.id}")
                runCatching {
                    ServiceLocator.provideOrderRepository(applicationContext).registerDevice(subscriptionId)
                }.onSuccess {
                    android.util.Log.d("ChefPageActivity", "Device registered successfully: $subscriptionId")
                }.onFailure { e ->
                    android.util.Log.w("ChefPageActivity", "Failed to register device: ${e.message}")
                }
                return
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(delayBetweenAttempts)
            }
        }
        
        android.util.Log.w("ChefPageActivity", "Subscription ID check exhausted all retries")
    }

    private suspend fun waitForSubscriptionAfterLogin() {
        android.util.Log.d("ChefPageActivity", "Waiting for subscription ID after login")
        
        kotlinx.coroutines.delay(1500)
        
        var attempts = 0
        val maxAttempts = 10
        val delayBetweenAttempts = 1000L
        
        while (attempts < maxAttempts) {
            val sub = OneSignal.User.pushSubscription
            val subscriptionId = sub.id
            val optedIn = sub.optedIn
            
            android.util.Log.d(
                "ChefPageActivity",
                "Post-login subscription check attempt ${attempts + 1}/$maxAttempts: id=$subscriptionId, optedIn=$optedIn"
            )
            
            if (!subscriptionId.isNullOrBlank() && optedIn) {
                android.util.Log.d("ChefPageActivity", "Subscription ID available after login: $subscriptionId, registering device")
                runCatching {
                    ServiceLocator.provideOrderRepository(applicationContext).registerDevice(subscriptionId)
                }.onSuccess {
                    android.util.Log.d("ChefPageActivity", "Device registered successfully after login: $subscriptionId")
                }.onFailure { e ->
                    android.util.Log.w("ChefPageActivity", "Failed to register device after login: ${e.message}")
                }
                return
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(delayBetweenAttempts)
            }
        }
        
        android.util.Log.w("ChefPageActivity", "Subscription ID check after login exhausted all retries")
    }

    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        navController?.navigate(fragmentId)
    }

    private fun redirectToLogin() {
        // Prevent multiple redirect calls
        if (isRedirecting || isFinishing) {
            return
        }
        
        isRedirecting = true
        lifecycleScope.launch {
            try {
                // Stop realtime subscription
                ServiceLocator.provideOrderRepository(applicationContext).stopRealtime()
                
                // Logout from OneSignal
                OneSignal.logout()
                
                // Logout from AuthViewModel
                authViewModel.logout()
                
                // Clear ServiceLocator
                ServiceLocator.clear()
                
                // Navigate to LoginActivity
                val intent = Intent(this@ChefPageActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                android.util.Log.e("ChefPageActivity", "Error during redirect to login", e)
                val intent = Intent(this@ChefPageActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController?.currentDestination?.id == R.id.chefPageFragment) {
            // If on start destination, exit app
            finish()
        } else {
            // Otherwise, handle back navigation normally
            super.onBackPressed()
        }
    }
}

