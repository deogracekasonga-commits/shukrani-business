package com.shukranibusiness.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.databinding.ActivityMainBinding
import com.shukranibusiness.app.sync.SyncScheduler
import com.shukranibusiness.app.ui.dashboard.DashboardFragment
import com.shukranibusiness.app.ui.login.LoginActivity
import com.shukranibusiness.app.ui.pos.PosFragment
import com.shukranibusiness.app.ui.reports.ReportsFragment
import com.shukranibusiness.app.ui.settings.SettingsFragment
import com.shukranibusiness.app.ui.stock.StockFragment
import com.shukranibusiness.app.util.SloganRotator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ShopRepository
    private lateinit var prefs: Prefs
    private var currentEmployee: Employee? = null
    private var sloganRotator: SloganRotator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repository = ShopRepository(this)
        prefs = Prefs(this)
        SyncScheduler.schedulePeriodicSafetyNet(this)

        sloganRotator = SloganRotator(
            binding.sloganBanner,
            resources.getStringArray(R.array.rotating_slogans).toList()
        ).also { it.start() }

        val employeeId = prefs.loggedInEmployeeId
        if (employeeId == -1L) {
            goToLogin()
            return
        }

        lifecycleScope.launch {
            val employee = repository.getEmployee(employeeId)
            if (employee == null || !employee.active) {
                goToLogin()
                return@launch
            }
            currentEmployee = employee
            supportActionBar?.title = "${getString(R.string.app_name)} — ${employee.name}"
            setupNavigation(employee)
        }
    }

    private fun setupNavigation(employee: Employee) {
        if (employee.role == EmployeeRole.SELLER) {
            binding.bottomNav.visibility = android.view.View.GONE
            showFragment(PosFragment())
        } else {
            binding.bottomNav.visibility = android.view.View.VISIBLE
            binding.bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_dashboard -> showFragment(DashboardFragment())
                    R.id.nav_pos -> showFragment(PosFragment())
                    R.id.nav_stock -> showFragment(StockFragment())
                    R.id.nav_reports -> showFragment(ReportsFragment())
                    R.id.nav_settings -> showFragment(SettingsFragment())
                }
                true
            }
            showFragment(DashboardFragment())
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_switch_user) {
            prefs.clearSession()
            goToLogin()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        sloganRotator?.stop()
        super.onDestroy()
    }
}
