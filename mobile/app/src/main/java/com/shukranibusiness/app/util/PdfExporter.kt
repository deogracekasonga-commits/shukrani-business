package com.shukranibusiness.app.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.shukranibusiness.app.data.entities.Sale
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 40f
    private const val LINE_HEIGHT = 18f

    fun exportSalesSummary(
        context: Context,
        periodLabel: String,
        sales: List<Sale>,
        totalCdf: Double,
        totalUsd: Double,
        shopName: String
    ): File {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val boldPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 12f }
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 60f

        canvas.drawText(shopName, MARGIN_LEFT, y, titlePaint)
        y += LINE_HEIGHT * 1.5f
        canvas.drawText("Rapport des ventes — $periodLabel", MARGIN_LEFT, y, textPaint)
        y += LINE_HEIGHT
        canvas.drawText(
            "Total : ${String.format(Locale.FRANCE, "%.0f", totalCdf)} CDF " +
                "(${String.format(Locale.FRANCE, "%.2f", totalUsd)} USD)",
            MARGIN_LEFT,
            y,
            boldPaint
        )
        y += LINE_HEIGHT
        canvas.drawText("Nombre de ventes : ${sales.size}", MARGIN_LEFT, y, textPaint)
        y += LINE_HEIGHT * 1.5f

        canvas.drawText("Date", MARGIN_LEFT, y, boldPaint)
        canvas.drawText("Employé", MARGIN_LEFT + 140, y, boldPaint)
        canvas.drawText("Total CDF", MARGIN_LEFT + 300, y, boldPaint)
        canvas.drawText("Total USD", MARGIN_LEFT + 420, y, boldPaint)
        y += LINE_HEIGHT

        for (sale in sales) {
            if (y > PAGE_HEIGHT - 60) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
            }
            canvas.drawText(sdf.format(Date(sale.dateTimeMillis)), MARGIN_LEFT, y, textPaint)
            canvas.drawText(sale.employeeName, MARGIN_LEFT + 140, y, textPaint)
            canvas.drawText(String.format(Locale.FRANCE, "%.0f", sale.totalCdf), MARGIN_LEFT + 300, y, textPaint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f", sale.totalUsd), MARGIN_LEFT + 420, y, textPaint)
            y += LINE_HEIGHT
        }
        document.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "rapport_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
        return file
    }
}
