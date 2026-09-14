package com.shukranibusiness.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shukranibusiness.app.data.dao.EmployeeDao
import com.shukranibusiness.app.data.dao.ProductDao
import com.shukranibusiness.app.data.dao.SaleDao
import com.shukranibusiness.app.data.dao.StockMovementDao
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleItem
import com.shukranibusiness.app.data.entities.StockMovement

@Database(
    entities = [Employee::class, Product::class, Sale::class, SaleItem::class, StockMovement::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shukrani_business.db"
                )
                    // Encore en développement (avant tout déploiement réel) : une évolution de
                    // schéma recrée simplement la base plutôt que d'écrire une migration.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
