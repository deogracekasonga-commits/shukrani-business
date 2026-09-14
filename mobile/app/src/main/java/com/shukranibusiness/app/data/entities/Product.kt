package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val priceCdf: Double,
    val priceUsd: Double,
    val quantity: Int,
    val lowStockThreshold: Int = 5,
    val active: Boolean = true,
    /** Prix d'achat (coût) — optionnel (0 = non renseigné), sert au calcul de marge. */
    val purchasePriceCdf: Double = 0.0,
    val purchasePriceUsd: Double = 0.0
)
