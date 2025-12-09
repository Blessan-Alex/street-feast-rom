package com.streatfeast.app.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.R
import com.streatfeast.app.databinding.ItemGivenOrderCardBinding
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.utils.DateTimeUtils

class GivenOrderCardAdapter(
    private val onAlterOrderClick: (Order, OrderItem) -> Unit,
    private val onAddItemsClick: (Order) -> Unit
) : ListAdapter<Order, GivenOrderCardAdapter.ViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGivenOrderCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onAlterOrderClick, onAddItemsClick)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemGivenOrderCardBinding,
        private val onAlterOrderClick: (Order, OrderItem) -> Unit,
        private val onAddItemsClick: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(order: Order) {
            val tableNumber = order.tableNumber ?: extractTableNumber(order)
            binding.tvTableOrder.text = "Table $tableNumber - #${order.orderNumber}"
            binding.tvTimeAgo.text = DateTimeUtils.getTimeAgo(order.updatedAt)
            
            // Clear existing items
            binding.llItems.removeAllViews()
            
            // Add order items
            order.items.forEachIndexed { index, item ->
                val itemView = if (index % 2 == 1) {
                    // Shaded row
                    LayoutInflater.from(binding.root.context)
                        .inflate(R.layout.item_given_order_line_shaded, binding.llItems, false)
                } else {
                    // White row
                    LayoutInflater.from(binding.root.context)
                        .inflate(R.layout.item_given_order_line, binding.llItems, false)
                }
                
                // Find views in the item layout
                val stripe = itemView.findViewById<View>(R.id.stripe)
                val tvItem = itemView.findViewById<TextView>(R.id.tvItem)
                val badge = itemView.findViewById<TextView>(R.id.badge)
                val tvSize = itemView.findViewById<TextView>(R.id.tvSize)
                val tvTips = itemView.findViewById<TextView>(R.id.tvTips)
                val btnAlter = itemView.findViewById<TextView>(R.id.btnAlter)
                
                // Show alter button if order status allows modification
                val canModify = order.status != com.streatfeast.app.models.OrderStatus.DELIVERED &&
                        order.status != com.streatfeast.app.models.OrderStatus.CLOSED &&
                        order.status != com.streatfeast.app.models.OrderStatus.CANCELED
                
                if (canModify) {
                    btnAlter.visibility = View.VISIBLE
                    btnAlter.isEnabled = true
                    btnAlter.setOnClickListener {
                        onAlterOrderClick(order, item)
                    }
                } else {
                    btnAlter.visibility = View.GONE
                }
                
                // Set stripe color based on veg flag
                val stripeColor = if (item.isVeg) {
                    ContextCompat.getColor(binding.root.context, R.color.sf_green)
                } else {
                    ContextCompat.getColor(binding.root.context, R.color.sf_red)
                }
                stripe.setBackgroundColor(stripeColor)
                
                // Bind item data
                tvItem.text = item.nameSnapshot
                if (item.qty > 0) {
                    badge.text = "x${item.qty}"
                    badge.visibility = View.VISIBLE
                } else {
                    badge.visibility = View.GONE
                }
                tvSize.text = if (item.size != null) "Size: ${item.size}" else "Size: -"
                tvTips.text = if (item.chefTip.isNotBlank()) "Tips: ${item.chefTip}" else "Tips: None"
                
                binding.llItems.addView(itemView)
            }
            
            // Handle add items click
            binding.btnAddItems.setOnClickListener {
                onAddItemsClick(order)
            }
        }
        
        private fun extractTableNumber(order: Order): Int {
            // TODO: Extract table number from order metadata
            // For now, using a placeholder - in real implementation, this should come from order
            // Maybe from order metadata or a separate field
            return order.orderNumber % 20 + 1 // Placeholder: derive from order number
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}

