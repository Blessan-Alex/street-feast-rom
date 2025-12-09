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
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.PreviewOrderAdapter
import com.streatfeast.app.databinding.FragmentPreviewOrderHeaderBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.dialogs.OrderModificationDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreviewOrderHeaderFragment : Fragment() {
    
    private var _binding: FragmentPreviewOrderHeaderBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    private val ordersViewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    private var licensePlate: String? = null
    private var existingOrderId: String? = null
    private var isEditing: Boolean = false
    private var currentOrder: Order? = null
    
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
            licensePlate = args.getString("licensePlate")
            args.getString("orderType")?.let { typeString ->
                orderType = try {
                    OrderType.valueOf(typeString)
                } catch (e: IllegalArgumentException) {
                    OrderType.DINE_IN
                }
            }
            existingOrderId = args.getString("existingOrderId")
            isEditing = args.getBoolean("isEditing", false)
        }
        
        // Load existing order items if editing existing order or adding items
        if (existingOrderId != null) {
            loadExistingOrderItems()
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
        // btnClose removed from app bar
        // Users can use system back button instead
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
                // Navigate to customization with existing OrderItem
                openCustomizationForEdit(item)
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
        
        // Update button text based on edit mode
        val placeOrderText = binding.placeOrderBar.root.findViewById<android.widget.TextView>(R.id.tvPlaceOrder)
        if (isEditing) {
            placeOrderText?.text = "Save Changes"
        } else {
            placeOrderText?.text = "Place order"
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
                    if (isEditing) {
                        val newQuantity = (item.qty - 1).coerceAtLeast(1)
                        lifecycleScope.launch {
                            ordersViewModel.updateOrderItem(item.id, quantity = newQuantity)
                        }
                    } else {
                        draftViewModel.updateDraftItem(item.id) { currentItem ->
                            currentItem.copy(qty = (currentItem.qty - 1).coerceAtLeast(1))
                        }
                    }
                },
                onQuantityIncrease = { item ->
                    if (isEditing) {
                        val newQuantity = (item.qty + 1).coerceAtMost(99)
                        lifecycleScope.launch {
                            ordersViewModel.updateOrderItem(item.id, quantity = newQuantity)
                        }
                    } else {
                        draftViewModel.updateDraftItem(item.id) { currentItem ->
                            currentItem.copy(qty = (currentItem.qty + 1).coerceAtMost(99))
                        }
                    }
                },
                onAlterClick = { item ->
                    if (isEditing) {
                        openCustomizationForEditInOrder(item)
                    } else {
                        openCustomizationForEdit(item)
                    }
                },
                onRemoveClick = { item ->
                    if (isEditing) {
                        showDeleteItemConfirmation(item)
                    } else {
                        draftViewModel.removeDraftItem(item.id)
                    }
                }
            )
            binding.rvPreview.adapter = adapter
        }
    }
    
    private fun openCustomizationForEditInOrder(orderItem: OrderItem) {
        val menuItem = MenuItem(
            id = orderItem.itemId,
            name = orderItem.nameSnapshot,
            sizes = listOf("Small", "medium", "Large"),
            vegFlag = orderItem.vegFlagSnapshot
        )
        
        val bundle = Bundle().apply {
            putParcelable("menuItem", menuItem)
            putParcelable("orderItem", orderItem)
            putBoolean("isEditingOrder", true)
        }
        findNavController().navigate(R.id.itemCustomizeFragment, bundle)
    }
    
    private fun showDeleteItemConfirmation(item: OrderItem) {
        OrderModificationDialog.showDeleteItemConfirmation(
            requireContext(),
            itemName = item.nameSnapshot,
            onConfirm = {
                lifecycleScope.launch {
                    ordersViewModel.deleteOrderItem(item.id)
                }
            }
        )
    }
    
    private fun loadExistingOrderItems() {
        existingOrderId?.let { orderId ->
            // Observe delivered orders to find the existing order
            ordersViewModel.deliveredOrders.observe(viewLifecycleOwner) { orders ->
                val order = orders.find { it.id == orderId }
                order?.let {
                    currentOrder = it
                    if (isEditing) {
                        // In edit mode, load all existing items for editing
                        draftViewModel.loadOrderItems(it.items)
                    } else {
                        // In add items mode, start with empty draft (user adds new items)
                        draftViewModel.clearDraft()
                    }
                }
            }
        }
    }
    
    private fun navigateToCompactView() {
        val bundle = Bundle().apply {
            putInt("tableNumber", tableNumber)
            putString("orderType", orderType.name)
            licensePlate?.let { putString("licensePlate", it) }
            existingOrderId?.let { putString("existingOrderId", it) }
            putBoolean("isEditing", isEditing)
        }
        findNavController().navigate(R.id.previewOrderFragment, bundle)
    }
    
    private fun performAlterOrder(items: List<OrderItem>, chefTip: String? = null) {
        val orderId = existingOrderId ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.alterOrder(orderId, items, chefTip)
            
            // Navigate back or show success
            Toast.makeText(requireContext(), "Order updated successfully", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }
    
    private fun openCustomizationForEdit(orderItem: OrderItem) {
        // Create a MenuItem from OrderItem for the customization sheet
        // You may need to fetch the actual MenuItem from your data source
        // For now, creating a MenuItem with common sizes
        val menuItem = MenuItem(
            id = orderItem.itemId,
            name = orderItem.nameSnapshot,
            sizes = listOf("Small", "medium", "Large"),
            vegFlag = orderItem.vegFlagSnapshot
        )
        
        val bundle = Bundle().apply {
            putParcelable("menuItem", menuItem)
            putParcelable("orderItem", orderItem) // Pass the existing OrderItem for edit mode
        }
        findNavController().navigate(R.id.itemCustomizeFragment, bundle)
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
            Toast.makeText(requireContext(), "Please add at least one item to the order", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isEditing && existingOrderId != null) {
            // Edit mode: Alter existing order
            val order = currentOrder
            if (order == null) {
                Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Check status and show warning if needed
            val status = order.status
            if (OrderModificationDialog.requiresWarning(status)) {
                // Show confirmation dialog
                OrderModificationDialog.showPreparingOrderConfirmation(
                    requireContext(),
                    onConfirm = { performAlterOrder(items, order.chefTip) }
                )
            } else {
                // Directly alter for Created/Accepted orders
                performAlterOrder(items, order.chefTip)
            }
        } else if (existingOrderId != null) {
            // Add items mode: Add items to existing order
            android.util.Log.d("PreviewOrderHeaderFragment", "Adding ${items.size} items to existing order $existingOrderId")
            lifecycleScope.launch {
                ordersViewModel.addItemsToOrder(existingOrderId!!, items)
            }
            
            // Observe success/error
            ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    ordersViewModel.clearSuccessMessage()
                    draftViewModel.clearDraft()
                    findNavController().popBackStack(R.id.givenOrderFragment, false)
                }
            }
            
            ordersViewModel.error.observe(viewLifecycleOwner) { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    ordersViewModel.clearError()
                }
            }
        } else {
            // Create new order
            lifecycleScope.launch {
                ordersViewModel.createOrder(
                    orderType = orderType,
                    items = items,
                    tableNumber = if (orderType == OrderType.DINE_IN) tableNumber else null,
                    licensePlate = if (orderType == OrderType.EAT_AWAY) licensePlate else null,
                    chefTip = "" // TODO: Get chef tip from UI if needed
                )
            }
            
            // Observe success/error
            ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    ordersViewModel.clearSuccessMessage()
                    draftViewModel.clearDraft()
                    findNavController().popBackStack(R.id.orderTypeFragment, false)
                }
            }
            
            ordersViewModel.error.observe(viewLifecycleOwner) { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    ordersViewModel.clearError()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

