package com.streatfeast.app.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    indices = [
        Index("orderId")
    ]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val name: String,
    val size: String?,
    val vegFlag: String?,
    val quantity: Int,
    val chefTip: String?,
    val isPrepared: Boolean
)



