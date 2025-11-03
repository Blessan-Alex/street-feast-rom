package com.streatfeast.app.orders

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.streatfeast.app.utils.NotificationHelper

class OrderPushWatcher(
    private val ctx: Context,
    private val storeId: String
) {
    fun start(): ListenerRegistration {
        val db = Firebase.firestore
        return db.collection("stores").document(storeId).collection("orders")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e("OrderPushWatcher", "Firestore listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snap == null) {
                    Log.w("OrderPushWatcher", "Snapshot is null")
                    return@addSnapshotListener
                }
                
                for (dc in snap.documentChanges) {
                    try {
                        val data = dc.document.data
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                Log.d("OrderPushWatcher", "New order detected: ${data["orderNumber"]}")
                                NotificationHelper.showNewOrderNotification(ctx, data)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                if (data["status"] == "Prepared") {
                                    Log.d("OrderPushWatcher", "Order prepared: ${data["orderNumber"]}")
                                    NotificationHelper.showPreparedNotification(ctx, data)
                                }
                            }
                            else -> Unit
                        }
                    } catch (ex: Exception) {
                        Log.e("OrderPushWatcher", "Error processing document change", ex)
                    }
                }
            }
    }
}

