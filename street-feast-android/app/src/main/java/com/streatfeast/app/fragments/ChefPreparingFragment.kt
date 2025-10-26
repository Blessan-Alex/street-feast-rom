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
import com.streatfeast.app.databinding.FragmentChefPreparingBinding
import com.streatfeast.app.viewmodels.OrdersViewModel

class ChefPreparingFragment : Fragment() {
    
    private var _binding: FragmentChefPreparingBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OrdersViewModel by viewModels()
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
        observeOrders()
        observeMessages()
        
        // Load mock data
        viewModel.loadPreparingOrders()
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
            // Reload mock data
            viewModel.loadPreparingOrders()
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun observeOrders() {
        viewModel.preparingOrders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
            
            // Show/hide empty state
            if (orders.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvPreparingOrders.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvPreparingOrders.visibility = View.VISIBLE
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


