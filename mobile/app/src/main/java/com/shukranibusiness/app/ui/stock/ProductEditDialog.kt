package com.shukranibusiness.app.ui.stock

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.databinding.DialogProductEditBinding

class ProductEditDialog(
    private val existing: Product?,
    private val onSave: (Product) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogProductEditBinding.inflate(layoutInflater)

        existing?.let { product ->
            binding.nameInput.setText(product.name)
            binding.categoryInput.setText(product.category)
            binding.priceCdfInput.setText(product.priceCdf.toString())
            binding.priceUsdInput.setText(product.priceUsd.toString())
            if (product.purchasePriceCdf > 0) binding.purchasePriceCdfInput.setText(product.purchasePriceCdf.toString())
            if (product.purchasePriceUsd > 0) binding.purchasePriceUsdInput.setText(product.purchasePriceUsd.toString())
            binding.quantityInput.setText(product.quantity.toString())
            binding.thresholdInput.setText(product.lowStockThreshold.toString())
        }

        val title = if (existing == null) {
            getString(R.string.dialog_product_title_add)
        } else {
            getString(R.string.dialog_product_title_edit)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val product = Product(
                    id = existing?.id ?: 0L,
                    name = binding.nameInput.text.toString().trim(),
                    category = binding.categoryInput.text.toString().trim().ifEmpty { "Général" },
                    priceCdf = binding.priceCdfInput.text.toString().toDoubleOrNull() ?: 0.0,
                    priceUsd = binding.priceUsdInput.text.toString().toDoubleOrNull() ?: 0.0,
                    purchasePriceCdf = binding.purchasePriceCdfInput.text.toString().toDoubleOrNull() ?: 0.0,
                    purchasePriceUsd = binding.purchasePriceUsdInput.text.toString().toDoubleOrNull() ?: 0.0,
                    quantity = binding.quantityInput.text.toString().toIntOrNull() ?: 0,
                    lowStockThreshold = binding.thresholdInput.text.toString().toIntOrNull() ?: 5
                )
                if (product.name.isNotEmpty()) onSave(product)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }
}
