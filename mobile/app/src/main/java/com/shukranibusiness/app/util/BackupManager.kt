package com.shukranibusiness.app.util

import android.content.Context
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.data.entities.PaymentMethod
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.data.entities.StockMovement
import com.shukranibusiness.app.data.entities.StockMovementType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Sauvegarde/restauration complète des données locales de l'app (employés, produits, ventes,
 * mouvements de stock, réglages boutique) sous forme d'un unique fichier JSON que l'utilisateur
 * peut garder où il veut (Drive, email, WhatsApp...) et réimporter en cas de perte du téléphone.
 */
object BackupManager {

    class InvalidBackupException(message: String) : Exception(message)

    data class RestoreSummary(val employees: Int, val products: Int, val sales: Int)

    private const val FORMAT_VERSION = 1

    suspend fun export(context: Context, repository: ShopRepository, prefs: Prefs): File {
        val root = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())
            put(
                "settings",
                JSONObject().apply {
                    put("shopName", prefs.shopName)
                    put("shopAddress", prefs.shopAddress)
                    put("taxNumber", prefs.taxNumber)
                    put("rccmNumber", prefs.rccmNumber)
                    put("exchangeRateCdfPerUsd", prefs.exchangeRateCdfPerUsd)
                    put("printerAddress", prefs.printerAddress)
                }
            )
            put("employees", JSONArray(repository.getAllEmployees().map { it.toBackupJson() }))
            put("products", JSONArray(repository.getAllProductsIncludingInactive().map { it.toBackupJson() }))
            put("sales", JSONArray(repository.getAllSales().map { it.toBackupJson() }))
            put("saleItems", JSONArray(repository.getAllSaleItemsForBackup().map { it.toBackupJson() }))
            put("stockMovements", JSONArray(repository.getAllStockMovements().map { it.toBackupJson() }))
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "shukra_pos_sauvegarde_${System.currentTimeMillis()}.json")
        file.writeText(root.toString())
        return file
    }

    suspend fun restore(text: String, repository: ShopRepository, prefs: Prefs): RestoreSummary {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw InvalidBackupException("Ce fichier n'est pas une sauvegarde Shukra POS valide.")
        }
        if (!root.has("employees") || !root.has("products") || !root.has("sales")) {
            throw InvalidBackupException("Ce fichier n'est pas une sauvegarde Shukra POS valide.")
        }

        val employees = root.getJSONArray("employees").toList { it.toEmployee() }
        val products = root.getJSONArray("products").toList { it.toProduct() }
        val sales = root.getJSONArray("sales").toList { it.toSale() }
        val saleItems = root.optJSONArray("saleItems")?.toList { it.toSaleItem() } ?: emptyList()
        val stockMovements = root.optJSONArray("stockMovements")?.toList { it.toStockMovement() } ?: emptyList()

        repository.restoreAll(employees, products, sales, saleItems, stockMovements)

        root.optJSONObject("settings")?.let { settings ->
            if (!settings.isNull("shopName")) prefs.shopName = settings.optString("shopName", prefs.shopName)
            if (!settings.isNull("shopAddress")) prefs.shopAddress = settings.optString("shopAddress", prefs.shopAddress)
            if (!settings.isNull("taxNumber")) prefs.taxNumber = settings.optString("taxNumber", prefs.taxNumber)
            if (!settings.isNull("rccmNumber")) prefs.rccmNumber = settings.optString("rccmNumber", prefs.rccmNumber)
            if (!settings.isNull("exchangeRateCdfPerUsd")) {
                prefs.exchangeRateCdfPerUsd = settings.optDouble("exchangeRateCdfPerUsd", prefs.exchangeRateCdfPerUsd)
            }
            if (!settings.isNull("printerAddress")) {
                prefs.printerAddress = settings.optString("printerAddress")
            }
        }

        return RestoreSummary(employees.size, products.size, sales.size)
    }

    // --- Employee ---

    private fun Employee.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("pinHash", pinHash)
        put("role", role.name)
        put("active", active)
    }

    private fun JSONObject.toEmployee() = Employee(
        id = getLong("id"),
        name = getString("name"),
        pinHash = getString("pinHash"),
        role = EmployeeRole.valueOf(getString("role")),
        active = optBoolean("active", true)
    )

    // --- Product ---

    private fun Product.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category)
        put("priceCdf", priceCdf)
        put("priceUsd", priceUsd)
        put("quantity", quantity)
        put("lowStockThreshold", lowStockThreshold)
        put("active", active)
        put("purchasePriceCdf", purchasePriceCdf)
        put("purchasePriceUsd", purchasePriceUsd)
    }

    private fun JSONObject.toProduct() = Product(
        id = getLong("id"),
        name = getString("name"),
        category = getString("category"),
        priceCdf = getDouble("priceCdf"),
        priceUsd = getDouble("priceUsd"),
        quantity = getInt("quantity"),
        lowStockThreshold = optInt("lowStockThreshold", 5),
        active = optBoolean("active", true),
        purchasePriceCdf = optDouble("purchasePriceCdf", 0.0),
        purchasePriceUsd = optDouble("purchasePriceUsd", 0.0)
    )

    // --- Sale ---

    private fun Sale.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("employeeId", employeeId)
        put("employeeName", employeeName)
        put("dateTimeMillis", dateTimeMillis)
        put("totalCdf", totalCdf)
        put("totalUsd", totalUsd)
        put("subtotalCdf", subtotalCdf)
        put("subtotalUsd", subtotalUsd)
        put("discountCdf", discountCdf)
        put("discountUsd", discountUsd)
        put("currencyPaid", currencyPaid)
        put("exchangeRateUsed", exchangeRateUsed)
        put("status", status.name)
        put("canceledByEmployeeName", canceledByEmployeeName)
        put("canceledAtMillis", canceledAtMillis)
        put("paymentMethod", paymentMethod.name)
        put("mobileMoneyProvider", mobileMoneyProvider)
        put("cloudUuid", cloudUuid)
        put("pendingSync", pendingSync)
    }

    private fun JSONObject.toSale() = Sale(
        id = getLong("id"),
        employeeId = getLong("employeeId"),
        employeeName = getString("employeeName"),
        dateTimeMillis = getLong("dateTimeMillis"),
        totalCdf = getDouble("totalCdf"),
        totalUsd = getDouble("totalUsd"),
        subtotalCdf = optDouble("subtotalCdf", 0.0),
        subtotalUsd = optDouble("subtotalUsd", 0.0),
        discountCdf = optDouble("discountCdf", 0.0),
        discountUsd = optDouble("discountUsd", 0.0),
        currencyPaid = getString("currencyPaid"),
        exchangeRateUsed = getDouble("exchangeRateUsed"),
        status = SaleStatus.valueOf(optString("status", "COMPLETED")),
        canceledByEmployeeName = if (isNull("canceledByEmployeeName")) null else optString("canceledByEmployeeName"),
        canceledAtMillis = if (isNull("canceledAtMillis")) null else optLong("canceledAtMillis"),
        paymentMethod = PaymentMethod.valueOf(optString("paymentMethod", "CASH")),
        mobileMoneyProvider = if (isNull("mobileMoneyProvider")) null else optString("mobileMoneyProvider"),
        cloudUuid = optString("cloudUuid", UUID.randomUUID().toString()),
        pendingSync = optBoolean("pendingSync", true)
    )

    // --- SaleItem ---

    private fun SaleItem.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("saleId", saleId)
        put("productId", productId)
        put("productName", productName)
        put("unitPriceCdf", unitPriceCdf)
        put("unitPriceUsd", unitPriceUsd)
        put("quantity", quantity)
        put("subtotalCdf", subtotalCdf)
        put("subtotalUsd", subtotalUsd)
        put("unitCostCdf", unitCostCdf)
        put("unitCostUsd", unitCostUsd)
        put("cloudUuid", cloudUuid)
    }

    private fun JSONObject.toSaleItem() = SaleItem(
        id = getLong("id"),
        saleId = getLong("saleId"),
        productId = getLong("productId"),
        productName = getString("productName"),
        unitPriceCdf = getDouble("unitPriceCdf"),
        unitPriceUsd = getDouble("unitPriceUsd"),
        quantity = getInt("quantity"),
        subtotalCdf = getDouble("subtotalCdf"),
        subtotalUsd = getDouble("subtotalUsd"),
        unitCostCdf = optDouble("unitCostCdf", 0.0),
        unitCostUsd = optDouble("unitCostUsd", 0.0),
        cloudUuid = optString("cloudUuid", UUID.randomUUID().toString())
    )

    // --- StockMovement ---

    private fun StockMovement.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("productId", productId)
        put("productName", productName)
        put("type", type.name)
        put("quantityChange", quantityChange)
        put("dateTimeMillis", dateTimeMillis)
        put("note", note)
    }

    private fun JSONObject.toStockMovement() = StockMovement(
        id = getLong("id"),
        productId = getLong("productId"),
        productName = getString("productName"),
        type = StockMovementType.valueOf(getString("type")),
        quantityChange = getInt("quantityChange"),
        dateTimeMillis = getLong("dateTimeMillis"),
        note = if (isNull("note")) null else optString("note")
    )

    private inline fun <T> JSONArray.toList(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
}
