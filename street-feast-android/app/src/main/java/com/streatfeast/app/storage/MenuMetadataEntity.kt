package com.streatfeast.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_metadata")
data class MenuMetadataEntity(
    @PrimaryKey val storeId: String,
    val lastUpdatedAt: Long,
    val dataHash: String? = null
)













