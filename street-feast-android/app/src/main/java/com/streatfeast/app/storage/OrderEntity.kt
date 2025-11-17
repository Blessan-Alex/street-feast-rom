package com.streatfeast.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val orderNumber: Int?,
    val type: String?,
    val chefTip: String?,
    val status: String,
    val createdBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val parentOrderId: String?
)



