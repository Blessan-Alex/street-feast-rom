package com.streatfeast.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.streatfeast.app.R
import com.streatfeast.app.activities.MainActivity

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
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification for new order
     */
    fun showNewOrderNotification(context: Context, orderNumber: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
        SoundManager.playSound(context, Constants.SOUND_PING)
    }
    
    /**
     * Show notification for prepared order
     */
    fun showPreparedNotification(context: Context, orderNumber: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
        SoundManager.playSound(context, Constants.SOUND_PING)
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
        SoundManager.playSound(context, Constants.SOUND_BUZZER)
    }
}


