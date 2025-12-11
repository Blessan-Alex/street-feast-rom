package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.R
import com.streatfeast.app.models.MenuItem

class MostBoughtAdapter(
    private val onItemClick: (MenuItem) -> Unit,
    private val onAddClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, MostBoughtAdapter.MostBoughtViewHolder>(MostBoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MostBoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_most_bought, parent, false)
        return MostBoughtViewHolder(view)
    }

    override fun onBindViewHolder(holder: MostBoughtViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick, onAddClick)
    }

    inner class MostBoughtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val dotGreen: View = itemView.findViewById(R.id.dotGreen)
        private val dotRed: View = itemView.findViewById(R.id.dotRed)
        private val btnAdd: ViewGroup = itemView.findViewById(R.id.btnAdd)

        fun bind(item: MenuItem, onItemClick: (MenuItem) -> Unit, onAddClick: (MenuItem) -> Unit) {
            tvName.text = item.name
            
            if (item.sizes.isNullOrEmpty()) {
                tvQty.visibility = View.GONE
            } else {
                tvQty.visibility = View.VISIBLE
                tvQty.text = "qnty: ${item.sizes.joinToString(",")}"
            }

            // Show dots based on veg flag
            when (item.vegFlag) {
                "Veg" -> {
                    dotGreen.visibility = View.VISIBLE
                    dotRed.visibility = View.GONE
                }
                "NonVeg" -> {
                    dotGreen.visibility = View.GONE
                    dotRed.visibility = View.VISIBLE
                }
                else -> {
                    dotGreen.visibility = View.VISIBLE
                    dotRed.visibility = View.VISIBLE
                }
            }

            // Handle item card click
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Handle add button click
            btnAdd.setOnClickListener {
                onAddClick(item)
            }
        }
    }

    private class MostBoughtDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem == newItem
        }
    }
}



