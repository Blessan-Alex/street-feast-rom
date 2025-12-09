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
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.dialogs.OrderModificationDialog

@RequiresApi(Build.VERSION_CODES.O)
class GivenOrderFragment : Fragment() {

    private var _binding: FragmentGivenOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }

    private lateinit var tableChipAdapter: TableChipAdapter
    private lateinit var orderCardAdapter: GivenOrderCardAdapter

    private var allOrders: List<Order> = emptyList()
    private var filteredOrders: List<Order> = emptyList()
    private var selectedTableChip: TableChip? = null

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

        setupTableChipAdapter()
        setupOrderCardAdapter()
        setupSearch()
        setupBottomNavigation()
        observeDeliveredOrders()
        observeMessages()
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
                val bundle = Bundle().apply {
                    putString("existingOrderId", order.id)
                    putString("orderType", order.type.name)
                    putInt("tableNumber", extractTableNumber(order))
                    order.licensePlate?.let { putString("licensePlate", it) }
                }
                findNavController().navigate(R.id.previewOrderFragment, bundle)
            }
        )

        binding.rvGivenBody.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGivenBody.adapter = orderCardAdapter
    }
    
    private fun navigateToEditOrder(order: Order) {
        // Navigate to preview order with edit mode
        val bundle = Bundle().apply {
            putString("existingOrderId", order.id)
            putBoolean("isEditing", true)  // Key flag for edit mode
            putString("orderType", order.type.name)
            putInt("tableNumber", extractTableNumber(order))
            order.licensePlate?.let { putString("licensePlate", it) }
        }
        findNavController().navigate(R.id.previewOrderFragment, bundle)
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
        // Observe editable orders instead of just delivered orders
        // Editable orders: Created, Accepted, InKitchen, Prepared
        viewModel.editableOrders.observe(viewLifecycleOwner) { orders ->
            allOrders = orders
            updateTableChips(orders)
            filterOrders()
        }
    }

    private fun updateTableChips(orders: List<Order>) {
        val chips = orders.map { order ->
            TableChip(
                tableNumber = extractTableNumber(order),
                orderNumber = order.orderNumber,
                orderId = order.id
            )
        }.distinctBy { it.tableNumber to it.orderNumber }

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
                extractTableNumber(order) == chip.tableNumber && 
                order.orderNumber == chip.orderNumber
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
        _binding = null
    }
}

