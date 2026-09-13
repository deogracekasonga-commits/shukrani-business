package com.shukranibusiness.app.ui.reports

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.databinding.ItemSaleBinding
import com.shukranibusiness.app.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleAdapter(
    private val onClick: (Sale) -> Unit
) : RecyclerView.Adapter<SaleAdapter.ViewHolder>() {

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
        val canceled = sale.status == SaleStatus.CANCELED

        holder.binding.saleDate.text = if (canceled) {
            "${dateFormat.format(Date(sale.dateTimeMillis))} — annulée"
        } else {
            dateFormat.format(Date(sale.dateTimeMillis))
        }
        holder.binding.saleEmployee.text = sale.employeeName
        holder.binding.saleTotal.text = CurrencyFormatter.formatBoth(sale.totalCdf, sale.totalUsd)

        val color = if (canceled) Color.parseColor("#9AA3BC") else Color.parseColor("#182238")
        val strike = if (canceled) Paint.STRIKE_THRU_TEXT_FLAG else 0
        for (tv in listOf(holder.binding.saleEmployee, holder.binding.saleTotal)) {
            tv.setTextColor(color)
            tv.paintFlags = tv.paintFlags.let { flags ->
                if (canceled) flags or strike else flags and strike.inv()
            }
        }

        holder.itemView.setOnClickListener { onClick(sale) }
    }

    override fun getItemCount(): Int = sales.size

    class ViewHolder(val binding: ItemSaleBinding) : RecyclerView.ViewHolder(binding.root)
}
