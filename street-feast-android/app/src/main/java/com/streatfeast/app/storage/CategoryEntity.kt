package com.streatfeast.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)


