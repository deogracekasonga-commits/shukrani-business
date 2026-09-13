package com.shukranibusiness.app.util

import android.content.Context
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.SaleStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportSales(context: Context, sales: List<Sale>, itemsBySale: Map<Long, List<SaleItem>>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "ventes_${System.currentTimeMillis()}.csv")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

        file.bufferedWriter().use { writer ->
            writer.write(
                "Date,Employé,Produit,Quantité,Prix unitaire CDF,Prix unitaire USD," +
                    "Sous-total CDF,Sous-total USD,Devise payée,Statut\n"
            )
            for (sale in sales) {
                val statusLabel = if (sale.status == SaleStatus.CANCELED) "ANNULÉE" else "VALIDÉE"
                for (item in itemsBySale[sale.id].orEmpty()) {
                    val row = listOf(
                        sdf.format(Date(sale.dateTimeMillis)),
                        sale.employeeName,
                        item.productName,
                        item.quantity.toString(),
                        item.unitPriceCdf.toString(),
                        item.unitPriceUsd.toString(),
                        item.subtotalCdf.toString(),
                        item.subtotalUsd.toString(),
                        sale.currencyPaid,
                        statusLabel
                    ).joinToString(",") { escape(it) }
                    writer.write(row)
                    writer.write("\n")
                }
            }
        }
        return file
    }

    private fun escape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
