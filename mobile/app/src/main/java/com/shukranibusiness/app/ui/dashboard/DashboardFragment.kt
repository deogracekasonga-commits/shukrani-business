package com.shukranibusiness.app.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.SaleStatus
import com.shukranibusiness.app.databinding.FragmentDashboardBinding
import com.shukranibusiness.app.sync.RemoteSale
import com.shukranibusiness.app.sync.SupabaseSyncClient
import com.shukranibusiness.app.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ShopRepository
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = ShopRepository(requireContext())
        prefs = Prefs(requireContext())

        binding.greetingText.text = getString(R.string.dashboard_greeting, prefs.shopName)

        lifecycleScope.launch {
            repository.observeSalesBetween(startOfToday(), System.currentTimeMillis()).collect { sales ->
                val completed = sales.filter { it.status == SaleStatus.COMPLETED }
                binding.todayTotalText.text = CurrencyFormatter.formatBoth(
                    completed.sumOf { it.totalCdf },
                    completed.sumOf { it.totalUsd }
                )
                binding.todayCountText.text = getString(R.string.label_sales_count, completed.size)
            }
        }

        lifecycleScope.launch {
            repository.observeLowStockProducts().collect { products ->
                binding.lowStockContainer.removeAllViews()
                if (products.isEmpty()) {
                    binding.lowStockContainer.addView(
                        makeRow(getString(R.string.dashboard_no_low_stock), Color.parseColor("#666666"))
                    )
                } else {
                    for (product in products) {
                        binding.lowStockContainer.addView(
                            makeRow("${product.name} — ${product.quantity}", Color.parseColor("#D32F2F"))
                        )
                    }
                }
            }
        }

        if (prefs.isRemoteSyncConfigured && prefs.isManagerDevice) {
            binding.remoteSalesCard.visibility = View.VISIBLE
            binding.refreshRemoteButton.setOnClickListener { loadRemoteSales() }
            loadRemoteSales()
        }
    }

    private fun loadRemoteSales() {
        val deviceToken = prefs.deviceToken ?: return
        binding.remoteSalesContainer.removeAllViews()
        binding.remoteSalesContainer.addView(makeRow(getString(R.string.dashboard_remote_loading), Color.parseColor("#999999")))

        lifecycleScope.launch {
            try {
                val sales = SupabaseSyncClient.fetchRemoteSales(deviceToken)
                binding.remoteSalesContainer.removeAllViews()
                if (sales.isEmpty()) {
                    binding.remoteSalesContainer.addView(
                        makeRow(getString(R.string.dashboard_remote_empty), Color.parseColor("#666666"))
                    )
                } else {
                    for (sale in sales) {
                        binding.remoteSalesContainer.addView(remoteSaleRow(sale))
                    }
                }
            } catch (e: Exception) {
                binding.remoteSalesContainer.removeAllViews()
                Toast.makeText(requireContext(), getString(R.string.error_remote_load_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun remoteSaleRow(sale: RemoteSale): TextView {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE)
        val canceled = sale.status == "CANCELED"
        val prefix = if (canceled) "✕ " else ""
        val text = "$prefix${sale.shopName} — ${sale.employeeName} — " +
            CurrencyFormatter.formatBoth(sale.totalCdf, sale.totalUsd) +
            " (${sdf.format(Date(sale.dateTimeMillis))})"
        return makeRow(text, if (canceled) Color.parseColor("#9AA3BC") else Color.parseColor("#182238"))
    }

    private fun makeRow(text: String, color: Int): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setTextColor(color)
            setPadding(0, 6, 0, 6)
        }
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
