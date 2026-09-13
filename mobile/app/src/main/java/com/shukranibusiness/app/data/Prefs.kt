package com.shukranibusiness.app.data

import android.content.Context

class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shukrani_business_prefs", Context.MODE_PRIVATE)

    var shopName: String
        get() = prefs.getString(KEY_SHOP_NAME, "Shukrani Business") ?: "Shukrani Business"
        set(value) = prefs.edit().putString(KEY_SHOP_NAME, value).apply()

    /** Nombre de francs congolais (CDF) pour 1 dollar américain (USD). */
    var exchangeRateCdfPerUsd: Double
        get() = prefs.getFloat(KEY_EXCHANGE_RATE, DEFAULT_EXCHANGE_RATE).toDouble()
        set(value) = prefs.edit().putFloat(KEY_EXCHANGE_RATE, value.toFloat()).apply()

    var loggedInEmployeeId: Long
        get() = prefs.getLong(KEY_SESSION_EMPLOYEE_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_SESSION_EMPLOYEE_ID, value).apply()

    var printerAddress: String?
        get() = prefs.getString(KEY_PRINTER_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_PRINTER_ADDRESS, value).apply()

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION_EMPLOYEE_ID).apply()
    }

    companion object {
        private const val KEY_SHOP_NAME = "shop_name"
        private const val KEY_EXCHANGE_RATE = "exchange_rate_cdf_per_usd"
        private const val KEY_SESSION_EMPLOYEE_ID = "session_employee_id"
        private const val KEY_PRINTER_ADDRESS = "printer_address"

        // Taux indicatif par défaut, à ajuster dans Réglages selon le taux du jour.
        private const val DEFAULT_EXCHANGE_RATE = 2800f
    }
}
