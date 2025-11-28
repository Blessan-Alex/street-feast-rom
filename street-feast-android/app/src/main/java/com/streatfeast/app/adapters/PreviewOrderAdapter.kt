package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.R
import com.streatfeast.app.models.OrderItem

class PreviewOrderAdapter(
    private val items: List<OrderItem>,
    private val onQuantityDecrease: (OrderItem) -> Unit,
    private val onQuantityIncrease: (OrderItem) -> Unit,
    private val onAlterClick: (OrderItem) -> Unit,
    private val onRemoveClick: (OrderItem) -> Unit
) : RecyclerView.Adapter<PreviewOrderAdapter.PreviewOrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_order_card, parent, false)
        return PreviewOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewOrderViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class PreviewOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvTip: TextView = itemView.findViewById(R.id.tvTip)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val btnAlter: TextView = itemView.findViewById(R.id.btnAlter)
        private val btnMinus: CardView = itemView.findViewById(R.id.btnMinus)
        private val btnPlus: CardView = itemView.findViewById(R.id.btnPlus)
        private val btnRemove: View = itemView.findViewById(R.id.btnRemove)

        fun bind(item: OrderItem) {
            tvName.text = item.nameSnapshot
            
            // Display amount/size
            val amountText = if (item.size != null) {
                "amount : ${item.size.lowercase()}"
            } else {
                "amount : standard"
            }
            tvAmount.text = amountText
            
            // Display chef tip
            val tipText = if (item.chefTip.isNotBlank()) {
                "Chef tip : ${item.chefTip}"
            } else {
                "Chef tip : none"
            }
            tvTip.text = tipText
            
            // Display quantity
            tvQty.text = item.qty.toString()
            
            // Handle quantity decrease
            btnMinus.setOnClickListener {
                if (item.qty > 1) {
                    onQuantityDecrease(item)
                }
            }
            
            // Handle quantity increase
            btnPlus.setOnClickListener {
                if (item.qty < 99) {
                    onQuantityIncrease(item)
                }
            }
            
            // Handle alter order
            btnAlter.setOnClickListener {
                onAlterClick(item)
            }
            
            // Handle remove
            btnRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }
}

