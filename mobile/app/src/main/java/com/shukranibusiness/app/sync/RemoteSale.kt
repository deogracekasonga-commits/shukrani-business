package com.shukranibusiness.app.sync

data class RemoteSale(
    val shopName: String,
    val employeeName: String,
    val dateTimeMillis: Long,
    val totalCdf: Double,
    val totalUsd: Double,
    val status: String
)
