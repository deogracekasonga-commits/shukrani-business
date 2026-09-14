package com.shukranibusiness.app.data

import android.content.Context

class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shukrani_business_prefs", Context.MODE_PRIVATE)

    var shopName: String
        get() = prefs.getString(KEY_SHOP_NAME, "Shukrani Business") ?: "Shukrani Business"
        set(value) = prefs.edit().putString(KEY_SHOP_NAME, value).apply()

    /** Adresse de ce point de vente précis — propre à cet appareil (pas partagée entre boutiques). */
    var shopAddress: String
        get() = prefs.getString(KEY_SHOP_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SHOP_ADDRESS, value).apply()

    /** Numéro d'identification fiscale (NIF) — affiché sur le reçu si renseigné. */
    var taxNumber: String
        get() = prefs.getString(KEY_TAX_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TAX_NUMBER, value).apply()

    /** Numéro RCCM (Registre du Commerce et du Crédit Mobilier) — affiché sur le reçu si renseigné. */
    var rccmNumber: String
        get() = prefs.getString(KEY_RCCM_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RCCM_NUMBER, value).apply()

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

    /** Jeton secret reçu à l'enregistrement de cet appareil sur Supabase — null si non configuré. */
    var deviceToken: String?
        get() = prefs.getString(KEY_DEVICE_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value).apply()

    /** Identifiant (UUID) attribué à cet appareil à l'enregistrement — requis pour taguer les ventes. */
    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    /** "shop" (vendeur) ou "manager" (gérant à distance) — voir la table `devices` côté Supabase. */
    var deviceRole: String?
        get() = prefs.getString(KEY_DEVICE_ROLE, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ROLE, value).apply()

    val isRemoteSyncConfigured: Boolean
        get() = !deviceToken.isNullOrBlank()

    val isManagerDevice: Boolean
        get() = deviceRole == "manager"

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION_EMPLOYEE_ID).apply()
    }

    fun clearDeviceRegistration() {
        prefs.edit().remove(KEY_DEVICE_TOKEN).remove(KEY_DEVICE_ID).remove(KEY_DEVICE_ROLE).apply()
    }

    companion object {
        private const val KEY_SHOP_NAME = "shop_name"
        private const val KEY_SHOP_ADDRESS = "shop_address"
        private const val KEY_TAX_NUMBER = "tax_number"
        private const val KEY_RCCM_NUMBER = "rccm_number"
        private const val KEY_EXCHANGE_RATE = "exchange_rate_cdf_per_usd"
        private const val KEY_SESSION_EMPLOYEE_ID = "session_employee_id"
        private const val KEY_PRINTER_ADDRESS = "printer_address"
        private const val KEY_DEVICE_TOKEN = "sync_device_token"
        private const val KEY_DEVICE_ID = "sync_device_id"
        private const val KEY_DEVICE_ROLE = "sync_device_role"

        // Taux indicatif par défaut, à ajuster dans Réglages selon le taux du jour.
        private const val DEFAULT_EXCHANGE_RATE = 2800f
    }
}
