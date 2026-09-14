package com.shukranibusiness.app.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.databinding.FragmentSettingsBinding
import com.shukranibusiness.app.sync.SupabaseSyncClient
import com.shukranibusiness.app.ui.employees.EmployeesActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs

    private val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showPrinterPicker()
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_bluetooth_permission), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        binding.shopNameInput.setText(prefs.shopName)
        binding.exchangeRateInput.setText(prefs.exchangeRateCdfPerUsd.toString())
        updatePrinterStatus()

        binding.saveSettingsButton.setOnClickListener {
            val name = binding.shopNameInput.text.toString().trim()
            if (name.isNotEmpty()) prefs.shopName = name
            binding.exchangeRateInput.text.toString().toDoubleOrNull()?.let { prefs.exchangeRateCdfPerUsd = it }
            Toast.makeText(requireContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        }

        binding.choosePrinterButton.setOnClickListener { onChoosePrinterClicked() }
        binding.manageEmployeesButton.setOnClickListener {
            startActivity(Intent(requireContext(), EmployeesActivity::class.java))
        }

        updateSyncStatus()
        binding.registerSyncButton.setOnClickListener { onRegisterSyncClicked() }
        binding.resetSyncButton.setOnClickListener {
            prefs.clearDeviceRegistration()
            updateSyncStatus()
        }
    }

    private fun onRegisterSyncClicked() {
        val setupKey = binding.setupKeyInput.text.toString().trim()
        if (setupKey.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_setup_key_required), Toast.LENGTH_SHORT).show()
            return
        }
        val role = if (binding.syncRoleManagerRadio.isChecked) "manager" else "shop"
        val shopName = binding.shopNameInput.text.toString().trim().ifEmpty { prefs.shopName }

        binding.registerSyncButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val result = SupabaseSyncClient.registerDevice(setupKey, shopName, role)
                prefs.deviceToken = result.deviceToken
                prefs.deviceId = result.deviceId
                prefs.deviceRole = role
                prefs.shopName = shopName
                Toast.makeText(requireContext(), getString(R.string.sync_registered_success), Toast.LENGTH_SHORT).show()
                updateSyncStatus()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_sync_registration_failed, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.registerSyncButton.isEnabled = true
            }
        }
    }

    private fun updateSyncStatus() {
        if (prefs.isRemoteSyncConfigured) {
            binding.syncSetupGroup.visibility = View.GONE
            binding.resetSyncButton.visibility = View.VISIBLE
            binding.syncStatusText.text = if (prefs.isManagerDevice) {
                getString(R.string.sync_configured_manager, prefs.shopName)
            } else {
                getString(R.string.sync_configured_shop, prefs.shopName)
            }
        } else {
            binding.syncSetupGroup.visibility = View.VISIBLE
            binding.resetSyncButton.visibility = View.GONE
            binding.syncStatusText.text = getString(R.string.sync_not_configured)
        }
    }

    private fun onChoosePrinterClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        showPrinterPicker()
    }

    private fun showPrinterPicker() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val paired = try {
            adapter?.bondedDevices
        } catch (e: SecurityException) {
            null
        }
        if (paired.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_no_paired_devices), Toast.LENGTH_LONG).show()
            return
        }

        val devices = paired.toList()
        val names = devices.map { device ->
            try {
                device.name ?: device.address
            } catch (e: SecurityException) {
                device.address
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_pair_printer)
            .setItems(names) { _, which ->
                prefs.printerAddress = devices[which].address
                updatePrinterStatus()
            }
            .show()
    }

    private fun updatePrinterStatus() {
        val address = prefs.printerAddress
        binding.printerStatus.text = if (address == null) {
            getString(R.string.printer_none_selected)
        } else {
            getString(R.string.printer_selected, address)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
