package com.shukranibusiness.app.ui.employees

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.databinding.ItemEmployeeBinding

class EmployeeAdapter(
    private val onEdit: (Employee) -> Unit,
    private val onDelete: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.ViewHolder>() {

    private var employees: List<Employee> = emptyList()

    fun submit(newEmployees: List<Employee>) {
        employees = newEmployees
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val employee = employees[position]
        holder.binding.employeeName.text = employee.name
        holder.binding.employeeRole.text = holder.binding.root.context.getString(
            if (employee.role == EmployeeRole.MANAGER) R.string.role_manager else R.string.role_seller
        )
        holder.binding.editEmployeeButton.setOnClickListener { onEdit(employee) }
        holder.binding.deleteEmployeeButton.setOnClickListener { onDelete(employee) }
    }

    override fun getItemCount(): Int = employees.size

    class ViewHolder(val binding: ItemEmployeeBinding) : RecyclerView.ViewHolder(binding.root)
}
