package com.streatfeast.app.utils

object Constants {
    // Firestore Collections
    const val STORES_COLLECTION = "stores"
    const val ORDERS_COLLECTION = "orders"
    const val ORDER_ITEMS_COLLECTION = "orderItems"
    const val CATEGORIES_COLLECTION = "categories"
    const val ITEMS_COLLECTION = "items"
    const val USERS_COLLECTION = "users"
    
    // Default Store ID (can be made dynamic in future)
    const val DEFAULT_STORE_ID = "default"
    
    // Order Status Values (Firestore)
    const val STATUS_CREATED = "Created"
    const val STATUS_ACCEPTED = "Accepted"
    const val STATUS_IN_KITCHEN = "InKitchen"
    const val STATUS_PREPARED = "Prepared"
    const val STATUS_DELIVERED = "Delivered"
    const val STATUS_CLOSED = "Closed"
    const val STATUS_CANCELED = "Canceled"
    
    // Order Type Values (Firestore)
    const val TYPE_DINE_IN = "DineIn"
    const val TYPE_PARCEL = "Parcel"
    const val TYPE_DELIVERY = "Delivery"
    
    // Notification Channel
    const val NOTIFICATION_CHANNEL_ID = "order_updates"
    const val NOTIFICATION_CHANNEL_NAME = "Order Updates"
    
    // Shared Preferences
    const val PREFS_NAME = "StreetFeastPrefs"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_USER_ID = "user_id"
    
    // Intent Extras
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_ORDER = "order"
    
    // Sound Types
    const val SOUND_PING = "ping"
    const val SOUND_CLICK = "click"
    const val SOUND_BUZZER = "buzzer"
}


