package com.streatfeast.app.ui

import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.utils.Constants

object OrderDisplayMapper {
    private const val DEFAULT_TABLE_COUNT = Constants.DEFAULT_TABLE_COUNT

    fun locationLabel(order: Order, tableCount: Int = DEFAULT_TABLE_COUNT): String {
        return when (order.type) {
            OrderType.DINE_IN -> {
                val table = order.tableNumber
                if (table != null && table in 1..tableCount) {
                    "Table $table"
                } else {
                    "Table ?"
                }
            }
            OrderType.EAT_AWAY -> {
                val plate = order.licensePlate?.takeIf { it.length == 4 } ?: "----"
                "EatAway $plate"
            }
            OrderType.PARCEL -> "Parcel"
        }
    }

    fun headerLabel(order: Order, tableCount: Int = DEFAULT_TABLE_COUNT): String {
        return "${locationLabel(order, tableCount)} · #${order.orderNumber}"
    }
}


