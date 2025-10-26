package com.streatfeast.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.CHEF,
    val deviceTokens: List<String> = emptyList()
) : Parcelable

enum class UserRole {
    ADMIN, CHEF, WAITER;
    
    companion object {
        fun fromString(value: String): UserRole = when(value.lowercase()) {
            "admin" -> ADMIN
            "chef" -> CHEF
            "waiter" -> WAITER
            else -> CHEF
        }
    }
    
    fun toFirestoreString(): String = when(this) {
        ADMIN -> "admin"
        CHEF -> "chef"
        WAITER -> "waiter"
    }
}


