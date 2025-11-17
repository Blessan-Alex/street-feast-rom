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
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SupabaseOrderItemDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    val name: String,
    val size: String? = null,
    @SerialName("veg_flag") val vegFlag: String? = null,
    val quantity: Int = 1
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

