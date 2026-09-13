package com.shukranibusiness.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val pinHash: String,
    val role: EmployeeRole,
    val active: Boolean = true
)
