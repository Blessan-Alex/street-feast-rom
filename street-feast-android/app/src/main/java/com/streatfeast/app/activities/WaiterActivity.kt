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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import com.streatfeast.app.R
import com.streatfeast.app.databinding.ActivityWaiterBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.UserRole
import com.streatfeast.app.utils.NotificationHelper
import com.streatfeast.app.utils.navigateSafe
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class WaiterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaiterBinding
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
            android.util.Log.d("WaiterActivity", "Notification permission granted via system API")
            lifecycleScope.launch {
                requestOneSignalPermissionAndRegister()
            }
        } else {
            android.util.Log.w("WaiterActivity", "Notification permission denied via system API")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply edge-to-edge before inflating content
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityWaiterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scrim = binding.statusBarScrim
        val host = binding.navHostFragment
        val initialBottom = host.paddingBottom

        // Apply insets: occupy status bar space with scrim color, pad bottom for nav
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            android.util.Log.d("Insets", "top=${bars.top} bottom=${bars.bottom}")

            scrim.layoutParams = scrim.layoutParams.apply { height = bars.top }
            scrim.requestLayout()

            host.updatePadding(bottom = initialBottom + bars.bottom)
            insets
        }
        binding.root.post { ViewCompat.requestApplyInsets(binding.root) }

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
                android.util.Log.w("WaiterActivity", "Session expired, redirecting to login")
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
                            android.util.Log.w("WaiterActivity", "No user after delay, redirecting")
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
            if (user.role != UserRole.WAITER) {
                android.util.Log.e("WaiterActivity", "Invalid role: ${user.role}")
                redirectToLogin()
                return@observe
            }

            // Mark as validated
            if (!isUserValidated) {
                isUserValidated = true
                android.util.Log.d("WaiterActivity", "User validated: ${user.id}, role: ${user.role}")
            }

            // Setup OneSignal only once
            if (!hasSetupOneSignal) {
                hasSetupOneSignal = true
                android.util.Log.d("WaiterActivity", "Setting up OneSignal for user: ${user.id}")
                OneSignal.login(user.id)
                lifecycleScope.launch {
                    waitForSubscriptionAfterLogin()
                }
            }
        }
    }

    private fun setupOneSignalNotificationHandler() {
        // Handle foreground notifications - show toast when notification received
        OneSignal.Notifications.addForegroundLifecycleListener(object : com.onesignal.notifications.INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                android.util.Log.d("WaiterActivity", "OneSignal notification received in foreground")

                val additionalData = event.notification.additionalData

                val orderNumber =
                    additionalData.optIntOrNull("orderNumber")
                        ?: additionalData.optIntOrNull("number")

                val status = additionalData.optStringOrNull("status")

                // Show toast for all order-related notifications, not just Prepared
                lifecycleScope.launch(Dispatchers.Main) {
                    when (status?.lowercase()) {
                        "prepared", "itemprepared" -> {
                            val message = if (orderNumber != null) {
                                "Order #$orderNumber is ready to be delivered"
                            } else {
                                "New order is ready to be delivered"
                            }
                            Toast.makeText(
                                this@WaiterActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            val navHostFragment = supportFragmentManager
                                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                            val navController = navHostFragment?.navController

                            navController?.let { controller ->
                                val currentDestination = controller.currentDestination?.id
                                if (currentDestination != R.id.readyOrderFragment) {
                                    navigateToFragment(R.id.readyOrderFragment)
                                }
                            }
                        }
                        "orderaltered", "created", "inkitchen" -> {
                            val message = when (status.lowercase()) {
                                "orderaltered" -> if (orderNumber != null) "Order #$orderNumber has been updated" else "Order has been updated"
                                "created" -> if (orderNumber != null) "New order #$orderNumber" else "New order"
                                "inkitchen" -> if (orderNumber != null) "Order #$orderNumber is being prepared" else "Order is being prepared"
                                else -> if (orderNumber != null) "Order #$orderNumber updated" else "Order updated"
                            }
                            Toast.makeText(
                                this@WaiterActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // Display the notification
                event.notification.display()
            }
        })
        
        // Handle notification clicks
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                android.util.Log.d("WaiterActivity", "OneSignal notification clicked")

                val additionalData = event.notification.additionalData
                val orderId = additionalData.optStringOrNull("orderId")

                val orderNumber =
                    additionalData.optIntOrNull("orderNumber")
                        ?: additionalData.optIntOrNull("number")

                val status = additionalData.optStringOrNull("status")

                if (orderNumber != null) {
                    Toast.makeText(
                        this@WaiterActivity,
                        "Order #$orderNumber update received!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@WaiterActivity,
                        "Order update received!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.navHostFragment) as? NavHostFragment
                val navController = navHostFragment?.navController

                navController?.let { controller ->
                    // If order is ready, navigate to ready orders
                    if (status?.equals("prepared", true) == true || status?.equals("itemprepared", true) == true) {
                        navigateToFragment(R.id.readyOrderFragment)
                    } else {
                        // Otherwise, navigate to appropriate fragment based on current state
                        val currentDestination = controller.currentDestination?.id
                        if (currentDestination != R.id.readyOrderFragment) {
                            navigateToFragment(R.id.readyOrderFragment)
                        }
                    }
                }
            }
        })
    }

    private fun handleDeepLink(intent: Intent) {
        if (intent.action == "OPEN_ORDER") {
            android.util.Log.d("WaiterActivity", "Deep link received: OPEN_ORDER")
            
            val status = intent.getStringExtra("status")
            
            // Navigate to appropriate fragment based on order status
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as? NavHostFragment
            val navController = navHostFragment?.navController

            navController?.let { controller ->
                if (status?.equals("prepared", true) == true || status?.equals("itemprepared", true) == true) {
                    navigateToFragment(R.id.readyOrderFragment)
                } else {
                    // Default to order type fragment
                    navigateToFragment(R.id.orderTypeFragment)
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
                    android.util.Log.d("WaiterActivity", "Notification permission already granted")
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(2000)
                        waitForSubscriptionAndRegister()
                    }
                }
                else -> {
                    android.util.Log.d("WaiterActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            android.util.Log.d("WaiterActivity", "Android version < TIRAMISU - checking subscription directly")
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000)
                waitForSubscriptionAndRegister()
            }
        }
    }

    private suspend fun requestOneSignalPermissionAndRegister() {
        android.util.Log.d("WaiterActivity", "Requesting OneSignal notification permission")
        try {
            val accepted = OneSignal.Notifications.requestPermission(fallbackToSettings = true)
            android.util.Log.d("WaiterActivity", "OneSignal notification permission accepted=$accepted")
            
            if (accepted) {
                waitForSubscriptionAndRegister()
            } else {
                android.util.Log.w("WaiterActivity", "OneSignal notification permission denied")
            }
        } catch (e: Exception) {
            android.util.Log.e("WaiterActivity", "Error requesting OneSignal permission: ${e.message}", e)
        }
    }

    private suspend fun waitForSubscriptionAndRegister() {
        val currentUser = authViewModel.currentUser.value
        if (currentUser == null) {
            android.util.Log.d("WaiterActivity", "User not logged in yet - skipping device registration")
            return
        }
        
        android.util.Log.d("WaiterActivity", "Waiting for subscription ID to become available (user logged in: ${currentUser.id})")
        
        var attempts = 0
        val maxAttempts = 8
        val delayBetweenAttempts = 1000L
        
        while (attempts < maxAttempts) {
            val sub = OneSignal.User.pushSubscription
            val subscriptionId = sub.id
            val optedIn = sub.optedIn
            
            android.util.Log.d(
                "WaiterActivity",
                "Subscription check attempt ${attempts + 1}/$maxAttempts: id=$subscriptionId, optedIn=$optedIn"
            )
            
            if (!subscriptionId.isNullOrBlank() && optedIn) {
                android.util.Log.d("WaiterActivity", "Subscription ID available: $subscriptionId, registering device for user ${currentUser.id}")
                runCatching {
                    ServiceLocator.provideOrderRepository(applicationContext).registerDevice(subscriptionId)
                }.onSuccess {
                    android.util.Log.d("WaiterActivity", "Device registered successfully: $subscriptionId")
                }.onFailure { e ->
                    android.util.Log.w("WaiterActivity", "Failed to register device: ${e.message}")
                }
                return
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(delayBetweenAttempts)
            }
        }
        
        android.util.Log.w("WaiterActivity", "Subscription ID check exhausted all retries")
    }

    private suspend fun waitForSubscriptionAfterLogin() {
        android.util.Log.d("WaiterActivity", "Waiting for subscription ID after login")
        
        kotlinx.coroutines.delay(1500)
        
        var attempts = 0
        val maxAttempts = 10
        val delayBetweenAttempts = 1000L
        
        while (attempts < maxAttempts) {
            val sub = OneSignal.User.pushSubscription
            val subscriptionId = sub.id
            val optedIn = sub.optedIn
            
            android.util.Log.d(
                "WaiterActivity",
                "Post-login subscription check attempt ${attempts + 1}/$maxAttempts: id=$subscriptionId, optedIn=$optedIn"
            )
            
            if (!subscriptionId.isNullOrBlank() && optedIn) {
                android.util.Log.d("WaiterActivity", "Subscription ID available after login: $subscriptionId, registering device")
                runCatching {
                    ServiceLocator.provideOrderRepository(applicationContext).registerDevice(subscriptionId)
                }.onSuccess {
                    android.util.Log.d("WaiterActivity", "Device registered successfully after login: $subscriptionId")
                }.onFailure { e ->
                    android.util.Log.w("WaiterActivity", "Failed to register device after login: ${e.message}")
                }
                return
            }
            
            attempts++
            if (attempts < maxAttempts) {
                kotlinx.coroutines.delay(delayBetweenAttempts)
            }
        }
        
        android.util.Log.w("WaiterActivity", "Subscription ID check after login exhausted all retries")
    }

    private fun JSONObject?.optIntOrNull(key: String): Int? {
        if (this == null) return null
        return try {
            if (has(key) && !isNull(key)) optInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            else null
        } catch (_: Exception) { null }
    }

    private fun JSONObject?.optStringOrNull(key: String): String? {
        if (this == null) return null
        return try {
            if (has(key) && !isNull(key)) optString(key, null)
            else null
        } catch (_: Exception) { null }
    }

    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        navController?.navigateSafe(fragmentId)
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
                val intent = Intent(this@WaiterActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                android.util.Log.e("WaiterActivity", "Error during redirect to login", e)
                val intent = Intent(this@WaiterActivity, LoginActivity::class.java)
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

        if (navController?.currentDestination?.id == R.id.orderTypeFragment) {
            // If on start destination, exit app
            finish()
        } else {
            // Otherwise, handle back navigation normally
            super.onBackPressed()
        }
    }
}

