package com.shukranibusiness.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        maximumFractionDigits = 0
    }
    private val usdFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun formatCdf(amount: Double): String = "${numberFormat.format(amount)} CDF"

    fun formatUsd(amount: Double): String = "${usdFormat.format(amount)} USD"

    fun formatBoth(amountCdf: Double, amountUsd: Double): String =
        "${formatCdf(amountCdf)} (${formatUsd(amountUsd)})"
}
