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
import com.streatfeast.app.navigation.OrderEditMode
import com.streatfeast.app.navigation.OrderNavArgs
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
    
    private var navArgs: OrderNavArgs = OrderNavArgs()
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    private var licensePlate: String? = null
    private var existingOrderId: String? = null
    private var editMode: OrderEditMode = OrderEditMode.NEW
    private var currentOrder: Order? = null
    private var baseOrderItems: List<OrderItem> = emptyList()
    
    private fun isLicenseValid(): Boolean {
        return licensePlate?.filter { it.isDigit() }?.length == 4
    }
    
    private fun canPlaceOrder(): Boolean {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) return false
        return when(orderType) {
            OrderType.DINE_IN -> tableNumber in 1..7
            OrderType.EAT_AWAY -> licensePlate?.filter { it.isDigit() }?.length == 4
            OrderType.PARCEL -> true
        }
    }
    
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
        
        navArgs = OrderNavArgs.from(arguments)
        orderType = navArgs.orderType
        tableNumber = navArgs.tableNumber ?: navArgs.effectiveTableNumber()
        licensePlate = navArgs.licensePlate
        existingOrderId = navArgs.existingOrderId
        editMode = navArgs.editMode
        
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
        
        // For EAT_AWAY, ensure button state is updated when license plate is set
        // Use post to ensure view is fully ready and binding is accessible
        view?.post {
            updatePlaceOrderButtonState()
            // Log for debugging
            if (orderType == OrderType.EAT_AWAY) {
                android.util.Log.d("PreviewOrderHeaderFragment", "EAT_AWAY button state: enabled=${canPlaceOrder()}, licensePlate='$licensePlate', items=${draftViewModel.getDraftItems().size}")
            }
        }
        
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
    
    private fun updatePlaceOrderButtonState() {
        val enabled = canPlaceOrder()
        binding.placeOrderBar.root.isEnabled = enabled
        binding.placeOrderBar.root.alpha = if (enabled) 1f else 0.5f
    }
    
    private fun setupPlaceOrderBar() {
        // Update button state based on canPlaceOrder()
        fun updatePlaceOrderButton() {
            updatePlaceOrderButtonState()
            
            // Show/hide place order bar based on item count
            val count = draftViewModel.getDraftItems().size
            if (count > 0) {
                binding.placeOrderBar.root.visibility = View.VISIBLE
            } else {
                binding.placeOrderBar.root.visibility = View.GONE
            }
        }
        
        // Observe item count and update badge
        draftViewModel.itemCount.observe(viewLifecycleOwner) { count ->
            val badgeCount = binding.placeOrderBar.root.findViewById<TextView>(R.id.tvBadgeCount)
            badgeCount?.text = count.toString()
            
            // Update button state when items change
            updatePlaceOrderButton()
        }
        
        // Update button text based on edit mode
        val placeOrderText = binding.placeOrderBar.root.findViewById<android.widget.TextView>(R.id.tvPlaceOrder)
        placeOrderText?.text = if (editMode == OrderEditMode.EDIT) "Save Changes" else "Place order"
        
        // Initial state
        updatePlaceOrderButton()
        
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
            val displayItems = when (editMode) {
                OrderEditMode.ADD_ITEMS -> {
                    val newItems = items.filter { draftItem ->
                        baseOrderItems.none { it.id == draftItem.id }
                    }
                    baseOrderItems + newItems
                }
                else -> items
            }

            adapter = PreviewOrderAdapter(
                items = displayItems,
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
                    openCustomizationForEdit(item)
                },
                onRemoveClick = { item ->
                    draftViewModel.removeDraftItem(item.id)
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
            // Observe editable orders to get real-time updates when order changes
            ordersViewModel.editableOrders.observe(viewLifecycleOwner) { orders ->
                val order = orders.find { it.id == orderId }
                order?.let {
                    val previousItemCount = currentOrder?.items?.size ?: 0
                    val previousItemIds = currentOrder?.items?.map { it.id }?.toSet() ?: emptySet()
                    currentOrder = it
                    baseOrderItems = it.items
                    
                    // Check if order actually changed
                    val currentItemIds = it.items.map { it.id }.toSet()
                    val orderChanged = it.items.size != previousItemCount || currentItemIds != previousItemIds
                    
                    if (editMode == OrderEditMode.EDIT) {
                        if (orderChanged || previousItemCount == 0) {
                            android.util.Log.d("PreviewOrderHeaderFragment", "Order updated in edit mode: ${it.items.size} items (was $previousItemCount)")
                            draftViewModel.loadOrderItems(it.items)
                        }
                    } else {
                        // In add items mode, keep draft focused on new additions
                        if (previousItemCount == 0) {
                            draftViewModel.clearDraft()
                        }
                    }
                }
            }
        }
    }
    
    private fun navigateToCompactView() {
        val nextArgs = navArgs.copy(
            orderType = orderType,
            tableNumber = tableNumber,
            licensePlate = licensePlate,
            existingOrderId = existingOrderId,
            editMode = editMode,
            showHeader = false
        )
        findNavController().navigate(R.id.previewOrderFragment, nextArgs.toBundle())
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
        if (existingOrderId != null) {
            // Preserve context for add/alter flows
            val nextArgs = navArgs.copy(
                existingOrderId = existingOrderId,
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = licensePlate,
                editMode = editMode, // keep ADD_ITEMS or EDIT
                showHeader = false
            )
            findNavController().navigate(R.id.orderItemFragment, nextArgs.toBundle())
        } else {
            // Navigate back to item selection (Screen 3/4)
            findNavController().popBackStack(R.id.orderItemFragment, false)
        }
    }
    
    private fun placeOrder() {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) {
            android.util.Log.e("PreviewOrderHeaderFragment", "Cannot place order: no items")
            Toast.makeText(requireContext(), "Please add at least one item to the order", Toast.LENGTH_SHORT).show()
            return
        }

        val args = navArgs
        val effectiveOrderType = args.orderType
        val effectiveLicense = when (effectiveOrderType) {
            OrderType.EAT_AWAY -> args.licensePlate?.filter { it.isDigit() }?.take(4)
            else -> null
        }

        if (effectiveOrderType == OrderType.EAT_AWAY &&
            (effectiveLicense.isNullOrEmpty() || !effectiveLicense.matches(Regex("^\\d{4}$")))
        ) {
            Toast.makeText(requireContext(), "Enter 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d(
            "PreviewOrderHeaderFragment",
            "placeOrder mode=$editMode, existingOrderId=$existingOrderId, items=${items.size}, baseItems=${baseOrderItems.size}"
        )

        lifecycleScope.launch {
            when (editMode) {
                OrderEditMode.NEW -> {
                    ordersViewModel.createOrder(
                        orderType = effectiveOrderType,
                        items = items,
                        tableNumber = if (effectiveOrderType == OrderType.DINE_IN) tableNumber else null,
                        licensePlate = effectiveLicense,
                        chefTip = "",
                        isEdit = false
                    )
                }
                OrderEditMode.ADD_ITEMS -> {
                    val orderId = existingOrderId
                    if (orderId == null) {
                        Toast.makeText(requireContext(), "No base order to add items to", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val newItems = items.filter { draftItem ->
                        baseOrderItems.none { it.id == draftItem.id }
                    }
                    if (newItems.isEmpty()) {
                        Toast.makeText(requireContext(), "Add at least one new item", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    ordersViewModel.addItemsToOrder(orderId, newItems)
                }
                OrderEditMode.EDIT -> {
                    val order = currentOrder
                    val orderId = existingOrderId
                    if (order == null || orderId == null) {
                        Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    if (OrderModificationDialog.requiresWarning(order.status)) {
                        OrderModificationDialog.showPreparingOrderConfirmation(
                            requireContext(),
                            onConfirm = { performAlterOrder(items, order.chefTip) }
                        )
                    } else {
                        performAlterOrder(items, order.chefTip)
                    }
                }
            }
        }

        ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                ordersViewModel.clearSuccessMessage()
                draftViewModel.clearDraft()
                when (editMode) {
                    OrderEditMode.EDIT -> findNavController().popBackStack()
                    OrderEditMode.ADD_ITEMS -> findNavController().popBackStack(R.id.givenOrderFragment, false)
                    OrderEditMode.NEW -> {
                        // If NEW mode has existingOrderId, it came from "Add Items" flow
                        // Navigate back to givenOrderFragment, otherwise to orderTypeFragment
                        if (existingOrderId != null) {
                            findNavController().popBackStack(R.id.givenOrderFragment, false)
                        } else {
                            findNavController().popBackStack(R.id.orderTypeFragment, false)
                        }
                    }
                }
            }
        }

        ordersViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                ordersViewModel.clearError()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

