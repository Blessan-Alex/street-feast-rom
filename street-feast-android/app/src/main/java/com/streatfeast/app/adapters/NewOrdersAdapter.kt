package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.databinding.ItemNewOrderBinding
import com.streatfeast.app.models.Order
import com.streatfeast.app.utils.DateTimeUtils

class NewOrdersAdapter(
    private val onAcceptClick: (Order) -> Unit
) : ListAdapter<Order, NewOrdersAdapter.ViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNewOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onAcceptClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemNewOrderBinding,
        private val onAcceptClick: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.apply {
                // Order number
                tvOrderNumber.text = "Order #${order.orderNumber}"
                
                // Order type chip
                chipOrderType.text = order.type.toDisplayString()
                chipOrderType.setChipBackgroundColorResource(order.status.colorRes)
                
                // Time ago
                tvTime.text = DateTimeUtils.getTimeAgo(order.createdAt)
                
                // Items summary
                val itemsSummary = order.items.joinToString("\n") { item ->
                    "${item.qty}× ${item.getDisplayName()}"
                }
                tvItemsSummary.text = itemsSummary.ifEmpty { "No items" }
                
                // Accept button
                btnAccept.setOnClickListener {
                    onAcceptClick(order)
                }
            }
        }
    }
    
    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}


