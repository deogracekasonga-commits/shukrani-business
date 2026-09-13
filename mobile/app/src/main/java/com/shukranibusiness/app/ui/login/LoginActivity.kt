package com.shukranibusiness.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.R
import com.shukranibusiness.app.databinding.ActivityLoginBinding
import com.shukranibusiness.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var repository: ShopRepository
    private lateinit var prefs: Prefs
    private var employees: List<Employee> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ShopRepository(this)
        prefs = Prefs(this)

        binding.createManagerButton.setOnClickListener { onCreateManagerClicked() }
        binding.loginButton.setOnClickListener { onLoginClicked() }

        checkFirstRun()
    }

    private fun checkFirstRun() {
        lifecycleScope.launch {
            if (repository.employeeCount() == 0) {
                binding.setupContainer.visibility = android.view.View.VISIBLE
                binding.loginContainer.visibility = android.view.View.GONE
            } else {
                binding.setupContainer.visibility = android.view.View.GONE
                binding.loginContainer.visibility = android.view.View.VISIBLE
                loadEmployees()
            }
        }
    }

    private fun loadEmployees() {
        lifecycleScope.launch {
            repository.observeActiveEmployees().collect { list ->
                employees = list
                val labels = list.map { employee ->
                    val roleLabel = if (employee.role == EmployeeRole.MANAGER) "Gérant" else "Vendeur"
                    "${employee.name} ($roleLabel)"
                }
                binding.employeeSpinner.adapter = ArrayAdapter(
                    this@LoginActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    labels
                )
            }
        }
    }

    private fun onCreateManagerClicked() {
        val name = binding.nameInput.text.toString().trim()
        val pin = binding.pinInput.text.toString()
        val pinConfirm = binding.pinConfirmInput.text.toString()

        if (name.isEmpty() || pin.isEmpty() || pinConfirm.isEmpty()) {
            showError(getString(R.string.error_fill_all_fields))
            return
        }
        if (pin.length < 4) {
            showError(getString(R.string.error_pin_too_short))
            return
        }
        if (pin != pinConfirm) {
            showError(getString(R.string.error_pin_mismatch))
            return
        }

        lifecycleScope.launch {
            val id = repository.createEmployee(name, pin, EmployeeRole.MANAGER)
            onLoginSuccess(id)
        }
    }

    private fun onLoginClicked() {
        val position = binding.employeeSpinner.selectedItemPosition
        if (position < 0 || position >= employees.size) {
            showError(getString(R.string.error_select_employee))
            return
        }
        val pin = binding.pinLoginInput.text.toString()
        val employee = employees[position]

        lifecycleScope.launch {
            val authenticated = repository.authenticate(employee.id, pin)
            if (authenticated != null) {
                onLoginSuccess(authenticated.id)
            } else {
                showError(getString(R.string.error_invalid_pin))
            }
        }
    }

    private fun onLoginSuccess(employeeId: Long) {
        prefs.loggedInEmployeeId = employeeId
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = android.view.View.VISIBLE
    }
}
