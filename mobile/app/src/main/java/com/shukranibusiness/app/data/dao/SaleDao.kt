package com.shukranibusiness.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.SaleStatus
import kotlinx.coroutines.flow.Flow

data class TopProductStat(
    val productName: String,
    val totalQuantity: Int,
    val totalRevenueCdf: Double,
    val totalRevenueUsd: Double,
    val totalCostCdf: Double,
    val totalCostUsd: Double
)

@Dao
interface SaleDao {

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getById(saleId: Long): Sale?

    @Query(
        "UPDATE sales SET status = :status, canceledByEmployeeName = :canceledByEmployeeName, " +
            "canceledAtMillis = :canceledAtMillis, pendingSync = 1 WHERE id = :saleId"
    )
    suspend fun updateStatus(
        saleId: Long,
        status: SaleStatus,
        canceledByEmployeeName: String?,
        canceledAtMillis: Long?
    )

    @Query("SELECT * FROM sales WHERE pendingSync = 1 ORDER BY dateTimeMillis ASC")
    suspend fun getPendingSync(): List<Sale>

    @Query("UPDATE sales SET pendingSync = 0 WHERE id = :saleId")
    suspend fun markSynced(saleId: Long)

    @Query("SELECT * FROM sales WHERE dateTimeMillis BETWEEN :startMillis AND :endMillis ORDER BY dateTimeMillis DESC")
    fun observeSalesBetween(startMillis: Long, endMillis: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE dateTimeMillis BETWEEN :startMillis AND :endMillis ORDER BY dateTimeMillis DESC")
    suspend fun getSalesBetween(startMillis: Long, endMillis: Long): List<Sale>

    @Query("SELECT * FROM sales ORDER BY dateTimeMillis DESC LIMIT 100")
    fun observeRecentSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE saleId IN (SELECT id FROM sales WHERE dateTimeMillis BETWEEN :startMillis AND :endMillis)")
    suspend fun getItemsBetween(startMillis: Long, endMillis: Long): List<SaleItem>

    @Query(
        "SELECT si.productName as productName, SUM(si.quantity) as totalQuantity, " +
            "SUM(si.subtotalCdf) as totalRevenueCdf, SUM(si.subtotalUsd) as totalRevenueUsd, " +
            "SUM(si.unitCostCdf * si.quantity) as totalCostCdf, SUM(si.unitCostUsd * si.quantity) as totalCostUsd " +
            "FROM sale_items si INNER JOIN sales s ON s.id = si.saleId " +
            "WHERE s.status = 'COMPLETED' AND s.dateTimeMillis BETWEEN :startMillis AND :endMillis " +
            "GROUP BY si.productName ORDER BY totalQuantity DESC LIMIT :limit"
    )
    suspend fun getTopProducts(startMillis: Long, endMillis: Long, limit: Int): List<TopProductStat>
}
