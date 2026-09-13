package com.shukranibusiness.app.ui.stock

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.databinding.ItemStockProductBinding
import com.shukranibusiness.app.util.CurrencyFormatter

class StockAdapter(
    private val onEdit: (Product) -> Unit,
    private val onRestock: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

    private var products: List<Product> = emptyList()

    fun submit(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        val lowStock = product.quantity <= product.lowStockThreshold

        holder.binding.stockProductName.text = product.name
        holder.binding.stockProductCategory.text = product.category
        holder.binding.stockProductPrice.text = CurrencyFormatter.formatBoth(product.priceCdf, product.priceUsd)
        holder.binding.stockProductQuantity.text = if (lowStock) {
            "Stock : ${product.quantity} ⚠ stock bas"
        } else {
            "Stock : ${product.quantity}"
        }
        holder.binding.stockProductQuantity.setTextColor(
            if (lowStock) Color.parseColor("#D32F2F") else Color.parseColor("#212121")
        )
        holder.binding.editButton.setOnClickListener { onEdit(product) }
        holder.binding.restockButton.setOnClickListener { onRestock(product) }
        holder.binding.deleteButton.setOnClickListener { onDelete(product) }
    }

    override fun getItemCount(): Int = products.size

    class ViewHolder(val binding: ItemStockProductBinding) : RecyclerView.ViewHolder(binding.root)
}
