package com.shukranibusiness.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sales WHERE dateTimeMillis BETWEEN :startMillis AND :endMillis ORDER BY dateTimeMillis DESC")
    fun observeSalesBetween(startMillis: Long, endMillis: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY dateTimeMillis DESC LIMIT 100")
    fun observeRecentSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE saleId IN (SELECT id FROM sales WHERE dateTimeMillis BETWEEN :startMillis AND :endMillis)")
    suspend fun getItemsBetween(startMillis: Long, endMillis: Long): List<SaleItem>
}
