package com.shukranibusiness.app.data

class InsufficientStockException(val productName: String, val available: Int) :
    Exception("Stock insuffisant pour $productName (disponible : $available)")
