package com.shukranibusiness.app.data

import android.content.Context
import androidx.room.withTransaction
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.data.entities.PaymentMethod
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.data.entities.StockMovement
import com.shukranibusiness.app.data.entities.StockMovementType
import com.shukranibusiness.app.sync.SyncScheduler
import com.shukranibusiness.app.util.CsvImporter
import com.shukranibusiness.app.util.PinHasher
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val context: Context) {

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
        if (!employee.active || !PinHasher.matches(pin, employee.pinHash)) return null
        if (PinHasher.isLegacyFormat(employee.pinHash)) {
            // Migration silencieuse vers le nouveau hachage (PBKDF2 + sel par employé) : aucune
            // action requise de l'employé, son PIN reste le même.
            val upgraded = employee.copy(pinHash = PinHasher.hash(pin))
            employeeDao.update(upgraded)
            return upgraded
        }
        return employee
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

    fun observeOutOfStockProducts(): Flow<List<Product>> = productDao.observeOutOfStock()

    fun observeActiveProductCount(): Flow<Int> = productDao.observeActiveCount()

    fun observeCategories(): Flow<List<String>> = productDao.observeCategories()

    suspend fun saveProduct(product: Product): Long {
        return if (product.id == 0L) productDao.insert(product) else {
            productDao.update(product)
            product.id
        }
    }

    suspend fun deactivateProduct(productId: Long) = productDao.deactivate(productId)

    /** Supprime tous les produits — utile pour nettoyer un import raté (ex. fichier non converti en CSV). */
    suspend fun deleteAllProducts(): Int = productDao.deleteAll()

    // --- Sauvegarde / restauration complète ---

    suspend fun getAllEmployees(): List<Employee> = employeeDao.getAll()

    suspend fun getAllProductsIncludingInactive(): List<Product> = productDao.getAll()

    suspend fun getAllSales(): List<Sale> = saleDao.getAllSales()

    suspend fun getAllSaleItemsForBackup(): List<SaleItem> = saleDao.getAllSaleItems()

    suspend fun getAllStockMovements(): List<StockMovement> = stockMovementDao.getAll()

    /**
     * Remplace toutes les données de l'app par celles fournies (restauration d'une sauvegarde).
     * Les IDs sont préservés pour garder les liens entre ventes/produits/employés cohérents.
     */
    suspend fun restoreAll(
        employees: List<Employee>,
        products: List<Product>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        stockMovements: List<StockMovement>
    ) {
        db.withTransaction {
            saleDao.deleteAllSaleItems()
            saleDao.deleteAllSales()
            stockMovementDao.deleteAll()
            productDao.deleteAll()
            employeeDao.deleteAll()

            employeeDao.insertAll(employees)
            productDao.insertAll(products)
            saleDao.insertAllSales(sales)
            saleDao.insertAllSaleItems(saleItems)
            stockMovementDao.insertAll(stockMovements)
        }
    }

    /**
     * Importe une liste de produits (typiquement depuis un fichier CSV) : un produit dont le nom
     * correspond déjà à un produit actif est mis à jour, sinon un nouveau produit est créé.
     * @return le nombre de produits importés (créés ou mis à jour).
     */
    suspend fun importProducts(products: List<CsvImporter.ParsedProduct>): Int {
        return db.withTransaction {
            for (parsed in products) {
                val existing = productDao.findByName(parsed.name)
                val product = Product(
                    id = existing?.id ?: 0L,
                    name = parsed.name,
                    category = parsed.category,
                    priceCdf = parsed.priceCdf,
                    priceUsd = parsed.priceUsd,
                    quantity = parsed.quantity,
                    lowStockThreshold = parsed.lowStockThreshold,
                    purchasePriceCdf = parsed.purchasePriceCdf,
                    purchasePriceUsd = parsed.purchasePriceUsd
                )
                if (existing != null) productDao.update(product) else productDao.insert(product)
            }
            products.size
        }
    }

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

    suspend fun getSalesBetween(startMillis: Long, endMillis: Long): List<Sale> =
        saleDao.getSalesBetween(startMillis, endMillis)

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = saleDao.getItemsForSale(saleId)

    suspend fun getSaleItemsBetween(startMillis: Long, endMillis: Long): List<SaleItem> =
        saleDao.getItemsBetween(startMillis, endMillis)

    suspend fun getTopProducts(startMillis: Long, endMillis: Long, limit: Int = 5) =
        saleDao.getTopProducts(startMillis, endMillis, limit)

    suspend fun getEmployeeSalesStats(startMillis: Long, endMillis: Long) =
        saleDao.getEmployeeSalesStats(startMillis, endMillis)

    suspend fun getSale(saleId: Long): Sale? = saleDao.getById(saleId)

    suspend fun getPendingSyncSales(): List<Sale> = saleDao.getPendingSync()

    suspend fun markSynced(saleId: Long) = saleDao.markSynced(saleId)

    /**
     * Annule une vente (note de crédit) : remet les quantités vendues en stock, journalise
     * chaque remise en stock, et marque la vente "CANCELED" sans la supprimer (traçabilité).
     * Réservé au gérant côté UI. Ne fait rien si la vente n'existe plus ou est déjà annulée.
     */
    suspend fun cancelSale(saleId: Long, canceledBy: Employee): Sale? {
        return db.withTransaction {
            val sale = saleDao.getById(saleId) ?: return@withTransaction null
            if (sale.status == SaleStatus.CANCELED) return@withTransaction sale

            val items = saleDao.getItemsForSale(saleId)
            val now = System.currentTimeMillis()
            for (item in items) {
                productDao.adjustQuantity(item.productId, item.quantity)
                stockMovementDao.insert(
                    StockMovement(
                        productId = item.productId,
                        productName = item.productName,
                        type = StockMovementType.ANNULATION,
                        quantityChange = item.quantity,
                        dateTimeMillis = now,
                        note = "Annulation vente #$saleId (note de crédit) par ${canceledBy.name}"
                    )
                )
            }

            saleDao.updateStatus(saleId, SaleStatus.CANCELED, canceledBy.name, now)
            sale.copy(status = SaleStatus.CANCELED, canceledByEmployeeName = canceledBy.name, canceledAtMillis = now)
        }.also { SyncScheduler.scheduleImmediateSync(context) }
    }

    /**
     * Enregistre une vente : vérifie le stock, décrémente les quantités, journalise le
     * mouvement de stock, le tout dans une seule transaction (tout ou rien).
     */
    suspend fun recordSale(
        employee: Employee,
        cartLines: List<CartLine>,
        currencyPaid: String,
        exchangeRateUsed: Double,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        mobileMoneyProvider: String? = null
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
            val cloudUuid = java.util.UUID.randomUUID().toString()

            val saleId = saleDao.insertSale(
                Sale(
                    employeeId = employee.id,
                    employeeName = employee.name,
                    dateTimeMillis = now,
                    totalCdf = totalCdf,
                    totalUsd = totalUsd,
                    currencyPaid = currencyPaid,
                    exchangeRateUsed = exchangeRateUsed,
                    paymentMethod = paymentMethod,
                    mobileMoneyProvider = mobileMoneyProvider,
                    cloudUuid = cloudUuid
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
                        subtotalUsd = line.subtotalUsd,
                        unitCostCdf = line.product.purchasePriceCdf,
                        unitCostUsd = line.product.purchasePriceUsd
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
                exchangeRateUsed = exchangeRateUsed,
                paymentMethod = paymentMethod,
                mobileMoneyProvider = mobileMoneyProvider,
                cloudUuid = cloudUuid
            )
        }.also { SyncScheduler.scheduleImmediateSync(context) }
    }
}
