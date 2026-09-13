package com.shukranibusiness.app.ui.employees

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.databinding.DialogEmployeeEditBinding

class EmployeeEditDialog(
    private val existing: Employee?,
    private val onSave: (name: String, pin: String?, role: EmployeeRole) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogEmployeeEditBinding.inflate(layoutInflater)

        existing?.let { employee ->
            binding.nameInput.setText(employee.name)
            if (employee.role == EmployeeRole.MANAGER) {
                binding.managerRadio.isChecked = true
            } else {
                binding.sellerRadio.isChecked = true
            }
        }

        val title = if (existing == null) {
            getString(R.string.action_add_employee)
        } else {
            getString(R.string.action_edit)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = binding.nameInput.text.toString().trim()
                val pin = binding.pinInput.text.toString()
                val pinConfirm = binding.pinConfirmInput.text.toString()
                val role = if (binding.managerRadio.isChecked) EmployeeRole.MANAGER else EmployeeRole.SELLER

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing == null && pin.length < 4) {
                    Toast.makeText(requireContext(), getString(R.string.error_pin_too_short), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (pin.isNotEmpty() && pin != pinConfirm) {
                    Toast.makeText(requireContext(), getString(R.string.error_pin_mismatch), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                onSave(name, pin.ifEmpty { null }, role)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }
}
