package com.streatfeast.app.models

import android.os.Parcelable
import com.streatfeast.app.R
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    val id: String = "",
    val orderNumber: Int = 0,
    val type: OrderType = OrderType.DINE_IN,
    val chefTip: String = "",
    val status: OrderStatus = OrderStatus.CREATED,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val parentOrderId: String? = null,
    var items: List<OrderItem> = emptyList() // populated from subcollection
) : Parcelable

enum class OrderType {
    DINE_IN, PARCEL, DELIVERY;
    
    companion object {
        fun fromString(value: String): OrderType = when(value) {
            "DineIn" -> DINE_IN
            "Parcel" -> PARCEL
            "Delivery" -> DELIVERY
            else -> DINE_IN
        }
    }
    
    fun toDisplayString(): String = when(this) {
        DINE_IN -> "Dine-in"
        PARCEL -> "Parcel"
        DELIVERY -> "Delivery"
    }
    
    fun toFirestoreString(): String = when(this) {
        DINE_IN -> "DineIn"
        PARCEL -> "Parcel"
        DELIVERY -> "Delivery"
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
    
    fun toFirestoreString(): String = when(this) {
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


