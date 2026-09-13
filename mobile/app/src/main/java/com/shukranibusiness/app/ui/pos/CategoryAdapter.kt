package com.shukranibusiness.app.ui.pos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.shukranibusiness.app.databinding.ItemCategoryChipBinding

class CategoryAdapter(
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var categories: List<String> = emptyList()
    private var selected: String = ""

    fun submit(newCategories: List<String>, newSelected: String) {
        categories = newCategories
        selected = newSelected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.chip.text = category
        holder.chip.isChecked = category == selected
        holder.chip.setOnClickListener { onSelect(category) }
    }

    override fun getItemCount(): Int = categories.size

    class ViewHolder(binding: ItemCategoryChipBinding) : RecyclerView.ViewHolder(binding.root) {
        val chip: Chip = binding.categoryChip
    }
}
