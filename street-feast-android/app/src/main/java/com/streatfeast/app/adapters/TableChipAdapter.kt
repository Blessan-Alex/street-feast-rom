package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.R

data class TableChip(
    val tableNumber: Int,
    val orderNumber: Int,
    val orderId: String
)

class TableChipAdapter(
    private val chips: List<TableChip>,
    private val onChipClick: (TableChip) -> Unit
) : RecyclerView.Adapter<TableChipAdapter.ViewHolder>() {

    private var selectedChip: TableChip? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: androidx.cardview.widget.CardView = itemView as androidx.cardview.widget.CardView
        private val tvTableChip: TextView = itemView.findViewById(R.id.tvTableChip)

        fun bind(chip: TableChip) {
            tvTableChip.text = "Table ${chip.tableNumber} #${chip.orderNumber}"
            
            val isSelected = selectedChip?.orderId == chip.orderId
            if (isSelected) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.sf_primary))
                tvTableChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.sf_white))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.sf_chip_bg))
                tvTableChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.sf_text_secondary))
            }
            
            itemView.setOnClickListener {
                selectedChip = chip
                notifyDataSetChanged()
                onChipClick(chip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(chips[position])
    }

    override fun getItemCount(): Int = chips.size

    fun setSelectedChip(chip: TableChip?) {
        selectedChip = chip
        notifyDataSetChanged()
    }
}

