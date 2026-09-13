package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitPriceCdf: Double,
    val unitPriceUsd: Double,
    val quantity: Int,
    val subtotalCdf: Double,
    val subtotalUsd: Double
)
