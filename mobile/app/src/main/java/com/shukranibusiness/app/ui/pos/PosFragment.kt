package com.shukranibusiness.app.ui.pos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.shukranibusiness.app.R
import com.shukranibusiness.app.data.CartLine
import com.shukranibusiness.app.data.InsufficientStockException
import com.shukranibusiness.app.data.Prefs
import com.shukranibusiness.app.data.ShopRepository
import com.shukranibusiness.app.data.entities.Employee
import com.shukranibusiness.app.data.entities.PaymentMethod
import com.shukranibusiness.app.data.entities.Product
import com.shukranibusiness.app.databinding.FragmentPosBinding
import com.shukranibusiness.app.util.CurrencyFormatter
import com.shukranibusiness.app.util.ReceiptPrinter
import kotlinx.coroutines.launch

class PosFragment : Fragment() {

    private var _binding: FragmentPosBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ShopRepository
    private lateinit var prefs: Prefs

    private var allProducts: List<Product> = emptyList()
    private var categories: List<String> = listOf(ALL_CATEGORIES)
    private var selectedCategory: String = ALL_CATEGORIES
    private val cart = LinkedHashMap<Long, CartLine>()
    private var currentEmployee: Employee? = null

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = ShopRepository(requireContext())
        prefs = Prefs(requireContext())

        categoryAdapter = CategoryAdapter { category -> onCategorySelected(category) }
        binding.categoryList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryList.adapter = categoryAdapter

        productAdapter = ProductAdapter { product -> addToCart(product) }
        binding.productGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.productGrid.adapter = productAdapter

        cartAdapter = CartAdapter(
            onIncrement = { productId -> changeCartQuantity(productId, 1) },
            onDecrement = { productId -> changeCartQuantity(productId, -1) },
            onRemove = { productId -> cart.remove(productId); refreshCartUi() }
        )
        binding.cartList.layoutManager = LinearLayoutManager(requireContext())
        binding.cartList.adapter = cartAdapter

        binding.currencyGroup.setOnCheckedChangeListener { _, _ -> refreshCartUi() }
        binding.checkoutButton.setOnClickListener { checkout() }

        binding.mobileMoneyProviderSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.mobile_money_providers)
        )
        binding.paymentMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.mobileMoneyProviderSpinner.visibility =
                if (checkedId == R.id.paymentMobileMoneyRadio) View.VISIBLE else View.GONE
        }

        binding.discountModeGroup.setOnCheckedChangeListener { _, _ -> refreshCartUi() }
        binding.discountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshCartUi()
        })

        binding.creditSaleCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.creditSaleGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        lifecycleScope.launch {
            val employeeId = prefs.loggedInEmployeeId
            currentEmployee = if (employeeId != -1L) repository.getEmployee(employeeId) else null
        }

        lifecycleScope.launch {
            repository.observeCategories().collect { cats ->
                categories = listOf(ALL_CATEGORIES) + cats
                categoryAdapter.submit(categories, selectedCategory)
            }
        }

        lifecycleScope.launch {
            repository.observeActiveProducts().collect { products ->
                allProducts = products
                applyFilter()
            }
        }

        refreshCartUi()
    }

    private fun onCategorySelected(category: String) {
        selectedCategory = category
        categoryAdapter.submit(categories, selectedCategory)
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (selectedCategory == ALL_CATEGORIES) {
            allProducts
        } else {
            allProducts.filter { it.category == selectedCategory }
        }
        productAdapter.submit(filtered)
    }

    private fun addToCart(product: Product) {
        val existing = cart[product.id]
        val newQuantity = (existing?.quantity ?: 0) + 1
        if (newQuantity > product.quantity) {
            Toast.makeText(requireContext(), getString(R.string.error_stock_max, product.quantity), Toast.LENGTH_SHORT).show()
            return
        }
        cart[product.id] = CartLine(product, newQuantity)
        refreshCartUi()
    }

    private fun changeCartQuantity(productId: Long, delta: Int) {
        val line = cart[productId] ?: return
        val newQuantity = line.quantity + delta
        when {
            newQuantity <= 0 -> cart.remove(productId)
            newQuantity > line.product.quantity -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_stock_max, line.product.quantity),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            else -> cart[productId] = line.copy(quantity = newQuantity)
        }
        refreshCartUi()
    }

    private fun refreshCartUi() {
        val lines = cart.values.toList()
        cartAdapter.submit(lines)
        binding.cartEmptyLabel.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE

        val subtotalCdf = lines.sumOf { it.subtotalCdf }
        val subtotalUsd = lines.sumOf { it.subtotalUsd }
        val (discountCdf, discountUsd) = computeDiscount(subtotalCdf, subtotalUsd)
        val totalCdf = subtotalCdf - discountCdf
        val totalUsd = subtotalUsd - discountUsd

        if (discountCdf > 0.0 || discountUsd > 0.0) {
            binding.subtotalText.text = "${getString(R.string.label_subtotal)} : " +
                CurrencyFormatter.formatBoth(subtotalCdf, subtotalUsd) +
                "  —  ${getString(R.string.label_discount)} : " +
                CurrencyFormatter.formatBoth(discountCdf, discountUsd)
            binding.subtotalText.visibility = View.VISIBLE
        } else {
            binding.subtotalText.visibility = View.GONE
        }

        binding.totalText.text = if (binding.usdRadio.isChecked) {
            CurrencyFormatter.formatUsd(totalUsd)
        } else {
            CurrencyFormatter.formatCdf(totalCdf)
        }
        binding.checkoutButton.isEnabled = lines.isNotEmpty()
    }

    /** Calcule la remise (en CDF et USD) à partir de la saisie et du mode choisi (% ou montant CDF). */
    private fun computeDiscount(subtotalCdf: Double, subtotalUsd: Double): Pair<Double, Double> {
        val rawInput = binding.discountInput.text.toString().toDoubleOrNull() ?: return 0.0 to 0.0
        if (rawInput <= 0.0) return 0.0 to 0.0

        val discountCdf = if (binding.discountPercentRadio.isChecked) {
            subtotalCdf * (rawInput.coerceIn(0.0, 100.0) / 100.0)
        } else {
            rawInput
        }.coerceIn(0.0, subtotalCdf)

        val discountUsd = if (subtotalCdf > 0.0) subtotalUsd * (discountCdf / subtotalCdf) else 0.0
        return discountCdf to discountUsd.coerceIn(0.0, subtotalUsd)
    }

    private fun checkout() {
        val employee = currentEmployee ?: return
        val lines = cart.values.toList()
        if (lines.isEmpty()) return

        val currency = if (binding.usdRadio.isChecked) "USD" else "CDF"
        val exchangeRate = prefs.exchangeRateCdfPerUsd
        val paymentMethod = when (binding.paymentMethodGroup.checkedRadioButtonId) {
            R.id.paymentMobileMoneyRadio -> PaymentMethod.MOBILE_MONEY
            R.id.paymentCardRadio -> PaymentMethod.CARD
            else -> PaymentMethod.CASH
        }
        val mobileMoneyProvider = if (paymentMethod == PaymentMethod.MOBILE_MONEY) {
            binding.mobileMoneyProviderSpinner.selectedItem as? String
        } else {
            null
        }
        val subtotalCdf = lines.sumOf { it.subtotalCdf }
        val subtotalUsd = lines.sumOf { it.subtotalUsd }
        val (discountCdf, discountUsd) = computeDiscount(subtotalCdf, subtotalUsd)
        val totalCdf = subtotalCdf - discountCdf
        val totalUsd = subtotalUsd - discountUsd

        var amountPaidCdf: Double? = null
        var amountPaidUsd: Double? = null
        var customerReference: String? = null
        if (binding.creditSaleCheckbox.isChecked) {
            customerReference = binding.customerReferenceInput.text.toString().trim().ifEmpty { null }
            val rawPaid = binding.amountPaidInput.text.toString().toDoubleOrNull() ?: 0.0
            if (binding.usdRadio.isChecked) {
                amountPaidUsd = rawPaid.coerceIn(0.0, totalUsd)
                amountPaidCdf = if (totalUsd > 0.0) totalCdf * (amountPaidUsd / totalUsd) else 0.0
            } else {
                amountPaidCdf = rawPaid.coerceIn(0.0, totalCdf)
                amountPaidUsd = if (totalCdf > 0.0) totalUsd * (amountPaidCdf / totalCdf) else 0.0
            }
        }

        lifecycleScope.launch {
            try {
                val sale = repository.recordSale(
                    employee, lines, currency, exchangeRate, paymentMethod, mobileMoneyProvider,
                    discountCdf, discountUsd, amountPaidCdf, amountPaidUsd, customerReference
                )
                cart.clear()
                binding.discountInput.text?.clear()
                binding.creditSaleCheckbox.isChecked = false
                binding.customerReferenceInput.text?.clear()
                binding.amountPaidInput.text?.clear()
                refreshCartUi()
                Toast.makeText(requireContext(), getString(R.string.sale_recorded), Toast.LENGTH_SHORT).show()
                ReceiptPrinter.printReceiptIfConfigured(requireContext(), sale, lines, prefs.shopName)
            } catch (e: InsufficientStockException) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ALL_CATEGORIES = "Tous"
    }
}
