package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

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
    val subtotalUsd: Double,
    /** Prix d'achat unitaire au moment de la vente (0 = non renseigné pour ce produit) — fige la marge dans le temps. */
    val unitCostCdf: Double = 0.0,
    val unitCostUsd: Double = 0.0,
    /** Identifiant stable utilisé pour la synchronisation cloud (upsert idempotent). */
    val cloudUuid: String = UUID.randomUUID().toString()
)
