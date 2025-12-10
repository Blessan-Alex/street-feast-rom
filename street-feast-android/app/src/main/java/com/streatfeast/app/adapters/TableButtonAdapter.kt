package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.R

data class TableButtonUi(
    val number: Int,
    val occupied: Boolean,
    val selected: Boolean
)

class TableButtonAdapter(
    private val onClick: (Int) -> Unit
) : ListAdapter<TableButtonUi, TableButtonAdapter.TableViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_button, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView as CardView
        private val title: TextView = itemView.findViewById(R.id.tvTable)

        fun bind(ui: TableButtonUi, onClick: (Int) -> Unit) {
            title.text = if (ui.occupied) "${ui.number} · Occupied" else ui.number.toString()
            title.isSingleLine = true
            title.textSize = 18f

            val ctx = itemView.context
            when {
                ui.occupied -> {
                    card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.sf_surface_soft2))
                    title.setTextColor(ContextCompat.getColor(ctx, R.color.sf_text_secondary))
                    title.setTypeface(null, android.graphics.Typeface.NORMAL)
                    card.isEnabled = false
                    card.alpha = 0.6f
                }
                ui.selected -> {
                    card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.sf_primary))
                    title.setTextColor(ContextCompat.getColor(ctx, R.color.sf_white))
                    title.setTypeface(null, android.graphics.Typeface.BOLD)
                    card.isEnabled = true
                    card.alpha = 1f
                }
                else -> {
                    card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.sf_surface_soft2))
                    title.setTextColor(ContextCompat.getColor(ctx, R.color.sf_text_primary))
                    title.setTypeface(null, android.graphics.Typeface.NORMAL)
                    card.isEnabled = true
                    card.alpha = 1f
                }
            }

            card.setOnClickListener {
                if (!ui.occupied) onClick(ui.number)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TableButtonUi>() {
        override fun areItemsTheSame(oldItem: TableButtonUi, newItem: TableButtonUi): Boolean =
            oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: TableButtonUi, newItem: TableButtonUi): Boolean =
            oldItem == newItem
    }
}

