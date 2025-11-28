package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.PreviewOrderAdapter
import com.streatfeast.app.databinding.FragmentPreviewOrderHeaderBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreviewOrderHeaderFragment : Fragment() {
    
    private var _binding: FragmentPreviewOrderHeaderBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    
    private lateinit var adapter: PreviewOrderAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewOrderHeaderBinding.inflate(inflater, container, false)
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
        
        setupAppbar()
        setupLogoutButton()
        setupStepper()
        setupRecyclerView()
        setupPlaceOrderBar()
        setupBottomNavigation()
        observeDraftItems()
        
        // Handle up chevron click
        binding.ivUp.setOnClickListener {
            navigateToCompactView()
        }
        
        // Handle go back button
        binding.btnGoBack.setOnClickListener {
            navigateToItemSelection()
        }
    }
    
    private fun setupAppbar() {
        val appbarView = binding.appbar.root
        val btnClose = appbarView.findViewById<View>(R.id.btnClose)
        btnClose?.setOnClickListener {
            // Navigate back or clear draft
            findNavController().popBackStack()
        }
    }
    
    private fun setupLogoutButton() {
        val appbarView = binding.appbar.root
        val btnLogout = appbarView.findViewById<View>(R.id.btnLogout)
        btnLogout?.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // FIRST: Logout from auth (this needs the SupabaseClient)
                    authViewModel.logout()
                    
                    // Wait a bit to ensure logout completes
                    kotlinx.coroutines.delay(100)
                    
                    // THEN: Stop realtime and clear ServiceLocator
                    ServiceLocator.provideOrderRepository(requireContext().applicationContext).stopRealtime()
                    ServiceLocator.clear()
                } catch (e: Exception) {
                    android.util.Log.e("PreviewOrderHeaderFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    try {
                        ServiceLocator.clear()
                    } catch (clearError: Exception) {
                        android.util.Log.e("PreviewOrderHeaderFragment", "Error clearing ServiceLocator", clearError)
                    }
                }
            }
            // MainActivity will handle navigation when currentUser becomes null
        }
    }
    
    private fun setupStepper() {
        val stepperView = binding.stepper.root
        val v1 = stepperView.findViewById<TextView>(R.id.v1)
        val v2 = stepperView.findViewById<TextView>(R.id.v2)
        val v3 = stepperView.findViewById<TextView>(R.id.v3)
        
        // Update stepper values
        v1?.text = orderType.toDisplayString()
        v2?.text = "No $tableNumber"
        
        // Update v3 with item count
        draftViewModel.itemCount.observe(viewLifecycleOwner) { count ->
            v3?.text = count.toString()
        }
        
        // Set dots to active (d1, d2, d3, d4 active, d5 inactive)
        val d1 = stepperView.findViewById<View>(R.id.d1)
        val d2 = stepperView.findViewById<View>(R.id.d2)
        val d3 = stepperView.findViewById<View>(R.id.d3)
        val d4 = stepperView.findViewById<View>(R.id.d4)
        val d5 = stepperView.findViewById<View>(R.id.d5)
        
        d1?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        d2?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        d3?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        d4?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        d5?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_inactive)
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
                android.util.Log.d("PreviewOrderHeaderFragment", "Alter item: ${item.nameSnapshot}")
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
                    android.util.Log.d("PreviewOrderHeaderFragment", "Alter item: ${item.nameSnapshot}")
                },
                onRemoveClick = { item ->
                    draftViewModel.removeDraftItem(item.id)
                }
            )
            binding.rvPreview.adapter = adapter
        }
    }
    
    private fun navigateToCompactView() {
        val bundle = Bundle().apply {
            putInt("tableNumber", tableNumber)
            putString("orderType", orderType.name)
        }
        findNavController().navigate(R.id.previewOrderFragment, bundle)
    }
    
    private fun navigateToItemSelection() {
        // Navigate back to item selection (Screen 3/4)
        findNavController().popBackStack(R.id.orderItemFragment, false)
    }
    
    private fun placeOrder() {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) {
            // Show error message
            android.util.Log.e("PreviewOrderHeaderFragment", "Cannot place order: no items")
            return
        }
        
        // TODO: Implement order placement via Supabase
        // Similar to admin placeDraft function
        // For now, just log and clear draft
        android.util.Log.d("PreviewOrderHeaderFragment", "Placing order with ${items.size} items")
        
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

