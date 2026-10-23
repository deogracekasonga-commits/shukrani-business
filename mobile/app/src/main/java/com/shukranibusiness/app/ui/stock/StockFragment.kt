package com.shukranibusiness.app.ui.stock

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.databinding.DialogRestockBinding
import com.shukranibusiness.app.databinding.FragmentStockBinding
import com.shukranibusiness.app.util.CsvImporter
import kotlinx.coroutines.launch

class StockFragment : Fragment() {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ShopRepository
    private lateinit var adapter: StockAdapter

    private val pickCsvFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importCsv(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = ShopRepository(requireContext())

        adapter = StockAdapter(
            onEdit = { product -> showProductDialog(product) },
            onRestock = { product -> showRestockDialog(product) },
            onDelete = { product -> confirmDelete(product) }
        )
        binding.stockList.layoutManager = LinearLayoutManager(requireContext())
        binding.stockList.adapter = adapter

        binding.addProductButton.setOnClickListener { showProductDialog(null) }
        binding.importCsvButton.setOnClickListener { showImportExplanation() }

        lifecycleScope.launch {
            repository.observeActiveProducts().collect { products -> adapter.submit(products) }
        }
    }

    private fun showProductDialog(product: Product?) {
        ProductEditDialog(product) { updated ->
            lifecycleScope.launch {
                val barcode = updated.barcode
                if (barcode != null) {
                    val existing = repository.getProductByBarcode(barcode)
                    if (existing != null && existing.id != updated.id) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_barcode_already_used, existing.name),
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                }
                repository.saveProduct(updated)
            }
        }.show(childFragmentManager, "product_edit")
    }

    private fun showRestockDialog(product: Product) {
        val dialogBinding = DialogRestockBinding.inflate(LayoutInflater.from(requireContext()))
        AlertDialog.Builder(requireContext())
            .setTitle("${getString(R.string.dialog_restock_title)} — ${product.name}")
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val quantity = dialogBinding.restockQuantityInput.text.toString().toIntOrNull() ?: 0
                if (quantity > 0) {
                    val note = dialogBinding.restockNoteInput.text.toString().ifBlank { null }
                    lifecycleScope.launch { repository.restock(product.id, quantity, note) }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showImportExplanation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_import_csv)
            .setMessage(R.string.import_csv_explanation)
            .setPositiveButton(R.string.action_choose_file) { _, _ -> pickCsvFile.launch("*/*") }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun importCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val text = requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                val result = CsvImporter.parse(text)
                if (result.parsed.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.import_no_products), Toast.LENGTH_LONG).show()
                    return@launch
                }
                val count = repository.importProducts(result.parsed)
                val message = if (result.skippedLines > 0) {
                    getString(R.string.import_success_with_skipped, count, result.skippedLines)
                } else {
                    getString(R.string.import_success, count)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.import_error, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle(product.name)
            .setMessage(R.string.confirm_delete_product)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch { repository.deactivateProduct(product.id) }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
