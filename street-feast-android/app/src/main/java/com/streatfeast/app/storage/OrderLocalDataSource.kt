package com.streatfeast.app.storage

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class OrderLocalDataSource(
    private val db: StreetFeastDatabase
) {

    private val orderDao = db.orderDao()

    fun observeOrders(storeId: String, status: OrderStatus): Flow<List<Order>> {
        return orderDao.observeByStatus(storeId, status.toRemoteValue())
            .map { list -> list.map { it.toModel() } }
    }

    fun observeOrdersByType(storeId: String, status: OrderStatus, type: OrderType): Flow<List<Order>> {
        return orderDao.observeByStatusAndType(storeId, status.toRemoteValue(), type.toRemoteValue())
            .map { list -> list.map { it.toModel() } }
    }

    fun observeOrdersByStatuses(storeId: String, statuses: List<OrderStatus>): Flow<List<Order>> {
        val statusStrings = statuses.map { it.toRemoteValue() }
        return orderDao.observeByStatuses(storeId, statusStrings)
            .map { list -> list.map { it.toModel() } }
    }

    suspend fun getOrder(orderId: String): Order? {
        return orderDao.getOrder(orderId)?.toModel()
    }

    suspend fun replaceStoreData(
        storeId: String,
        orders: List<OrderEntity>,
        items: List<OrderItemEntity>
    ) {
        db.withTransaction {
            orderDao.clearForStore(storeId)
            if (orders.isNotEmpty()) {
                orderDao.upsertOrders(orders)
            }
            if (items.isNotEmpty()) {
                orderDao.upsertItems(items)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun OrderWithItems.toModel(): Order {
    val orderType = order.type?.let { OrderType.fromString(it) } ?: OrderType.DINE_IN
    val status = OrderStatus.fromString(order.status)
    return Order(
        id = order.id,
        orderNumber = order.orderNumber ?: 0,
        type = orderType,
        chefTip = order.chefTip.orEmpty(),
        status = status,
        createdBy = order.createdBy.orEmpty(),
        createdAt = Instant.ofEpochMilli(order.createdAt),   // <-- Long -> Instant
        updatedAt = Instant.ofEpochMilli(order.updatedAt),   // <-- Long -> Instant
        parentOrderId = order.parentOrderId,
        tableNumber = order.tableNumber,
        licensePlate = order.licensePlate,
        items = items.map { it.toModel() }
    )
}

private fun OrderItemEntity.toModel(): OrderItem = OrderItem(
    id = id,
    itemId = id,
    nameSnapshot = name,
    size = size,
    vegFlagSnapshot = vegFlag ?: "Veg",
    qty = quantity,
    chefTip = chefTip ?: "",
    isPrepared = isPrepared
)
