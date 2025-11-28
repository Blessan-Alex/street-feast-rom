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
                // TODO: Open ItemCustomizeBottomSheet in edit mode
                android.util.Log.d("GivenOrderFragment", "Alter order: ${order.id}, item: ${item.id}")
            },
            onAddItemsClick = { order ->
                // Navigate to item selection with existing order context
                val bundle = Bundle().apply {
                    putString("existingOrderId", order.id)
                    putString("orderType", order.type.name)
                    // Extract table number - placeholder for now
                    putInt("tableNumber", extractTableNumber(order))
                }
                findNavController().navigate(R.id.orderItemFragment, bundle)
            }
        )

        binding.rvGivenBody.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGivenBody.adapter = orderCardAdapter
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
        viewModel.deliveredOrders.observe(viewLifecycleOwner) { orders ->
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
                extractTableNumber(order).toString().contains(searchQuery)
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
        // TODO: Extract table number from order metadata
        // For now, using a placeholder - in real implementation, this should come from order
        return order.orderNumber % 20 + 1 // Placeholder: derive from order number
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

