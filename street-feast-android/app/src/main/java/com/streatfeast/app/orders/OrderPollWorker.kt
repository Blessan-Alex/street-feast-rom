package com.streatfeast.app.orders

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.streatfeast.app.utils.Constants
import com.streatfeast.app.utils.NotificationHelper
import kotlinx.coroutines.tasks.await
import java.util.Date

class OrderPollWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {
    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("poll", Context.MODE_PRIVATE)
            val last = prefs.getLong("lastSeen", 0L)
            val since = Timestamp(Date(if (last == 0L) System.currentTimeMillis() - 60 * 60 * 1000 else last))

            val snap = Firebase.firestore
                .collection("stores").document(Constants.DEFAULT_STORE_ID).collection("orders")
                .whereGreaterThan("updatedAt", since)
                .limit(50)
                .get().await()

            var newest = last
            for (doc in snap.documents) {
                val data = doc.data ?: continue
                val ts = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: last
                newest = maxOf(newest, ts)
                val status = data["status"] as? String ?: ""
                if (status == "Prepared") NotificationHelper.showPreparedNotification(applicationContext, data)
                else NotificationHelper.showNewOrderNotification(applicationContext, data)
            }
            prefs.edit().putLong("lastSeen", newest).apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("OrderPollWorker", "Error polling orders", e)
            
            // Differentiate between retryable and permanent errors
            when (e) {
                is FirebaseFirestoreException -> {
                    when (e.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                        FirebaseFirestoreException.Code.UNAUTHENTICATED,
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT -> {
                            // Permanent errors - don't retry
                            Log.e("OrderPollWorker", "Permanent error: ${e.code}", e)
                            Result.failure()
                        }
                        FirebaseFirestoreException.Code.UNAVAILABLE,
                        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> {
                            // Transient errors - retry
                            Log.w("OrderPollWorker", "Transient error, will retry: ${e.code}", e)
                            Result.retry()
                        }
                        else -> {
                            // Unknown Firestore error - retry by default
                            Log.w("OrderPollWorker", "Unknown Firestore error, will retry: ${e.code}", e)
                            Result.retry()
                        }
                    }
                }
                else -> {
                    // Non-Firestore exceptions - retry (network issues, etc.)
                    Log.w("OrderPollWorker", "Non-Firestore error, will retry", e)
                    Result.retry()
                }
            }
        }
    }
}

