package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.adapters.NewOrdersAdapter
import com.streatfeast.app.databinding.FragmentChefNewOrdersBinding
import com.streatfeast.app.viewmodels.OrdersViewModel

class ChefNewOrdersFragment : Fragment() {
    
    private var _binding: FragmentChefNewOrdersBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OrdersViewModel by viewModels()
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
        observeOrders()
        observeMessages()
        
        // Load mock data
        viewModel.loadNewOrders()
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
            // Reload mock data
            viewModel.loadNewOrders()
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun observeOrders() {
        viewModel.newOrders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
            
            // Show/hide empty state
            if (orders.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvNewOrders.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvNewOrders.visibility = View.VISIBLE
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


