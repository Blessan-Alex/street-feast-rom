package com.streatfeast.app.utils

import com.streatfeast.app.adapters.TableChip
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderType

object TableDisplayMapper {

    /**
     * Maps an order to a TableChip for display. Returns null if the order
     * should not render a chip (e.g., invalid dine-in table number).
     */
    fun toChip(order: Order, tableCount: Int = Constants.DEFAULT_TABLE_COUNT): TableChip? {
        return when (order.type) {
            OrderType.DINE_IN -> {
                val table = order.tableNumber
                if (table != null && table in 1..tableCount) {
                    TableChip(
                        tableNumber = table,
                        orderNumber = order.orderNumber ?: 0,
                        orderId = order.id,
                        label = "Table $table #${order.orderNumber ?: ""}"
                    )
                } else {
                    null
                }
            }
            OrderType.EAT_AWAY -> {
                val plate = order.licensePlate?.takeIf { it.isNotBlank() } ?: "----"
                TableChip(
                    tableNumber = 0,
                    orderNumber = order.orderNumber ?: 0,
                    orderId = order.id,
                    label = "EatAway $plate #${order.orderNumber ?: ""}"
                )
            }
            OrderType.PARCEL -> {
                TableChip(
                    tableNumber = 0,
                    orderNumber = order.orderNumber ?: 0,
                    orderId = order.id,
                    label = "Parcel #${order.orderNumber ?: ""}"
                )
            }
            else -> null
        }
    }
}

