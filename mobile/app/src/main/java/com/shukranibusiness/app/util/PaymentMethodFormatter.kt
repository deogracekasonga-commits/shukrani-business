package com.shukranibusiness.app.util

import com.shukranibusiness.app.data.entities.PaymentMethod

object PaymentMethodFormatter {

    fun label(method: PaymentMethod, provider: String?): String = when (method) {
        PaymentMethod.CASH -> "Cash"
        PaymentMethod.CARD -> "Carte"
        PaymentMethod.MOBILE_MONEY -> if (!provider.isNullOrBlank()) "Mobile Money ($provider)" else "Mobile Money"
    }
}
