package com.shukranibusiness.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shukranibusiness.app.data.entities.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(employee: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<Employee>)

    @Update
    suspend fun update(employee: Employee)

    @Query("UPDATE employees SET active = 0 WHERE id = :employeeId")
    suspend fun deactivate(employeeId: Long)

    @Query("SELECT * FROM employees WHERE active = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<Employee>>

    @Query("SELECT * FROM employees")
    suspend fun getAll(): List<Employee>

    @Query("DELETE FROM employees")
    suspend fun deleteAll()

    @Query("SELECT * FROM employees WHERE id = :employeeId LIMIT 1")
    suspend fun getById(employeeId: Long): Employee?

    @Query("SELECT COUNT(*) FROM employees WHERE active = 1")
    suspend fun countActive(): Int
}
