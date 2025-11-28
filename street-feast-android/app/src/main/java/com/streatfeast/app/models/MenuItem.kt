package com.streatfeast.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MenuItem(
    val id: String,
    val name: String,
    val description: String = "",
    val sizes: List<String> = listOf("Small", "medium", "Large"),
    val vegFlag: String = "Veg", // "Veg" or "NonVeg" or "Both"
    val categoryId: String? = null
) : Parcelable {
    val isVeg: Boolean
        get() = vegFlag == "Veg"
    
    val isNonVeg: Boolean
        get() = vegFlag == "NonVeg"
    
    val hasBoth: Boolean
        get() = vegFlag == "Both"
    
    fun getSizesDisplay(): String {
        return sizes.joinToString(",") { it.lowercase() }
    }
}

