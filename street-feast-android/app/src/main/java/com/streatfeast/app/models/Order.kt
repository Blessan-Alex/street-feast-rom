package com.streatfeast.app.models

import android.os.Build
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.streatfeast.app.R
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class Order @RequiresApi(Build.VERSION_CODES.O) constructor(
    val id: String = "",
    val orderNumber: Int = 0,
    val type: OrderType = OrderType.DINE_IN,
    val chefTip: String = "",
    val status: OrderStatus = OrderStatus.CREATED,
    val createdBy: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val parentOrderId: String? = null,
    val tableNumber: Int? = null,
    val licensePlate: String? = null,
    val isEdited: Boolean = false,
    var items: List<OrderItem> = emptyList() // populated from subcollection
) : Parcelable

enum class OrderType {
    DINE_IN, PARCEL, EAT_AWAY;
    
    companion object {
        fun fromString(value: String): OrderType = when(value) {
            "DineIn" -> DINE_IN
            "Parcel" -> PARCEL
            "EatAway" -> EAT_AWAY
            "Delivery" -> EAT_AWAY  // Backward compatibility
            else -> DINE_IN
        }
    }
    
    fun toDisplayString(): String = when(this) {
        DINE_IN -> "Dine-in"
        PARCEL -> "Parcel"
        EAT_AWAY -> "Eat Away"
    }
    
    fun toRemoteValue(): String = when(this) {
        DINE_IN -> "DineIn"
        PARCEL -> "Parcel"
        EAT_AWAY -> "EatAway"
    }
}

enum class OrderStatus(val colorRes: Int) {
    CREATED(R.color.status_created),
    ACCEPTED(R.color.status_accepted),
    IN_KITCHEN(R.color.status_inkitchen),
    PREPARED(R.color.status_prepared),
    DELIVERED(R.color.status_delivered),
    CLOSED(R.color.status_closed),
    CANCELED(R.color.status_canceled);
    
    companion object {
        fun fromString(value: String): OrderStatus = when(value) {
            "Created" -> CREATED
            "Accepted" -> ACCEPTED
            "InKitchen" -> IN_KITCHEN
            "Prepared" -> PREPARED
            "Delivered" -> DELIVERED
            "Closed" -> CLOSED
            "Canceled" -> CANCELED
            else -> CREATED
        }
    }
    
    fun toRemoteValue(): String = when(this) {
        CREATED -> "Created"
        ACCEPTED -> "Accepted"
        IN_KITCHEN -> "InKitchen"
        PREPARED -> "Prepared"
        DELIVERED -> "Delivered"
        CLOSED -> "Closed"
        CANCELED -> "Canceled"
    }
    
    fun toDisplayString(): String = when(this) {
        CREATED -> "Created"
        ACCEPTED -> "Accepted"
        IN_KITCHEN -> "In Kitchen"
        PREPARED -> "Prepared"
        DELIVERED -> "Delivered"
        CLOSED -> "Closed"
        CANCELED -> "Canceled"
    }
}


