package com.streatfeast.app.navigation

import android.os.Bundle
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.utils.Constants

/**
 * Explicit modes for order flows.
 */
enum class OrderEditMode {
    NEW,
    ADD_ITEMS,
    EDIT
}

/**
 * Single source of truth for order navigation arguments.
 * Keeps order context (type, table, license, edit flags) consistent across fragments.
 */
data class OrderNavArgs(
    val orderType: OrderType = OrderType.DINE_IN,
    val tableNumber: Int? = null,
    val licensePlate: String? = null,
    val showHeader: Boolean = false,
    val existingOrderId: String? = null,
    val editMode: OrderEditMode = OrderEditMode.NEW,
    val chefTip: String? = null,
    val orderId: String? = null
) {
    companion object {
        private const val KEY_ORDER_TYPE = "orderType"
        private const val KEY_TABLE_NUMBER = "tableNumber"
        private const val KEY_LICENSE_PLATE = "licensePlate"
        private const val KEY_SHOW_HEADER = "showHeader"
        private const val KEY_EXISTING_ORDER_ID = "existingOrderId"
        private const val KEY_EDIT_MODE = "editMode"
        private const val KEY_CHEF_TIP = "chefTip"
        private const val KEY_ORDER_ID = "orderId"

        fun from(bundle: Bundle?): OrderNavArgs {
            if (bundle == null) return OrderNavArgs()

            val typeString = bundle.getString(KEY_ORDER_TYPE)
            val orderType = try {
                if (typeString.isNullOrBlank()) OrderType.DINE_IN else OrderType.valueOf(typeString)
            } catch (_: IllegalArgumentException) {
                OrderType.DINE_IN
            }

            val rawTable = if (bundle.containsKey(KEY_TABLE_NUMBER)) {
                bundle.getInt(KEY_TABLE_NUMBER, -1)
            } else {
                -1
            }
            val tableNumber = rawTable.takeIf { it > 0 } ?: null

            val editMode = bundle.getString(KEY_EDIT_MODE)?.let { raw ->
                runCatching { OrderEditMode.valueOf(raw) }.getOrNull()
            } ?: OrderEditMode.NEW

            return OrderNavArgs(
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = bundle.getString(KEY_LICENSE_PLATE),
                showHeader = bundle.getBoolean(KEY_SHOW_HEADER, false),
                existingOrderId = bundle.getString(KEY_EXISTING_ORDER_ID),
                editMode = editMode,
                chefTip = bundle.getString(KEY_CHEF_TIP),
                orderId = bundle.getString(KEY_ORDER_ID)
            )
        }
    }

    /**
     * Helper to fall back to a default table number when one is not set.
     */
    fun effectiveTableNumber(): Int = tableNumber ?: Constants.DEFAULT_TABLE_COUNT

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_ORDER_TYPE, orderType.name)
        tableNumber?.let { putInt(KEY_TABLE_NUMBER, it) }
        licensePlate?.let { putString(KEY_LICENSE_PLATE, it) }
        putBoolean(KEY_SHOW_HEADER, showHeader)
        existingOrderId?.let { putString(KEY_EXISTING_ORDER_ID, it) }
        putString(KEY_EDIT_MODE, editMode.name)
        chefTip?.let { putString(KEY_CHEF_TIP, it) }
        orderId?.let { putString(KEY_ORDER_ID, it) }
    }
}

