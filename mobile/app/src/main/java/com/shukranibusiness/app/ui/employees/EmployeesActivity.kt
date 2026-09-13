package com.shukranibusiness.app.ui.employees

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.databinding.ActivityEmployeesBinding
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch

class EmployeesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeesBinding
    private lateinit var repository: ShopRepository
    private lateinit var adapter: EmployeeAdapter
    private var employees: List<Employee> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_employees)

        repository = ShopRepository(this)

        adapter = EmployeeAdapter(
            onEdit = { employee -> showEditDialog(employee) },
            onDelete = { employee -> confirmDelete(employee) }
        )
        binding.employeeList.layoutManager = LinearLayoutManager(this)
        binding.employeeList.adapter = adapter

        binding.addEmployeeButton.setOnClickListener { showEditDialog(null) }

        lifecycleScope.launch {
            repository.observeActiveEmployees().collect { list ->
                employees = list
                adapter.submit(list)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showEditDialog(employee: Employee?) {
        EmployeeEditDialog(employee) { name, pin, role ->
            lifecycleScope.launch {
                if (employee == null) {
                    repository.createEmployee(name, pin!!, role)
                } else {
                    repository.updateEmployee(employee.copy(name = name, role = role), pin)
                }
            }
        }.show(supportFragmentManager, "employee_edit")
    }

    private fun confirmDelete(employee: Employee) {
        val activeManagers = employees.count { it.role == EmployeeRole.MANAGER }
        if (employee.role == EmployeeRole.MANAGER && activeManagers <= 1) {
            Toast.makeText(this, getString(R.string.error_last_manager), Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch { repository.deactivateEmployee(employee.id) }
    }
}
