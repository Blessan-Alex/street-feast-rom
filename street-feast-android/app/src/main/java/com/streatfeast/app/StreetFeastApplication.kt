package com.streatfeast.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.streatfeast.app.orders.OrderPollWorker
import com.streatfeast.app.orders.OrderPushWatcher
import com.streatfeast.app.utils.Constants
import com.streatfeast.app.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class StreetFeastApplication : Application() {
    
    private var orderListener: ListenerRegistration? = null
    
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // Enable offline persistence for Firestore
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Check notification permission (Android 13+)
        checkNotificationPermission()

        // Foreground push watcher - store registration for cleanup
        orderListener = OrderPushWatcher(this, Constants.DEFAULT_STORE_ID).start()

        // Background polling every 15 min
        val req = PeriodicWorkRequestBuilder<OrderPollWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "order_poll", ExistingPeriodicWorkPolicy.UPDATE, req
        )
    }
    
    /**
     * Check notification permission status for Android 13+ (API 33+)
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.w("StreetFeastApp", "POST_NOTIFICATIONS permission not granted. Request permission in MainActivity.")
            } else {
                Log.d("StreetFeastApp", "POST_NOTIFICATIONS permission granted.")
            }
        }
    }
    
    override fun onTerminate() {
        // Cleanup Firestore listener to prevent memory leaks
        orderListener?.remove()
        orderListener = null
        super.onTerminate()
    }
}


