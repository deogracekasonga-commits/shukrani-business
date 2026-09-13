package com.shukranibusiness.app.data

import androidx.room.TypeConverter
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.data.entities.StockMovementType

class Converters {

    @TypeConverter
    fun fromEmployeeRole(role: EmployeeRole): String = role.name

    @TypeConverter
    fun toEmployeeRole(value: String): EmployeeRole = EmployeeRole.valueOf(value)

    @TypeConverter
    fun fromStockMovementType(type: StockMovementType): String = type.name

    @TypeConverter
    fun toStockMovementType(value: String): StockMovementType = StockMovementType.valueOf(value)

    @TypeConverter
    fun fromSaleStatus(status: SaleStatus): String = status.name

    @TypeConverter
    fun toSaleStatus(value: String): SaleStatus = SaleStatus.valueOf(value)
}
