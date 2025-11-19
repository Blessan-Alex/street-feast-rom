package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.adapters.NewOrdersAdapter
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.databinding.FragmentChefNewOrdersBinding
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.R

class ChefNewOrdersFragment : Fragment() {
    
    private var _binding: FragmentChefNewOrdersBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    private lateinit var adapter: NewOrdersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefNewOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        setupAcceptAllButton()
        observeOrders()
        observeMessages()
        observeNewOrders()
        observeOrderAccepted()
        
        viewModel.refresh()
    }
    
    private fun setupRecyclerView() {
        adapter = NewOrdersAdapter { order ->
            viewModel.acceptOrder(order.id)
        }
        
        binding.rvNewOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefNewOrdersFragment.adapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun setupAcceptAllButton() {
        binding.btnAcceptAll.setOnClickListener {
            viewModel.acceptAllOrders()
        }
        
        // Show/hide button based on order count
        viewModel.newOrders.observe(viewLifecycleOwner) { orders ->
            binding.btnAcceptAll.visibility = if (orders.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
    
    private fun observeOrders() {
        viewModel.newOrders.observe(viewLifecycleOwner) { orders ->
            // View might already be destroyed when this arrives
            val currentBinding = _binding ?: run {
                android.util.Log.w(
                    "ChefNewOrdersFragment",
                    "observeOrders called after view destroyed, ignoring update"
                )
                return@observe
            }

            android.util.Log.d("ChefNewOrdersFragment", "newOrders changed, size=${orders.size}")

            // Sort newest orders first (highest orderNumber on top)
            val sorted = orders.sortedWith(
                compareByDescending<com.streatfeast.app.models.Order> {
                    // Put valid numbers first, invalid (0) at the end
                    it.orderNumber.takeIf { num -> num > 0 } ?: Int.MIN_VALUE
                }.thenByDescending {
                    // If you have createdAt on the model, this gives stable ordering
                    it.createdAt
                }
            )

            // Capture the RecyclerView BEFORE submitList schedules its diff callback
            val recyclerView = currentBinding.rvNewOrders

            adapter.submitList(sorted) {
                if (sorted.isNotEmpty()) {
                    // Use the captured view, not binding
                    recyclerView.scrollToPosition(0)
                }
            }

            // Show/hide empty state using the safe binding reference
            if (sorted.isEmpty()) {
                currentBinding.emptyState.visibility = View.VISIBLE
                currentBinding.rvNewOrders.visibility = View.GONE
            } else {
                currentBinding.emptyState.visibility = View.GONE
                currentBinding.rvNewOrders.visibility = View.VISIBLE
            }
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
    
    private fun observeNewOrders() {
        viewModel.newOrderDetected.observe(viewLifecycleOwner) { (orderId, orderNumber) ->
            android.util.Log.d("ChefNewOrdersFragment", "newOrderDetected: id=$orderId, number=$orderNumber")

            val msg = orderNumber?.let { "New order #$it received!" } ?: "New order received!"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

            // Ask ViewModel to sync (this will eventually update Room)
            viewModel.refresh()
        }
    }
    
    private fun observeOrderAccepted() {
        viewModel.orderAccepted.observe(viewLifecycleOwner) { orderId ->
            // Move chef to Preparing page
            findNavController().navigate(R.id.chefPreparingFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


