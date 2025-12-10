package com.streatfeast.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.streatfeast.app.R
import com.streatfeast.app.activities.MainActivity
import com.streatfeast.app.activities.ChefPageActivity
import com.streatfeast.app.activities.WaiterActivity
import com.streatfeast.app.models.UserRole

object NotificationHelper {
    
    /**
     * Create notification channel (Android O+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_orders)
                enableVibration(true)
                // Prefer long sound if available; fallback to default
                val soundUri = resolveLongSoundUri(context)
                if (soundUri != null) {
                    setSound(soundUri, android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build())
                }
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification for new order (Map overload)
     */
    fun showNewOrderNotification(context: Context, data: Map<String, Any>) {
        val orderNumber = when (val orderNum = data["orderNumber"]) {
            is Long -> orderNum.toInt()
            is Int -> orderNum
            is Number -> orderNum.toInt()
            else -> 0
        }
        showNewOrderNotification(context, orderNumber)
    }
    
    /**
     * Show notification for new order
     */
    fun showNewOrderNotification(context: Context, orderNumber: Int) {
        showNewOrderNotification(context, orderNumber, null)
    }
    
    /**
     * Show notification for new order with role-based routing
     */
    fun showNewOrderNotification(context: Context, orderNumber: Int, role: UserRole?) {
        // Determine target activity based on role
        val targetActivity = when (role) {
            UserRole.CHEF, UserRole.ADMIN -> ChefPageActivity::class.java
            UserRole.WAITER -> WaiterActivity::class.java
            null -> MainActivity::class.java // Fallback to MainActivity if role unknown
        }
        
        val intent = Intent(context, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "OPEN_ORDER"
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderNumber,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_new_orders)
            .setContentTitle(context.getString(R.string.notif_new_order_title))
            .setContentText(context.getString(R.string.notif_new_order_body, orderNumber))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(orderNumber, notification)
        
        // Play sound
        SoundManager.playSoundWithFallback(context, Constants.SOUND_PING, loopMillis = 0)
    }
    
    /**
     * Show notification for prepared order (Map overload)
     */
    fun showPreparedNotification(context: Context, data: Map<String, Any>) {
        val orderNumber = when (val orderNum = data["orderNumber"]) {
            is Long -> orderNum.toInt()
            is Int -> orderNum
            is Number -> orderNum.toInt()
            else -> 0
        }
        showPreparedNotification(context, orderNumber)
    }
    
    /**
     * Show notification for prepared order
     */
    fun showPreparedNotification(context: Context, orderNumber: Int) {
        showPreparedNotification(context, orderNumber, null)
    }
    
    /**
     * Show notification for prepared order with role-based routing
     */
    fun showPreparedNotification(context: Context, orderNumber: Int, role: UserRole?) {
        // Prepared orders are for waiters, but route to appropriate activity based on role
        val targetActivity = when (role) {
            UserRole.WAITER -> WaiterActivity::class.java
            UserRole.CHEF, UserRole.ADMIN -> ChefPageActivity::class.java
            null -> MainActivity::class.java // Fallback to MainActivity if role unknown
        }
        
        val intent = Intent(context, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "OPEN_ORDER"
            putExtra("status", "Prepared")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderNumber,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ready)
            .setContentTitle(context.getString(R.string.notif_prepared_title))
            .setContentText(context.getString(R.string.notif_prepared_body, orderNumber))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(orderNumber, notification)
        
        // Play sound
        SoundManager.playSoundWithFallback(context, Constants.SOUND_PING, loopMillis = 0)
    }
    
    /**
     * Show notification for canceled order
     */
    fun showCanceledNotification(context: Context, orderNumber: Int) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_new_orders)
            .setContentTitle("Order Canceled")
            .setContentText("Order #$orderNumber was canceled")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(orderNumber, notification)
        
        // Play buzzer sound
        SoundManager.playSoundWithFallback(context, Constants.SOUND_BUZZER, loopMillis = 0)
    }

    private fun resolveLongSoundUri(context: Context): Uri? {
        val resId = context.resources.getIdentifier("ping_long", "raw", context.packageName)
        return if (resId != 0) {
            Uri.parse("android.resource://${context.packageName}/$resId")
        } else {
            null
        }
    }
}


