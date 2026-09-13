package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StockMovementType {
    VENTE,
    REAPPRO,
    AJUSTEMENT,
    ANNULATION
}

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: StockMovementType,
    val quantityChange: Int,
    val dateTimeMillis: Long,
    val note: String? = null
)
