package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.adapters.PreparingOrdersAdapter
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.databinding.FragmentChefPreparingBinding
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory

class ChefPreparingFragment : Fragment() {
    
    private var _binding: FragmentChefPreparingBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    private lateinit var adapter: PreparingOrdersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChefPreparingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        setupMarkAllPreparedButton()
        observeOrders()
        observeMessages()
        
        viewModel.refresh()
    }
    
    private fun setupRecyclerView() {
        adapter = PreparingOrdersAdapter { order ->
            viewModel.markPrepared(order.id)
        }
        
        binding.rvPreparingOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChefPreparingFragment.adapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun setupMarkAllPreparedButton() {
        binding.btnMarkAllPrepared.setOnClickListener {
            viewModel.markAllPrepared()
        }
        
        // Show/hide button based on order count
        viewModel.preparingOrders.observe(viewLifecycleOwner) { orders ->
            binding.btnMarkAllPrepared.visibility = if (orders.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
    
    private fun observeOrders() {
        viewModel.preparingOrders.observe(viewLifecycleOwner) { orders ->
            val currentBinding = _binding ?: run {
                android.util.Log.w(
                    "ChefPreparingFragment",
                    "observeOrders called after view destroyed, ignoring update"
                )
                return@observe
            }

            android.util.Log.d("ChefPreparingFragment", "preparingOrders changed, size=${orders.size}")

            // Sort newest first (by order number, then by createdAt)
            val sorted = orders.sortedWith(
                compareByDescending<com.streatfeast.app.models.Order> {
                    it.orderNumber.takeIf { num -> num > 0 } ?: Int.MIN_VALUE
                }.thenByDescending {
                    it.createdAt
                }
            )

            val recyclerView = currentBinding.rvPreparingOrders

            adapter.submitList(sorted) {
                if (sorted.isNotEmpty()) {
                    recyclerView.scrollToPosition(0)
                }
            }

            if (sorted.isEmpty()) {
                currentBinding.emptyState.visibility = View.VISIBLE
                currentBinding.rvPreparingOrders.visibility = View.GONE
            } else {
                currentBinding.emptyState.visibility = View.GONE
                currentBinding.rvPreparingOrders.visibility = View.VISIBLE
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


