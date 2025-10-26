package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.adapters.ReadyOrdersAdapter
import com.streatfeast.app.databinding.FragmentWaiterReadyBinding
import com.streatfeast.app.viewmodels.OrdersViewModel

class WaiterReadyFragment : Fragment() {
    
    private var _binding: FragmentWaiterReadyBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var adapter: ReadyOrdersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaiterReadyBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeOrders()
        observeMessages()
        
        // Load mock data
        viewModel.loadReadyOrders()
    }
    
    private fun setupRecyclerView() {
        adapter = ReadyOrdersAdapter { order ->
            viewModel.markDelivered(order.id)
        }
        
        binding.rvReadyOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WaiterReadyFragment.adapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // Reload mock data
            viewModel.loadReadyOrders()
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun observeOrders() {
        viewModel.readyOrders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
            
            // Show/hide empty state
            if (orders.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvReadyOrders.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvReadyOrders.visibility = View.VISIBLE
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


