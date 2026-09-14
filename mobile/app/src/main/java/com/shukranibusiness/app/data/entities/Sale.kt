package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SaleStatus {
    COMPLETED,
    CANCELED
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val dateTimeMillis: Long,
    val totalCdf: Double,
    val totalUsd: Double,
    val currencyPaid: String,
    val exchangeRateUsed: Double,
    val status: SaleStatus = SaleStatus.COMPLETED,
    val canceledByEmployeeName: String? = null,
    val canceledAtMillis: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    /** Nom de l'opérateur (ex. "M-Pesa", "Airtel Money", "Orange Money") — renseigné seulement si paymentMethod = MOBILE_MONEY. */
    val mobileMoneyProvider: String? = null,
    /** Identifiant stable utilisé pour la synchronisation cloud (indépendant de [id] local). */
    val cloudUuid: String = UUID.randomUUID().toString(),
    /** true tant que l'état actuel de cette vente n'a pas encore été poussé vers le cloud. */
    val pendingSync: Boolean = true
)
