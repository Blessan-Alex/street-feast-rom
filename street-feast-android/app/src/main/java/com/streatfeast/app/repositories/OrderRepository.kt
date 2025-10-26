package com.streatfeast.app.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.utils.Constants
import kotlinx.coroutines.tasks.await

class OrderRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val storeId = Constants.DEFAULT_STORE_ID
    
    private fun getOrdersCollection() = 
        db.collection("${Constants.STORES_COLLECTION}/$storeId/${Constants.ORDERS_COLLECTION}")
    
    /**
     * Listen to new orders (Created status) in real-time
     */
    fun getNewOrdersRealtime(callback: (List<Order>) -> Unit): ListenerRegistration {
        return getOrdersCollection()
            .whereEqualTo("status", Constants.STATUS_CREATED)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                callback(orders)
            }
    }
    
    /**
     * Listen to preparing orders (InKitchen status) in real-time
     */
    fun getPreparingOrdersRealtime(callback: (List<Order>) -> Unit): ListenerRegistration {
        return getOrdersCollection()
            .whereEqualTo("status", Constants.STATUS_IN_KITCHEN)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                callback(orders)
            }
    }
    
    /**
     * Listen to ready orders (Prepared status) in real-time
     */
    fun getReadyOrdersRealtime(callback: (List<Order>) -> Unit): ListenerRegistration {
        return getOrdersCollection()
            .whereEqualTo("status", Constants.STATUS_PREPARED)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                callback(orders)
            }
    }
    
    /**
     * Get order items for a specific order
     */
    suspend fun getOrderItems(orderId: String): Result<List<OrderItem>> {
        return try {
            val snapshot = getOrdersCollection()
                .document(orderId)
                .collection(Constants.ORDER_ITEMS_COLLECTION)
                .get()
                .await()
            
            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(OrderItem::class.java)?.copy(id = doc.id)
            }
            
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Accept order - changes status directly to InKitchen (auto-accept)
     */
    suspend fun acceptOrder(orderId: String): Result<Unit> {
        return try {
            getOrdersCollection()
                .document(orderId)
                .update(
                    mapOf(
                        "status" to Constants.STATUS_IN_KITCHEN,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark order as prepared
     */
    suspend fun markPrepared(orderId: String): Result<Unit> {
        return try {
            getOrdersCollection()
                .document(orderId)
                .update(
                    mapOf(
                        "status" to Constants.STATUS_PREPARED,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark order as delivered
     */
    suspend fun markDelivered(orderId: String): Result<Unit> {
        return try {
            getOrdersCollection()
                .document(orderId)
                .update(
                    mapOf(
                        "status" to Constants.STATUS_DELIVERED,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


