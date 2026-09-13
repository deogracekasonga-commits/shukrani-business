package com.shukranibusiness.app.data

import android.content.Context
import androidx.room.withTransaction
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.StockMovement
import com.shukranibusiness.app.data.entities.StockMovementType
import com.shukranibusiness.app.util.PinHasher
import kotlinx.coroutines.flow.Flow

class ShopRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val employeeDao = db.employeeDao()
    private val productDao = db.productDao()
    private val saleDao = db.saleDao()
    private val stockMovementDao = db.stockMovementDao()

    // --- Employés ---

    fun observeActiveEmployees(): Flow<List<Employee>> = employeeDao.observeActive()

    suspend fun employeeCount(): Int = employeeDao.countActive()

    suspend fun authenticate(employeeId: Long, pin: String): Employee? {
        val employee = employeeDao.getById(employeeId) ?: return null
        return if (employee.active && PinHasher.matches(pin, employee.pinHash)) employee else null
    }

    suspend fun getEmployee(employeeId: Long): Employee? = employeeDao.getById(employeeId)

    suspend fun createEmployee(name: String, pin: String, role: EmployeeRole): Long {
        return employeeDao.insert(
            Employee(name = name, pinHash = PinHasher.hash(pin), role = role)
        )
    }

    suspend fun updateEmployee(employee: Employee, newPin: String?) {
        val toSave = if (newPin.isNullOrBlank()) employee else employee.copy(pinHash = PinHasher.hash(newPin))
        employeeDao.update(toSave)
    }

    suspend fun deactivateEmployee(employeeId: Long) = employeeDao.deactivate(employeeId)

    // --- Produits ---

    fun observeActiveProducts(): Flow<List<Product>> = productDao.observeActive()

    fun observeLowStockProducts(): Flow<List<Product>> = productDao.observeLowStock()

    fun observeCategories(): Flow<List<String>> = productDao.observeCategories()

    suspend fun saveProduct(product: Product): Long {
        return if (product.id == 0L) productDao.insert(product) else {
            productDao.update(product)
            product.id
        }
    }

    suspend fun deactivateProduct(productId: Long) = productDao.deactivate(productId)

    suspend fun restock(productId: Long, addQuantity: Int, note: String? = null) {
        db.withTransaction {
            val product = productDao.getById(productId) ?: return@withTransaction
            productDao.adjustQuantity(productId, addQuantity)
            stockMovementDao.insert(
                StockMovement(
                    productId = productId,
                    productName = product.name,
                    type = StockMovementType.REAPPRO,
                    quantityChange = addQuantity,
                    dateTimeMillis = System.currentTimeMillis(),
                    note = note
                )
            )
        }
    }

    // --- Ventes ---

    fun observeRecentSales(): Flow<List<Sale>> = saleDao.observeRecentSales()

    fun observeSalesBetween(startMillis: Long, endMillis: Long): Flow<List<Sale>> =
        saleDao.observeSalesBetween(startMillis, endMillis)

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = saleDao.getItemsForSale(saleId)

    suspend fun getSaleItemsBetween(startMillis: Long, endMillis: Long): List<SaleItem> =
        saleDao.getItemsBetween(startMillis, endMillis)

    /**
     * Enregistre une vente : vérifie le stock, décrémente les quantités, journalise le
     * mouvement de stock, le tout dans une seule transaction (tout ou rien).
     */
    suspend fun recordSale(
        employee: Employee,
        cartLines: List<CartLine>,
        currencyPaid: String,
        exchangeRateUsed: Double
    ): Sale {
        require(cartLines.isNotEmpty()) { "Le panier est vide" }

        return db.withTransaction {
            for (line in cartLines) {
                val current = productDao.getById(line.product.id)
                    ?: throw InsufficientStockException(line.product.name, 0)
                if (current.quantity < line.quantity) {
                    throw InsufficientStockException(line.product.name, current.quantity)
                }
            }

            val totalCdf = cartLines.sumOf { it.subtotalCdf }
            val totalUsd = cartLines.sumOf { it.subtotalUsd }
            val now = System.currentTimeMillis()

            val saleId = saleDao.insertSale(
                Sale(
                    employeeId = employee.id,
                    employeeName = employee.name,
                    dateTimeMillis = now,
                    totalCdf = totalCdf,
                    totalUsd = totalUsd,
                    currencyPaid = currencyPaid,
                    exchangeRateUsed = exchangeRateUsed
                )
            )

            saleDao.insertSaleItems(
                cartLines.map { line ->
                    SaleItem(
                        saleId = saleId,
                        productId = line.product.id,
                        productName = line.product.name,
                        unitPriceCdf = line.product.priceCdf,
                        unitPriceUsd = line.product.priceUsd,
                        quantity = line.quantity,
                        subtotalCdf = line.subtotalCdf,
                        subtotalUsd = line.subtotalUsd
                    )
                }
            )

            for (line in cartLines) {
                productDao.adjustQuantity(line.product.id, -line.quantity)
                stockMovementDao.insert(
                    StockMovement(
                        productId = line.product.id,
                        productName = line.product.name,
                        type = StockMovementType.VENTE,
                        quantityChange = -line.quantity,
                        dateTimeMillis = now
                    )
                )
            }

            Sale(
                id = saleId,
                employeeId = employee.id,
                employeeName = employee.name,
                dateTimeMillis = now,
                totalCdf = totalCdf,
                totalUsd = totalUsd,
                currencyPaid = currencyPaid,
                exchangeRateUsed = exchangeRateUsed
            )
        }
    }
}
