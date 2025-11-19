package com.streatfeast.app.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getTimeAgo(timestampMillis: Instant): String {
        val now = Instant.now()
        val diffMillis = now.toEpochMilli() - timestampMillis.toEpochMilli()
        
        // Handle negative differences (future timestamps)
        if (diffMillis < 0) {
            return "Just now"
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        return when {
            minutes < 1 -> "Just now"
            minutes == 1L -> "1 min ago"
            minutes < 60 -> "$minutes mins ago"
            else -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                if (hours == 1L) {
                    "1 hr ago"
                } else {
                    "$hours hrs ago"
                }
            }
        }
    }

    fun format12Hour(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    fun formatDateTime(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    fun now(): Long = System.currentTimeMillis()
}
