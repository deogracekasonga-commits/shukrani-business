package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val canceledAtMillis: Long? = null
)
