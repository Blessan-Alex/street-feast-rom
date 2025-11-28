package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.PreviewOrderAdapter
import com.streatfeast.app.databinding.FragmentPreviewOrderBinding
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreviewOrderFragment : Fragment() {
    
    private var _binding: FragmentPreviewOrderBinding? = null
    private val binding get() = _binding!!
    
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    
    private lateinit var adapter: PreviewOrderAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewOrderBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get arguments
        arguments?.let { args ->
            tableNumber = args.getInt("tableNumber", 4)
            args.getString("orderType")?.let { typeString ->
                orderType = try {
                    OrderType.valueOf(typeString)
                } catch (e: IllegalArgumentException) {
                    OrderType.DINE_IN
                }
            }
        }
        
        setupTableHandle()
        setupAddButton()
        setupRecyclerView()
        setupPlaceOrderBar()
        setupBottomNavigation()
        observeDraftItems()
    }
    
    private fun setupTableHandle() {
        val tableHandleView = binding.tableHandle.root
        val tvTable = tableHandleView.findViewById<TextView>(R.id.tvTable)
        tvTable?.text = "Table $tableNumber"
        
        // Handle down chevron click - expand to Screen 11
        val ivDown = tableHandleView.findViewById<ImageView>(R.id.ivDown)
        ivDown?.setOnClickListener {
            navigateToHeaderView()
        }
    }
    
    private fun setupAddButton() {
        // Handle "+" button click - navigate back to item selection
        binding.btnAdd.setOnClickListener {
            // Navigate back to item selection (Screen 3/4 or Screen 5)
            findNavController().popBackStack(R.id.orderItemFragment, false)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = PreviewOrderAdapter(
            items = emptyList(),
            onQuantityDecrease = { item ->
                draftViewModel.updateDraftItem(item.id) { currentItem ->
                    currentItem.copy(qty = (currentItem.qty - 1).coerceAtLeast(1))
                }
            },
            onQuantityIncrease = { item ->
                draftViewModel.updateDraftItem(item.id) { currentItem ->
                    currentItem.copy(qty = (currentItem.qty + 1).coerceAtMost(99))
                }
            },
            onAlterClick = { item ->
                // TODO: Open customization modal with existing item data
                // For now, just log
                android.util.Log.d("PreviewOrderFragment", "Alter item: ${item.nameSnapshot}")
            },
            onRemoveClick = { item ->
                draftViewModel.removeDraftItem(item.id)
            }
        )
        
        binding.rvPreview.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPreview.adapter = adapter
    }
    
    private fun setupPlaceOrderBar() {
        // Observe item count and update badge
        draftViewModel.itemCount.observe(viewLifecycleOwner) { count ->
            val badgeCount = binding.placeOrderBar.root.findViewById<TextView>(R.id.tvBadgeCount)
            badgeCount?.text = count.toString()
            
            // Show/hide place order bar based on count
            if (count > 0) {
                binding.placeOrderBar.root.visibility = View.VISIBLE
            } else {
                binding.placeOrderBar.root.visibility = View.GONE
            }
        }
        
        // Handle place order click
        binding.placeOrderBar.root.setOnClickListener {
            placeOrder()
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNavView = binding.bottomNav.root
        val navReadyOrder = bottomNavView.findViewById<ViewGroup>(R.id.navReadyOrder)
        val navGivenOrder = bottomNavView.findViewById<ViewGroup>(R.id.navGivenOrder)
        
        navReadyOrder?.setOnClickListener {
            findNavController().navigate(R.id.readyOrderFragment)
        }
        
        navGivenOrder?.setOnClickListener {
            findNavController().navigate(R.id.givenOrderFragment)
        }
    }
    
    private fun observeDraftItems() {
        draftViewModel.draftItems.observe(viewLifecycleOwner) { items ->
            adapter = PreviewOrderAdapter(
                items = items,
                onQuantityDecrease = { item ->
                    draftViewModel.updateDraftItem(item.id) { currentItem ->
                        currentItem.copy(qty = (currentItem.qty - 1).coerceAtLeast(1))
                    }
                },
                onQuantityIncrease = { item ->
                    draftViewModel.updateDraftItem(item.id) { currentItem ->
                        currentItem.copy(qty = (currentItem.qty + 1).coerceAtMost(99))
                    }
                },
                onAlterClick = { item ->
                    // TODO: Open customization modal with existing item data
                    android.util.Log.d("PreviewOrderFragment", "Alter item: ${item.nameSnapshot}")
                },
                onRemoveClick = { item ->
                    draftViewModel.removeDraftItem(item.id)
                }
            )
            binding.rvPreview.adapter = adapter
        }
    }
    
    private fun navigateToHeaderView() {
        val bundle = Bundle().apply {
            putInt("tableNumber", tableNumber)
            putString("orderType", orderType.name)
        }
        findNavController().navigate(R.id.previewOrderHeaderFragment, bundle)
    }
    
    private fun placeOrder() {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) {
            // Show error message
            android.util.Log.e("PreviewOrderFragment", "Cannot place order: no items")
            return
        }
        
        // TODO: Implement order placement via Supabase
        // Similar to admin placeDraft function
        // For now, just log and clear draft
        android.util.Log.d("PreviewOrderFragment", "Placing order with ${items.size} items")
        
        // Placeholder: Clear draft after "placing" order
        // In real implementation, this should happen after successful order creation
        CoroutineScope(Dispatchers.Main).launch {
            // TODO: Call Supabase RPC orders_upsert
            // For now, just clear draft
            draftViewModel.clearDraft()
            
            // Navigate back to start or to Given Order tab
            findNavController().popBackStack(R.id.orderTypeFragment, false)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

