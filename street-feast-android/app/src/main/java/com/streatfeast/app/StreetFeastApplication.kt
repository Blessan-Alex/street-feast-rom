package com.streatfeast.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.repositories.SupabaseOrderRepository
import com.streatfeast.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.streatfeast.app.BuildConfig



class StreetFeastApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var orderRepository: SupabaseOrderRepository

    override fun onCreate() {
        super.onCreate()

        orderRepository = ServiceLocator.provideOrderRepository(this)

        NotificationHelper.createNotificationChannel(this)
        logNotificationPermissionState()

        initOneSignal()

        // Let ViewModel handle realtime subscriptions - Application should not manage realtime
        // This ensures ViewModel's callback is properly registered
        applicationScope.launch {
            orderRepository.refresh()
        }
    }

    private fun initOneSignal() {
        if (BuildConfig.ONESIGNAL_APP_ID.isBlank()) {
            Log.w("StreetFeastApp", "ONESIGNAL_APP_ID missing; notifications disabled.")
            return
        }

        Log.d("StreetFeastApp", "Initializing OneSignal with App ID: ${BuildConfig.ONESIGNAL_APP_ID}")
        
        // Enable verbose logging for debugging
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        
        // v5 initialization API
        OneSignal.initWithContext(applicationContext, BuildConfig.ONESIGNAL_APP_ID)
        Log.d("StreetFeastApp", "OneSignal initialized with context")

        // Don't request permission or register device here - let MainActivity handle it after user login
        // Only log current permission state
        maybeRequestPostNotificationsPermission()

        // Device registration must happen AFTER OneSignal.login(user.id) in MainActivity
        // Application should only initialize OneSignal SDK, not register devices
    }

    // REMOVED: waitForSubscriptionAndRegister()
    // Device registration should only happen in MainActivity AFTER OneSignal.login(user.id)
    // This prevents registering devices as anonymous before user login

    private fun maybeRequestPostNotificationsPermission() {
        // Only log permission state - don't register devices here
        // Device registration must happen in MainActivity AFTER OneSignal.login(user.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                Log.d("StreetFeastApp", "Notification permission already granted - MainActivity will register device after login")
            } else {
                Log.d("StreetFeastApp", "Notification permission not granted - MainActivity will request it after user interaction")
                // Don't request permission here - let MainActivity handle it after user sees the UI
                // This provides better UX as permission request happens when user is engaged
            }
        } else {
            Log.d("StreetFeastApp", "Android version < TIRAMISU - notification permission not required")
        }
    }

    // REMOVED: checkAndRegisterSubscription()
    // Device registration should only happen in MainActivity AFTER OneSignal.login(user.id)
    // This prevents registering devices before user login

    private fun logNotificationPermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w("StreetFeastApp", "POST_NOTIFICATIONS not granted. Request in UI when appropriate.")
            } else {
                Log.d("StreetFeastApp", "POST_NOTIFICATIONS granted.")
            }
        }
    }

    override fun onTerminate() {
        // Realtime is managed by ViewModel, not Application
        // call suspend clear() from a coroutine (minimal change)
        CoroutineScope(Dispatchers.IO).launch { ServiceLocator.clear() }
        super.onTerminate()
    }
}
