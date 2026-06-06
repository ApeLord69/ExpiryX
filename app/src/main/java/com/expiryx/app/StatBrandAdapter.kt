package com.expiryx.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class StatBrandAdapter(
    private var items: List<BrandStat> = emptyList(),
    private var maxCount: Int = 1,
) : RecyclerView.Adapter<StatBrandAdapter.VH>() {

    fun submitList(newItems: List<BrandStat>) {
        items = newItems
        maxCount = newItems.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtBrand: TextView = view.findViewById(R.id.txtBrandName)
        val txtCount: TextView = view.findViewById(R.id.txtBrandCount)
        val progress: ProgressBar = view.findViewById(R.id.brandProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stat_brand_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.txtBrand.text = item.brand
        holder.txtCount.text = holder.itemView.context.getString(R.string.stats_brand_count, item.count)
        holder.progress.max = maxCount
        holder.progress.progress = item.count
        holder.progress.progressTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.grey_800)
    }

    override fun getItemCount(): Int = items.size
}
