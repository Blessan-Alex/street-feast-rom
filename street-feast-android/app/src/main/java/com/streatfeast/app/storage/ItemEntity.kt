package com.streatfeast.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val storeId: String,
    val name: String,
    val sizes: String, // JSON string, use TypeConverter
    val vegFlag: String,
    val flavors: String?, // JSON string or null, use TypeConverter
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)


