package com.streatfeast.app.fragments

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.GivenOrderCardAdapter
import com.streatfeast.app.adapters.TableChip
import com.streatfeast.app.adapters.TableChipAdapter
import com.streatfeast.app.databinding.FragmentGivenOrderBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.navigation.OrderEditMode
import com.streatfeast.app.navigation.OrderNavArgs
import com.streatfeast.app.utils.TableDisplayMapper
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.dialogs.OrderModificationDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class GivenOrderFragment : Fragment() {

    private var _binding: FragmentGivenOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var tableChipAdapter: TableChipAdapter
    private lateinit var orderCardAdapter: GivenOrderCardAdapter

    private var allOrders: List<Order> = emptyList()
    private var filteredOrders: List<Order> = emptyList()
    private var selectedTableChip: TableChip? = null
    private var dateUpdateJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGivenOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppbar()
        setupTableChipAdapter()
        setupOrderCardAdapter()
        setupSearch()
        setupBottomNavigation()
        observeDeliveredOrders()
        observeMessages()
    }

    private fun setupAppbar() {
        // Hide back button (users can use system back)
        val navBack = binding.appbar.root.findViewById<View>(R.id.ivNavBack)
        navBack?.visibility = View.GONE
        
        // Setup real-time date
        setupDate()
        
        // Show logout button
        setupLogoutButton()
    }
    
    private fun setupDate() {
        val tvDate = binding.appbar.root.findViewById<TextView>(R.id.tvDate)
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        
        // Update date immediately
        fun updateDate() {
            tvDate?.text = dateFormat.format(Date())
        }
        
        updateDate()
        
        // Update date every minute
        dateUpdateJob = lifecycleScope.launch {
            while (true) {
                delay(60000) // 1 minute
                if (isAdded && view != null) {
                    updateDate()
                }
            }
        }
    }

    private fun setupLogoutButton() {
        val logoutButton = binding.appbar.root.findViewById<ViewGroup>(R.id.btnLogout)
        logoutButton?.visibility = View.VISIBLE
        logoutButton?.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // FIRST: Logout from auth
                    authViewModel.logout()
                    
                    // Wait a bit to ensure logout completes
                    delay(100)
                    
                    // THEN: Stop realtime and clear ServiceLocator
                    ServiceLocator.provideOrderRepository(requireContext().applicationContext).stopRealtime()
                    ServiceLocator.clear()
                } catch (e: Exception) {
                    android.util.Log.e("GivenOrderFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    ServiceLocator.clear()
                }
            }
        }
    }

    private fun setupTableChipAdapter() {
        tableChipAdapter = TableChipAdapter(emptyList()) { chip ->
            selectedTableChip = chip
            filterOrders()
        }

        binding.rvTables.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvTables.adapter = tableChipAdapter
    }

    private fun setupOrderCardAdapter() {
        orderCardAdapter = GivenOrderCardAdapter(
            onAlterOrderClick = { order, item ->
                // Check order status and show confirmation if needed
                val status = order.status
                if (!OrderModificationDialog.canModifyOrder(status)) {
                    // Show error - cannot alter
                    android.widget.Toast.makeText(
                        requireContext(),
                        OrderModificationDialog.getModificationErrorMessage(status),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@GivenOrderCardAdapter
                }
                
                // Show confirmation dialog if order is being prepared
                if (OrderModificationDialog.requiresWarning(status)) {
                    OrderModificationDialog.showPreparingOrderConfirmation(
                        requireContext(),
                        onConfirm = { navigateToEditOrder(order) }
                    )
                } else {
                    // Navigate directly for Created/Accepted orders
                    navigateToEditOrder(order)
                }
            },
            onAddItemsClick = { order ->
                // Navigate to preview order with existing order context (add items mode)
                val args = OrderNavArgs(
                    orderType = order.type,
                    tableNumber = extractTableNumber(order),
                    licensePlate = order.licensePlate,
                    existingOrderId = order.id,
                    editMode = OrderEditMode.ADD_ITEMS,
                    showHeader = false
                )
                findNavController().navigate(R.id.previewOrderFragment, args.toBundle())
            }
        )

        binding.rvGivenBody.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGivenBody.adapter = orderCardAdapter
    }
    
    private fun navigateToEditOrder(order: Order) {
        val args = OrderNavArgs(
            orderType = order.type,
            tableNumber = extractTableNumber(order),
            licensePlate = order.licensePlate,
            existingOrderId = order.id,
            editMode = OrderEditMode.EDIT,
            showHeader = false
        )
        findNavController().navigate(R.id.previewOrderFragment, args.toBundle())
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterOrders()
            }
        })
    }

    private fun observeDeliveredOrders() {
        // Observe editable orders but filter out fully prepared orders
        // Editable orders: Created, Accepted, InKitchen, Prepared
        // But exclude orders where all items are prepared (order is fully ready for delivery)
        viewModel.editableOrders.observe(viewLifecycleOwner) { orders ->
            // Filter out orders where all items are prepared
            val editableOrders = orders.filter { order ->
                // If order has items, check if all are prepared
                if (order.items.isNotEmpty()) {
                    // Order is editable if not all items are prepared
                    !order.items.all { it.isPrepared }
                } else {
                    // Empty orders are not editable
                    false
                }
            }
            allOrders = editableOrders
            updateTableChips(editableOrders)
            filterOrders()
        }
    }

    private fun updateTableChips(orders: List<Order>) {
        val chips = orders.mapNotNull { order ->
            TableDisplayMapper.toChip(order)
        }.distinctBy { it.orderId }

        tableChipAdapter = TableChipAdapter(chips) { chip ->
            selectedTableChip = chip
            filterOrders()
        }
        binding.rvTables.adapter = tableChipAdapter
    }

    private fun filterOrders() {
        val searchQuery = binding.etSearch.text.toString().lowercase().trim()
        
        filteredOrders = allOrders.filter { order ->
            // Filter by selected table chip
            val matchesTable = selectedTableChip?.let { chip ->
                order.id == chip.orderId
            } ?: true

            // Filter by search query
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                order.orderNumber.toString().contains(searchQuery) ||
                extractTableNumber(order).toString().contains(searchQuery) ||
                order.items.any { item ->
                    item.nameSnapshot.lowercase().contains(searchQuery) ||
                    item.chefTip.lowercase().contains(searchQuery)
                }
            }

            matchesTable && matchesSearch
        }

        orderCardAdapter.submitList(filteredOrders)
    }

    private fun setupBottomNavigation() {
        val bottomNavView = binding.bottomNav.root
        val navNewOrder = bottomNavView.findViewById<ViewGroup>(R.id.navNewOrder)
        val navReadyOrder = bottomNavView.findViewById<ViewGroup>(R.id.navReadyOrder)

        navNewOrder?.setOnClickListener {
            findNavController().navigate(R.id.orderTypeFragment)
        }

        navReadyOrder?.setOnClickListener {
            findNavController().navigate(R.id.readyOrderFragment)
        }
    }

    private fun observeMessages() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    private fun extractTableNumber(order: Order): Int {
        // Extract table number from order model
        return order.tableNumber ?: 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dateUpdateJob?.cancel()
        dateUpdateJob = null
        _binding = null
    }
}

