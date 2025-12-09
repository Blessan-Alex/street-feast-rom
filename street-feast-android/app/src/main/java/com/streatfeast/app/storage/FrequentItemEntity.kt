package com.streatfeast.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frequent_items")
data class FrequentItemEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val itemId: String,
    val orderIndex: Int
)


