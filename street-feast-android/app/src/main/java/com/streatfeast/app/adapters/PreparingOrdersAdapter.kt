package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.databinding.ItemPreparingOrderBinding
import com.streatfeast.app.models.Order
import com.streatfeast.app.utils.DateTimeUtils

class PreparingOrdersAdapter(
    private val onMarkPreparedClick: (Order) -> Unit
) : ListAdapter<Order, PreparingOrdersAdapter.ViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPreparingOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onMarkPreparedClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemPreparingOrderBinding,
        private val onMarkPreparedClick: (Order) -> Unit
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
                
                // Items list with details
                val itemsList = order.items.joinToString("\n") { item ->
                    "${item.qty}× ${item.getDisplayName()}"
                }
                tvItems.text = itemsList.ifEmpty { "No items" }
                
                // Chef tip
                if (order.chefTip.isNotBlank()) {
                    tvChefTipLabel.visibility = View.VISIBLE
                    tvChefTip.visibility = View.VISIBLE
                    tvChefTip.text = order.chefTip
                } else {
                    tvChefTipLabel.visibility = View.GONE
                    tvChefTip.visibility = View.GONE
                }
                
                // Mark prepared button
                btnMarkPrepared.setOnClickListener {
                    onMarkPreparedClick(order)
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


