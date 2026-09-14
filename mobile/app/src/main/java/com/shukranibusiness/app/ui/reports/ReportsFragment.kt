package com.shukranibusiness.app.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.EmployeeRole
import com.shukranibusiness.app.data.entities.Sale
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.databinding.FragmentReportsBinding
import com.shukranibusiness.app.util.CsvExporter
import com.shukranibusiness.app.util.CurrencyFormatter
import com.shukranibusiness.app.util.FileSharer
import com.shukranibusiness.app.util.PdfExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ShopRepository
    private lateinit var prefs: Prefs
    private lateinit var adapter: SaleAdapter

    private var collectJob: Job? = null
    private var currentSales: List<Sale> = emptyList()
    private var currentPeriodLabel: String = ""
    private var periodStart: Long = 0L
    private var periodEnd: Long = System.currentTimeMillis()
    private var currentEmployee: Employee? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = ShopRepository(requireContext())
        prefs = Prefs(requireContext())
        adapter = SaleAdapter(onClick = { sale -> onSaleClicked(sale) })
        binding.salesList.layoutManager = LinearLayoutManager(requireContext())
        binding.salesList.adapter = adapter

        lifecycleScope.launch {
            val employeeId = prefs.loggedInEmployeeId
            currentEmployee = if (employeeId != -1L) repository.getEmployee(employeeId) else null
        }

        binding.periodTodayButton.setOnClickListener {
            selectPeriod(periodToday(), getString(R.string.period_today))
        }
        binding.periodWeekButton.setOnClickListener {
            selectPeriod(periodDaysAgo(7), getString(R.string.period_week))
        }
        binding.periodMonthButton.setOnClickListener {
            selectPeriod(periodDaysAgo(30), getString(R.string.period_month))
        }
        binding.periodAllButton.setOnClickListener {
            selectPeriod(0L to System.currentTimeMillis(), getString(R.string.period_all))
        }

        binding.exportCsvButton.setOnClickListener { exportCsv() }
        binding.exportPdfButton.setOnClickListener { exportPdf() }

        selectPeriod(periodToday(), getString(R.string.period_today))
    }

    private fun selectPeriod(range: Pair<Long, Long>, label: String) {
        periodStart = range.first
        periodEnd = range.second
        currentPeriodLabel = label

        collectJob?.cancel()
        collectJob = lifecycleScope.launch {
            repository.observeSalesBetween(periodStart, periodEnd).collect { sales ->
                currentSales = sales
                adapter.submit(sales)
                val completed = sales.filter { it.status == SaleStatus.COMPLETED }
                val totalCdf = completed.sumOf { it.totalCdf }
                val totalUsd = completed.sumOf { it.totalUsd }
                binding.summaryTotal.text = CurrencyFormatter.formatBoth(totalCdf, totalUsd)
                binding.summaryCount.text = getString(R.string.label_sales_count, completed.size)

                val completedIds = completed.map { it.id }.toSet()
                val items = repository.getSaleItemsBetween(periodStart, periodEnd)
                    .filter { it.saleId in completedIds }
                if (items.any { it.unitCostCdf > 0 || it.unitCostUsd > 0 }) {
                    val marginCdf = items.sumOf { it.subtotalCdf - it.unitCostCdf * it.quantity }
                    val marginUsd = items.sumOf { it.subtotalUsd - it.unitCostUsd * it.quantity }
                    binding.summaryMargin.text =
                        "${getString(R.string.label_margin)} : ${CurrencyFormatter.formatBoth(marginCdf, marginUsd)}"
                    binding.summaryMargin.visibility = View.VISIBLE
                } else {
                    binding.summaryMargin.visibility = View.GONE
                }
            }
        }
    }

    private fun exportCsv() {
        if (currentSales.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_data_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val items = repository.getSaleItemsBetween(periodStart, periodEnd)
            val itemsBySale = items.groupBy { it.saleId }
            val file = CsvExporter.exportSales(requireContext(), currentSales, itemsBySale)
            FileSharer.share(requireContext(), file, "text/csv")
        }
    }

    private fun exportPdf() {
        if (currentSales.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_data_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        val completed = currentSales.filter { it.status == SaleStatus.COMPLETED }
        val totalCdf = completed.sumOf { it.totalCdf }
        val totalUsd = completed.sumOf { it.totalUsd }
        lifecycleScope.launch {
            val completedIds = completed.map { it.id }.toSet()
            val items = repository.getSaleItemsBetween(periodStart, periodEnd)
                .filter { it.saleId in completedIds }
            val hasCost = items.any { it.unitCostCdf > 0 || it.unitCostUsd > 0 }
            val marginCdf = if (hasCost) items.sumOf { it.subtotalCdf - it.unitCostCdf * it.quantity } else null
            val marginUsd = if (hasCost) items.sumOf { it.subtotalUsd - it.unitCostUsd * it.quantity } else null

            val file = PdfExporter.exportSalesSummary(
                requireContext(), currentPeriodLabel, currentSales, totalCdf, totalUsd, prefs.shopName,
                marginCdf, marginUsd
            )
            FileSharer.share(requireContext(), file, "application/pdf")
        }
    }

    private fun onSaleClicked(sale: Sale) {
        lifecycleScope.launch {
            val items = repository.getSaleItems(sale.id)
            val canCancel = currentEmployee?.role == EmployeeRole.MANAGER
            SaleDetailDialog(sale, items, canCancel) { toCancel ->
                lifecycleScope.launch {
                    val employee = currentEmployee ?: return@launch
                    repository.cancelSale(toCancel.id, employee)
                    Toast.makeText(requireContext(), getString(R.string.sale_canceled_confirmation), Toast.LENGTH_SHORT).show()
                }
            }.show(childFragmentManager, "sale_detail")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun periodToday(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to System.currentTimeMillis()
    }

    private fun periodDaysAgo(days: Int): Pair<Long, Long> {
        val end = System.currentTimeMillis()
        val start = end - days * 24L * 60L * 60L * 1000L
        return start to end
    }
}
