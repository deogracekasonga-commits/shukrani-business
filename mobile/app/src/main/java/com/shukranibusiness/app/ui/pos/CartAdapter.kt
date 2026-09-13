package com.shukranibusiness.app.ui.pos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shukranibusiness.app.data.CartLine
import com.shukranibusiness.app.databinding.ItemCartLineBinding
import com.shukranibusiness.app.util.CurrencyFormatter

class CartAdapter(
    private val onIncrement: (Long) -> Unit,
    private val onDecrement: (Long) -> Unit,
    private val onRemove: (Long) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    private var lines: List<CartLine> = emptyList()

    fun submit(newLines: List<CartLine>) {
        lines = newLines
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = lines[position]
        val productId = line.product.id
        holder.binding.lineName.text = line.product.name
        holder.binding.lineQuantity.text = line.quantity.toString()
        holder.binding.lineSubtotal.text = CurrencyFormatter.formatBoth(line.subtotalCdf, line.subtotalUsd)
        holder.binding.incrementButton.setOnClickListener { onIncrement(productId) }
        holder.binding.decrementButton.setOnClickListener { onDecrement(productId) }
        holder.binding.removeButton.setOnClickListener { onRemove(productId) }
    }

    override fun getItemCount(): Int = lines.size

    class ViewHolder(val binding: ItemCartLineBinding) : RecyclerView.ViewHolder(binding.root)
}
