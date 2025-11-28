package com.streatfeast.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.R
import com.streatfeast.app.models.MenuItem

class MenuItemAdapter(
    private val items: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit,
    private val onAddClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_card, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val dotG: View = itemView.findViewById(R.id.dotG)
        private val dotR: View = itemView.findViewById(R.id.dotR)
        private val btnAdd: FrameLayout = itemView.findViewById(R.id.btnAdd)

        fun bind(item: MenuItem) {
            tvName.text = item.name
            tvDesc.text = "qnty: ${item.getSizesDisplay()}"

            // Show dots based on veg flag
            when {
                item.isVeg -> {
                    dotG.visibility = View.VISIBLE
                    dotR.visibility = View.GONE
                }
                item.isNonVeg -> {
                    dotG.visibility = View.GONE
                    dotR.visibility = View.VISIBLE
                }
                item.hasBoth -> {
                    dotG.visibility = View.VISIBLE
                    dotR.visibility = View.VISIBLE
                }
                else -> {
                    dotG.visibility = View.GONE
                    dotR.visibility = View.GONE
                }
            }

            // Handle item card click
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Handle plus button click
            btnAdd.setOnClickListener {
                onAddClick(item)
            }
        }
    }
}

