package com.shukranibusiness.app.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.databinding.ItemSaleBinding
import com.shukranibusiness.app.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleAdapter : RecyclerView.Adapter<SaleAdapter.ViewHolder>() {

    private var sales: List<Sale> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    fun submit(newSales: List<Sale>) {
        sales = newSales
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sale = sales[position]
        holder.binding.saleDate.text = dateFormat.format(Date(sale.dateTimeMillis))
        holder.binding.saleEmployee.text = sale.employeeName
        holder.binding.saleTotal.text = CurrencyFormatter.formatBoth(sale.totalCdf, sale.totalUsd)
    }

    override fun getItemCount(): Int = sales.size

    class ViewHolder(val binding: ItemSaleBinding) : RecyclerView.ViewHolder(binding.root)
}
