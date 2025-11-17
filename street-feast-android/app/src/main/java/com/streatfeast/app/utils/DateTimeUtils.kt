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
        val now = System.currentTimeMillis()
        val diffMillis = now-timestampMillis.toEpochMilli()

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
