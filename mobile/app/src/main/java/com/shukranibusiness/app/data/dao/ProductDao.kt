package com.shukranibusiness.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shukranibusiness.app.data.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET active = 0 WHERE id = :productId")
    suspend fun deactivate(productId: Long)

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY category ASC, name ASC")
    fun observeActive(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE active = 1 AND quantity <= lowStockThreshold ORDER BY quantity ASC")
    fun observeLowStock(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getById(productId: Long): Product?

    @Query("UPDATE products SET quantity = quantity + :delta WHERE id = :productId")
    suspend fun adjustQuantity(productId: Long, delta: Int)

    @Query("SELECT DISTINCT category FROM products WHERE active = 1 ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>
}
