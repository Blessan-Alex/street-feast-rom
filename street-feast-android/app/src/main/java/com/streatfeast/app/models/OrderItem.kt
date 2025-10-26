package com.streatfeast.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    val id: String = "",
    val itemId: String = "",
    val nameSnapshot: String = "",
    val size: String? = null, // "Small" or "Large" or null
    val vegFlagSnapshot: String = "Veg", // "Veg" or "NonVeg"
    val qty: Int = 1
) : Parcelable {
    
    val isVeg: Boolean
        get() = vegFlagSnapshot == "Veg"
        
    fun getDisplayName(): String {
        return if (size != null) {
            "$nameSnapshot - $size"
        } else {
            nameSnapshot
        }
    }
    
    fun getDisplayQuantity(): String {
        return "× $qty"
    }
}


