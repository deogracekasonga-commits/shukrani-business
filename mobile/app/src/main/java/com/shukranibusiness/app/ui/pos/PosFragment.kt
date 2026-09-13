package com.shukranibusiness.app.ui.pos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val totalCdf = lines.sumOf { it.subtotalCdf }
        val totalUsd = lines.sumOf { it.subtotalUsd }
        binding.totalText.text = if (binding.usdRadio.isChecked) {
            CurrencyFormatter.formatUsd(totalUsd)
        } else {
            CurrencyFormatter.formatCdf(totalCdf)
        }
        binding.checkoutButton.isEnabled = lines.isNotEmpty()
    }

    private fun checkout() {
        val employee = currentEmployee ?: return
        val lines = cart.values.toList()
        if (lines.isEmpty()) return

        val currency = if (binding.usdRadio.isChecked) "USD" else "CDF"
        val exchangeRate = prefs.exchangeRateCdfPerUsd

        lifecycleScope.launch {
            try {
                val sale = repository.recordSale(employee, lines, currency, exchangeRate)
                cart.clear()
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
