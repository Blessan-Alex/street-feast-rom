package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.databinding.ItemReadyOrderBinding
import com.streatfeast.app.models.Order
import com.streatfeast.app.utils.DateTimeUtils

class ReadyOrdersAdapter(
    private val onMarkDeliveredClick: (Order) -> Unit
) : ListAdapter<Order, ReadyOrdersAdapter.ViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReadyOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onMarkDeliveredClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemReadyOrderBinding,
        private val onMarkDeliveredClick: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.apply {
                // Order number
                tvOrderNumber.text = "Order #${order.orderNumber}"
                
                // Order type chip
                chipOrderType.text = order.type.toDisplayString()
                chipOrderType.setChipBackgroundColorResource(order.status.colorRes)
                
                // Time since prepared
                tvTime.text = "Ready ${DateTimeUtils.getTimeAgo(order.updatedAt)}"
                
                // Items summary
                val itemsSummary = order.items.joinToString("\n") { item ->
                    "${item.qty}× ${item.getDisplayName()}"
                }
                tvItemsSummary.text = itemsSummary.ifEmpty { "No items" }
                
                // Mark delivered button
                btnMarkDelivered.setOnClickListener {
                    onMarkDeliveredClick(order)
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


