package com.shukranibusiness.app.ui.pos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.databinding.ItemProductBinding
import com.shukranibusiness.app.util.CurrencyFormatter

class ProductAdapter(
    private val onAdd: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private var products: List<Product> = emptyList()

    fun submit(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.binding.productName.text = product.name
        holder.binding.productPrice.text =
            CurrencyFormatter.formatBoth(product.priceCdf, product.priceUsd)
        holder.binding.productStock.text = "Stock : ${product.quantity}"
        holder.binding.productRoot.setOnClickListener { onAdd(product) }
    }

    override fun getItemCount(): Int = products.size

    class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)
}
