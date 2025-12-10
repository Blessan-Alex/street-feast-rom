package com.streatfeast.app.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseOrderDto(
    val id: String,
    @SerialName("store_id") val storeId: String,
    val number: Int? = null,
    val status: String,
    val type: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("chef_tip") val chefTip: String? = null,
    @SerialName("parent_order_id") val parentOrderId: String? = null,
    @SerialName("table_number") val tableNumber: Int? = null,
    @SerialName("license_plate") val licensePlate: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SupabaseOrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    val sku: String? = null,  // Item ID (menu item UUID) - nullable because it may not always be present
    val name: String,
    val size: String? = null,
    @SerialName("veg_flag") val vegFlag: String? = null,
    val quantity: Int = 1,
    @SerialName("is_prepared") val isPrepared: Boolean? = null,
    val modifiers: Map<String, String>? = null  // JSONB field containing chefTip
)

@Serializable
data class SupabaseUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val role: String? = null,
    @SerialName("store_id") val storeId: String? = null
)

@Serializable
data class SupabaseStoreDto(
    val id: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class OccupiedTableResult(
    @SerialName("table_number") val tableNumber: Int
)

