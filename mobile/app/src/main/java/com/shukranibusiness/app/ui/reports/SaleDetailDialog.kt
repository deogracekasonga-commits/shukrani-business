package com.shukranibusiness.app.ui.reports

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.databinding.DialogSaleDetailBinding
import com.shukranibusiness.app.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleDetailDialog(
    private val sale: Sale,
    private val items: List<SaleItem>,
    private val canCancel: Boolean,
    private val onCancelConfirmed: (Sale) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSaleDetailBinding.inflate(layoutInflater)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

        for (item in items) {
            val row = TextView(requireContext()).apply {
                text = "${item.quantity} × ${item.productName} — " +
                    CurrencyFormatter.formatBoth(item.subtotalCdf, item.subtotalUsd)
                textSize = 14f
                gravity = Gravity.START
                setPadding(0, 6, 0, 6)
            }
            binding.itemsContainer.addView(row)
        }

        binding.totalText.text = "${getString(R.string.label_total)} : " +
            CurrencyFormatter.formatBoth(sale.totalCdf, sale.totalUsd) +
            " — ${sale.employeeName}, ${sdf.format(Date(sale.dateTimeMillis))}"

        if (sale.status == SaleStatus.CANCELED) {
            val canceledAt = sale.canceledAtMillis?.let { sdf.format(Date(it)) } ?: ""
            binding.statusText.text = getString(
                R.string.sale_status_canceled,
                canceledAt,
                sale.canceledByEmployeeName ?: ""
            )
            binding.statusText.visibility = android.view.View.VISIBLE
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.sale_detail_title)
            .setView(binding.root)
            .setPositiveButton(R.string.action_close, null)

        if (canCancel && sale.status == SaleStatus.COMPLETED) {
            builder.setNegativeButton(R.string.action_cancel_sale) { _, _ -> confirmCancel() }
        }

        return builder.create()
    }

    private fun confirmCancel() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_cancel_sale)
            .setMessage(R.string.confirm_cancel_sale)
            .setPositiveButton(R.string.action_cancel_sale) { _, _ -> onCancelConfirmed(sale) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
