package com.shukranibusiness.app.util

import android.content.Context
import android.util.Log
import com.shukranibusiness.app.data.CartLine
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.entities.Sale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrinter {

    private const val TAG = "ReceiptPrinter"

    suspend fun printReceiptIfConfigured(
        context: Context,
        sale: Sale,
        lines: List<CartLine>,
        shopName: String
    ) {
        val address = Prefs(context).printerAddress ?: return
        val receiptLines = buildReceiptLines(shopName, sale, lines)

        withContext(Dispatchers.IO) {
            try {
                EscPosPrinter.printLines(address, receiptLines)
            } catch (e: Exception) {
                // L'échec d'impression ne doit jamais annuler une vente déjà enregistrée.
                Log.w(TAG, "Impression du reçu impossible : ${e.message}")
            }
        }
    }

    private fun buildReceiptLines(shopName: String, sale: Sale, lines: List<CartLine>): List<String> {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val out = mutableListOf<String>()
        out += shopName
        out += sdf.format(Date(sale.dateTimeMillis))
        out += "Vendeur : ${sale.employeeName}"
        out += "--------------------------------"
        for (line in lines) {
            out += line.product.name
            out += "  ${line.quantity} x ${CurrencyFormatter.formatBoth(line.product.priceCdf, line.product.priceUsd)}"
            out += "  = ${CurrencyFormatter.formatBoth(line.subtotalCdf, line.subtotalUsd)}"
        }
        out += "--------------------------------"
        out += "TOTAL : ${CurrencyFormatter.formatBoth(sale.totalCdf, sale.totalUsd)}"
        out += "Payé en ${sale.currencyPaid}"
        out += ""
        out += "Merci pour votre achat !"
        return out
    }
}
