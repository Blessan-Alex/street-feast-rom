package com.streatfeast.app.dialogs

import android.app.AlertDialog
import android.content.Context
import com.streatfeast.app.models.OrderStatus

object OrderModificationDialog {
    
    /**
     * Shows a confirmation dialog when modifying an order that is being prepared.
     * Returns true if user confirms, false if cancelled.
     */
    fun showPreparingOrderConfirmation(
        context: Context,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle("Modify Order?")
            .setMessage("This order is being prepared. Are you sure you want to modify it?")
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel") { _, _ -> onCancel?.invoke() }
            .show()
    }
    
    /**
     * Shows a confirmation dialog for total update warning.
     */
    fun showTotalUpdateConfirmation(
        context: Context,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle("Update Order Total?")
            .setMessage("This will update the order total. Continue?")
            .setPositiveButton("Continue") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel") { _, _ -> onCancel?.invoke() }
            .show()
    }
    
    /**
     * Shows a confirmation dialog for deleting an item.
     */
    fun showDeleteItemConfirmation(
        context: Context,
        itemName: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle("Delete Item?")
            .setMessage("Are you sure you want to delete $itemName from this order?")
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel") { _, _ -> onCancel?.invoke() }
            .show()
    }
    
    /**
     * Checks if an order status allows modification.
     * Returns true if modification is allowed, false otherwise.
     */
    fun canModifyOrder(status: OrderStatus): Boolean {
        return status != OrderStatus.DELIVERED &&
                status != OrderStatus.CLOSED &&
                status != OrderStatus.CANCELED
    }
    
    /**
     * Checks if an order status requires a warning before modification.
     * Returns true if warning should be shown, false otherwise.
     */
    fun requiresWarning(status: OrderStatus): Boolean {
        return status == OrderStatus.IN_KITCHEN || status == OrderStatus.PREPARED
    }
    
    /**
     * Gets a user-friendly error message for orders that cannot be modified.
     */
    fun getModificationErrorMessage(status: OrderStatus): String {
        return when (status) {
            OrderStatus.DELIVERED -> "Cannot modify a delivered order"
            OrderStatus.CLOSED -> "Cannot modify a closed order"
            OrderStatus.CANCELED -> "Cannot modify a canceled order"
            else -> "Cannot modify this order"
        }
    }
}


