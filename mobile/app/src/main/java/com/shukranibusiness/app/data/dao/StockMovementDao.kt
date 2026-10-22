package com.shukranibusiness.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shukranibusiness.app.data.entities.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert
    suspend fun insert(movement: StockMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovement>)

    @Query("SELECT * FROM stock_movements")
    suspend fun getAll(): List<StockMovement>

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAll()

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY dateTimeMillis DESC")
    fun observeForProduct(productId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY dateTimeMillis DESC LIMIT 200")
    fun observeRecent(): Flow<List<StockMovement>>
}
