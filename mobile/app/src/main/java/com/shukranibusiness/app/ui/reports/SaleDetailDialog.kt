package com.shukranibusiness.app.ui.reports

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.databinding.DialogSaleDetailBinding
import com.shukranibusiness.app.util.CurrencyFormatter
import com.shukranibusiness.app.util.PaymentMethodFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleDetailDialog(
    private val sale: Sale,
    private val items: List<SaleItem>,
    private val canCancel: Boolean,
    private val onCancelConfirmed: (Sale) -> Unit,
    private val onPaymentRecorded: (Sale, Double, Double) -> Unit = { _, _, _ -> }
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

        val discountLine = if (sale.discountCdf > 0.0 || sale.discountUsd > 0.0) {
            "\n${getString(R.string.label_subtotal)} : ${CurrencyFormatter.formatBoth(sale.subtotalCdf, sale.subtotalUsd)}" +
                " — ${getString(R.string.label_discount)} : -${CurrencyFormatter.formatBoth(sale.discountCdf, sale.discountUsd)}"
        } else {
            ""
        }
        binding.totalText.text = "${getString(R.string.label_total)} : " +
            CurrencyFormatter.formatBoth(sale.totalCdf, sale.totalUsd) +
            " — ${sale.employeeName}, ${sdf.format(Date(sale.dateTimeMillis))}" +
            discountLine +
            "\n${getString(R.string.label_payment_method)} : ${PaymentMethodFormatter.label(sale.paymentMethod, sale.mobileMoneyProvider)}"

        val dueCdf = sale.totalCdf - sale.amountPaidCdf
        val dueUsd = sale.totalUsd - sale.amountPaidUsd
        val isCredit = dueCdf > 0.0 || dueUsd > 0.0
        if (isCredit) {
            val customerLine = sale.customerReference?.let { "\n${getString(R.string.hint_customer_reference)} : $it" } ?: ""
            binding.creditStatusText.text = "${getString(R.string.label_amount_paid)} : " +
                CurrencyFormatter.formatBoth(sale.amountPaidCdf, sale.amountPaidUsd) +
                "  —  ${getString(R.string.label_amount_due)} : " +
                CurrencyFormatter.formatBoth(dueCdf, dueUsd) +
                customerLine
            binding.creditStatusText.visibility = android.view.View.VISIBLE
        }

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

        if (isCredit && sale.status == SaleStatus.COMPLETED) {
            builder.setNeutralButton(R.string.action_record_payment) { _, _ -> promptPayment(dueCdf, dueUsd) }
        }

        return builder.create()
    }

    private fun promptPayment(dueCdf: Double, dueUsd: Double) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.hint_payment_amount)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_record_payment)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val raw = input.text.toString().toDoubleOrNull() ?: 0.0
                if (raw <= 0.0) return@setPositiveButton
                val (addCdf, addUsd) = if (sale.currencyPaid == "USD") {
                    val usd = raw.coerceIn(0.0, dueUsd)
                    val cdf = if (dueUsd > 0.0) dueCdf * (usd / dueUsd) else 0.0
                    cdf to usd
                } else {
                    val cdf = raw.coerceIn(0.0, dueCdf)
                    val usd = if (dueCdf > 0.0) dueUsd * (cdf / dueCdf) else 0.0
                    cdf to usd
                }
                onPaymentRecorded(sale, addCdf, addUsd)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
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
