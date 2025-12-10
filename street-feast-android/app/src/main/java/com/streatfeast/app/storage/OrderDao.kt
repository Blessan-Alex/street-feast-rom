package com.streatfeast.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Transaction
    @Query("SELECT * FROM orders WHERE storeId = :storeId AND status = :status ORDER BY createdAt DESC")
    fun observeByStatus(storeId: String, status: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE storeId = :storeId AND status = :status AND type = :type ORDER BY createdAt DESC")
    fun observeByStatusAndType(storeId: String, status: String, type: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE storeId = :storeId AND status IN (:statuses) ORDER BY createdAt DESC")
    fun observeByStatuses(storeId: String, statuses: List<String>): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrder(orderId: String): OrderWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId IN (:orderIds)")
    suspend fun deleteItems(orderIds: List<String>)

    @Query("DELETE FROM order_items WHERE orderId NOT IN (:orderIds)")
    suspend fun pruneItems(orderIds: List<String>)

    @Query("DELETE FROM orders WHERE storeId = :storeId")
    suspend fun clearForStore(storeId: String)
}



