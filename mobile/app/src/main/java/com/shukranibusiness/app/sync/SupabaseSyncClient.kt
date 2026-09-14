package com.shukranibusiness.app.sync

import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncException(message: String) : Exception(message)

/**
 * Appels REST directs vers Supabase (PostgREST), sans le SDK Supabase complet — juste ce dont
 * l'app a besoin : enregistrer un appareil, pousser ses ventes, lire les ventes distantes.
 * La sécurité repose sur les politiques RLS côté base (voir migration
 * `shukra_pos_remote_sync_schema`), pas sur le secret de la clé publique utilisée ici.
 */
object SupabaseSyncClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    data class RegistrationResult(val deviceId: String, val deviceToken: String)

    suspend fun registerDevice(setupKey: String, shopName: String, role: String): RegistrationResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("p_setup_key", setupKey)
                put("p_shop_name", shopName)
                put("p_role", role)
            }
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}/rest/v1/rpc/register_device")
                .addHeader("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw SyncException("Code de configuration invalide ou erreur serveur (${response.code})")
                }
                val array = JSONArray(text)
                if (array.length() == 0) throw SyncException("Réponse vide du serveur")
                val row = array.getJSONObject(0)
                RegistrationResult(row.getString("device_id"), row.getString("device_token"))
            }
        }

    /** @return true si la vente (et ses articles) a bien été envoyée. */
    suspend fun pushSale(
        deviceToken: String,
        deviceId: String,
        shopName: String,
        sale: Sale,
        items: List<SaleItem>
    ): Boolean = withContext(Dispatchers.IO) {
        val saleBody = JSONObject().apply {
            put("id", sale.cloudUuid)
            put("device_id", deviceId)
            put("shop_name", shopName)
            put("employee_name", sale.employeeName)
            put("date_time_millis", sale.dateTimeMillis)
            put("total_cdf", sale.totalCdf)
            put("total_usd", sale.totalUsd)
            put("currency_paid", sale.currencyPaid)
            put("exchange_rate_used", sale.exchangeRateUsed)
            put("status", sale.status.name)
            put("payment_method", sale.paymentMethod.name)
            put("mobile_money_provider", sale.mobileMoneyProvider)
            put("canceled_by_employee_name", sale.canceledByEmployeeName)
            put("canceled_at_millis", sale.canceledAtMillis)
        }
        val salesArray = JSONArray().put(saleBody)

        val salesRequest = Request.Builder()
            .url("${SupabaseConfig.BASE_URL}/rest/v1/sales")
            .addHeader("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            .addHeader("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
            .addHeader("x-device-token", deviceToken)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(salesArray.toString().toRequestBody(JSON))
            .build()

        client.newCall(salesRequest).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
        }

        if (items.isNotEmpty()) {
            val itemsArray = JSONArray()
            for (item in items) {
                itemsArray.put(
                    JSONObject().apply {
                        put("id", item.cloudUuid)
                        put("sale_id", sale.cloudUuid)
                        put("product_name", item.productName)
                        put("unit_price_cdf", item.unitPriceCdf)
                        put("unit_price_usd", item.unitPriceUsd)
                        put("quantity", item.quantity)
                        put("subtotal_cdf", item.subtotalCdf)
                        put("subtotal_usd", item.subtotalUsd)
                    }
                )
            }
            val itemsRequest = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}/rest/v1/sale_items")
                .addHeader("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                .addHeader("x-device-token", deviceToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(itemsArray.toString().toRequestBody(JSON))
                .build()
            client.newCall(itemsRequest).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
            }
        }
        true
    }

    suspend fun fetchRemoteSales(deviceToken: String, limit: Int = 200): List<RemoteSale> =
        withContext(Dispatchers.IO) {
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/sales" +
                "?select=shop_name,employee_name,date_time_millis,total_cdf,total_usd,status" +
                "&order=date_time_millis.desc&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                .addHeader("x-device-token", deviceToken)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val text = response.body?.string().orEmpty()
                val array = JSONArray(text)
                (0 until array.length()).map { i ->
                    val row = array.getJSONObject(i)
                    RemoteSale(
                        shopName = row.getString("shop_name"),
                        employeeName = row.getString("employee_name"),
                        dateTimeMillis = row.getLong("date_time_millis"),
                        totalCdf = row.getDouble("total_cdf"),
                        totalUsd = row.getDouble("total_usd"),
                        status = row.getString("status")
                    )
                }
            }
        }
}
