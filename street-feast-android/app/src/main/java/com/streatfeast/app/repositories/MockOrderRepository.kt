package com.streatfeast.app.repositories

import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay

class MockOrderRepository {
    
    private val mockOrders = mutableListOf<Order>()
    
    init {
        generateMockOrders()
    }
    
    private fun generateMockOrders() {
        // 2 New Orders (Created status) - for Chef's "New Orders" tab
        mockOrders.addAll(listOf(
            Order(
                id = "order_001",
                orderNumber = 101,
                type = OrderType.DINE_IN,
                chefTip = "Extra spicy, no onions",
                status = OrderStatus.CREATED,
                createdBy = "admin",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                items = listOf(
                    OrderItem("item_001", "item_001", "Chicken Burger", "Large", "NonVeg", 2),
                    OrderItem("item_002", "item_002", "Caesar Salad", null, "Veg", 1)
                )
            ),
            Order(
                id = "order_002",
                orderNumber = 102,
                type = OrderType.PARCEL,
                chefTip = "",
                status = OrderStatus.CREATED,
                createdBy = "admin",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                items = listOf(
                    OrderItem("item_003", "item_003", "Margherita Pizza", "Large", "Veg", 1),
                    OrderItem("item_004", "item_004", "French Fries", "Small", "Veg", 2)
                )
            )
        ))
        
        // 1 Preparing Order (InKitchen status) - for Chef's "Preparing" tab
        mockOrders.addAll(listOf(
            Order(
                id = "order_003",
                orderNumber = 103,
                type = OrderType.DELIVERY,
                chefTip = "No cheese, extra vegetables",
                status = OrderStatus.IN_KITCHEN,
                createdBy = "admin",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                items = listOf(
                    OrderItem("item_005", "item_005", "Veggie Wrap", "Large", "Veg", 1),
                    OrderItem("item_006", "item_006", "Chicken Wings", null, "NonVeg", 6)
                )
            )
        ))
        
        // 1 Ready Order (Prepared status) - for Waiter's "Ready" tab
        mockOrders.addAll(listOf(
            Order(
                id = "order_004",
                orderNumber = 104,
                type = OrderType.DINE_IN,
                chefTip = "Well done",
                status = OrderStatus.PREPARED,
                createdBy = "admin",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                items = listOf(
                    OrderItem("item_007", "item_007", "Grilled Chicken", "Large", "NonVeg", 1),
                    OrderItem("item_008", "item_008", "Rice Bowl", "Small", "Veg", 1)
                )
            )
        ))
    }
    
    /**
     * Get new orders (Created status)
     */
    suspend fun getNewOrders(): List<Order> {
        delay(500) // Simulate network delay
        return mockOrders.filter { it.status == OrderStatus.CREATED }
    }
    
    /**
     * Get preparing orders (InKitchen status)
     */
    suspend fun getPreparingOrders(): List<Order> {
        delay(500)
        return mockOrders.filter { it.status == OrderStatus.IN_KITCHEN }
    }
    
    /**
     * Get ready orders (Prepared status)
     */
    suspend fun getReadyOrders(): List<Order> {
        delay(500)
        return mockOrders.filter { it.status == OrderStatus.PREPARED }
    }
    
    /**
     * Accept order - moves from Created to InKitchen
     */
    suspend fun acceptOrder(orderId: String): Result<Unit> {
        delay(300) // Simulate network delay
        val order = mockOrders.find { it.id == orderId }
        order?.let {
            val index = mockOrders.indexOf(it)
            mockOrders[index] = it.copy(
                status = OrderStatus.IN_KITCHEN,
                updatedAt = Timestamp.now()
            )
        }
        return Result.success(Unit)
    }
    
    /**
     * Mark order as prepared - moves from InKitchen to Prepared
     */
    suspend fun markPrepared(orderId: String): Result<Unit> {
        delay(300)
        val order = mockOrders.find { it.id == orderId }
        order?.let {
            val index = mockOrders.indexOf(it)
            mockOrders[index] = it.copy(
                status = OrderStatus.PREPARED,
                updatedAt = Timestamp.now()
            )
        }
        return Result.success(Unit)
    }
    
    /**
     * Mark order as delivered - moves from Prepared to Delivered
     */
    suspend fun markDelivered(orderId: String): Result<Unit> {
        delay(300)
        val order = mockOrders.find { it.id == orderId }
        order?.let {
            val index = mockOrders.indexOf(it)
            mockOrders[index] = it.copy(
                status = OrderStatus.DELIVERED,
                updatedAt = Timestamp.now()
            )
        }
        return Result.success(Unit)
    }
    
    /**
     * Get order items for a specific order
     */
    suspend fun getOrderItems(orderId: String): Result<List<OrderItem>> {
        delay(200)
        val order = mockOrders.find { it.id == orderId }
        return if (order != null) {
            Result.success(order.items)
        } else {
            Result.failure(Exception("Order not found"))
        }
    }
}
