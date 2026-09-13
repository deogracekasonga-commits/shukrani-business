package com.shukranibusiness.app.data

import com.shukranibusiness.app.data.entities.Product

data class CartLine(
    val product: Product,
    val quantity: Int
) {
    val subtotalCdf: Double get() = product.priceCdf * quantity
    val subtotalUsd: Double get() = product.priceUsd * quantity
}
