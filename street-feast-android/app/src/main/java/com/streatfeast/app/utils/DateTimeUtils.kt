package com.streatfeast.app.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    
    /**
     * Format timestamp to "X min ago" or "Just now"
     */
    fun getTimeAgo(timestamp: Timestamp): String {
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diffMillis = now - then
        
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            else -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                "$hours hr ago"
            }
        }
    }
    
    /**
     * Format timestamp to 12-hour format (e.g., "2:30 PM")
     */
    fun format12Hour(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
    
    /**
     * Format timestamp to date and time (e.g., "Jan 15, 2:30 PM")
     */
    fun formatDateTime(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
    
    /**
     * Get current timestamp
     */
    fun now(): Timestamp {
        return Timestamp.now()
    }
}


