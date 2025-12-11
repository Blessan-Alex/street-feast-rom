package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.PreviewOrderAdapter
import com.streatfeast.app.databinding.FragmentPreviewOrderBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.navigation.OrderEditMode
import com.streatfeast.app.navigation.OrderNavArgs
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.dialogs.OrderModificationDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast

class PreviewOrderFragment : Fragment() {
    
    private var _binding: FragmentPreviewOrderBinding? = null
    private val binding get() = _binding!!
    
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
        _binding = FragmentPreviewOrderBinding.inflate(inflater, container, false)
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

        // Defensive: ADD_ITEMS must have an existing order id; otherwise bail out
        if (editMode == OrderEditMode.ADD_ITEMS && existingOrderId == null) {
            android.util.Log.w("PreviewOrderFragment", "ADD_ITEMS without existingOrderId; navigating back")
            findNavController().popBackStack()
            return
        }
        
        // Add defensive check for EAT_AWAY - recover from saved state if needed
        if (orderType == OrderType.EAT_AWAY && licensePlate.isNullOrBlank()) {
            android.util.Log.w("PreviewOrderFragment", "EAT_AWAY order missing license plate, attempting to recover from saved state")
            licensePlate = savedInstanceState?.getString("licensePlate")
        }
        
        android.util.Log.d("PreviewOrderFragment", "onViewCreated: orderType=$orderType, licensePlate='$licensePlate', tableNumber=$tableNumber, existingOrderId=$existingOrderId, editMode=$editMode")
        
        // Load existing order items if editing existing order or adding items
        if (existingOrderId != null) {
            loadExistingOrderItems()
        }
        
        setupTableHandle()
        setupAddButton()
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
                android.util.Log.d("PreviewOrderFragment", "EAT_AWAY button state: enabled=${canPlaceOrder()}, licensePlate='$licensePlate', items=${draftViewModel.getDraftItems().size}")
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        licensePlate?.let { outState.putString("licensePlate", it) }
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
        // Handle "+" button click - navigate to item selection
        binding.btnAdd.setOnClickListener {
            if (existingOrderId != null) {
                // If editing/adding items to existing order, preserve context and mode
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
                // Navigate back to item selection (Screen 3/4 or Screen 5)
                findNavController().popBackStack(R.id.orderItemFragment, false)
            }
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
            // Keep base order items visible in ADD_ITEMS even if draft is empty on return
            if (editMode == OrderEditMode.ADD_ITEMS &&
                baseOrderItems.isNotEmpty() &&
                items.isEmpty()
            ) {
                draftViewModel.loadOrderItems(baseOrderItems)
                return@observe
            }

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
                    // Always edit via draft and finalize through alter/add-on
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
        // Create a MenuItem from OrderItem for the customization sheet
        val menuItem = MenuItem(
            id = orderItem.itemId,
            name = orderItem.nameSnapshot,
            sizes = listOf("Small", "medium", "Large"),
            vegFlag = orderItem.vegFlagSnapshot
        )
        
        val bundle = Bundle().apply {
            putParcelable("menuItem", menuItem)
            putParcelable("orderItem", orderItem)
            putBoolean("isEditingOrder", true)  // Flag to indicate we're editing an order item
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
    
    private fun loadExistingOrderItems() {
        existingOrderId?.let { orderId ->
            ordersViewModel.editableOrders.observe(viewLifecycleOwner) { orders ->
                val order = orders.find { it.id == orderId }
                if (order != null) {
                    val previousItemCount = currentOrder?.items?.size ?: 0
                    val previousItemIds = currentOrder?.items?.map { it.id }?.toSet() ?: emptySet()
                    currentOrder = order
                    // Always set baseOrderItems to ensure it's available when placing order
                    baseOrderItems = order.items
                    
                    val currentItemIds = order.items.map { it.id }.toSet()
                    val orderChanged = order.items.size != previousItemCount || currentItemIds != previousItemIds
                    
                    if (editMode == OrderEditMode.EDIT) {
                        // Always load order items into draft in EDIT mode
                        // Check if draft is empty or if order changed to avoid unnecessary reloads
                        val draftItems = draftViewModel.getDraftItems()
                        val draftIsEmpty = draftItems.isEmpty()
                        val shouldLoad = draftIsEmpty || orderChanged
                        
                        if (shouldLoad) {
                            android.util.Log.d("PreviewOrderFragment", "Loading order items into draft: ${order.items.size} items (draft was empty: $draftIsEmpty, order changed: $orderChanged)")
                            draftViewModel.loadOrderItems(order.items)
                        }
                    } else if (editMode == OrderEditMode.ADD_ITEMS) {
                        // Keep draft focused on newly added items; clear only on first load
                        if (previousItemCount == 0) {
                            draftViewModel.clearDraft()
                        }
                    }
                } else {
                    // Log warning if order not found in editableOrders
                    android.util.Log.w("PreviewOrderFragment", "Order $orderId not found in editableOrders")
                    // Try refreshing to see if order appears
                    ordersViewModel.refresh()
                }
            }
        }
    }
    
    private fun navigateToHeaderView() {
        val nextArgs = navArgs.copy(
            orderType = orderType,
            tableNumber = tableNumber,
            licensePlate = licensePlate,
            existingOrderId = existingOrderId,
            editMode = editMode,
            showHeader = true
        )
        findNavController().navigate(R.id.previewOrderHeaderFragment, nextArgs.toBundle())
    }
    
    private fun performAlterOrder(items: List<OrderItem>, chefTip: String? = null) {
        val orderId = existingOrderId ?: run {
            Toast.makeText(requireContext(), "Order ID is missing. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.alterOrder(orderId, items, chefTip)
        }
    }
    
    private fun placeOrder(args: OrderNavArgs = navArgs) {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one item to the order", Toast.LENGTH_SHORT).show()
            return
        }
        
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

        // Disable UI during network call
        binding.placeOrderBar.root.isEnabled = false
        binding.placeOrderBar.root.alpha = 0.5f

        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.placeOrderBar.root.isEnabled = !isLoading
            binding.placeOrderBar.root.alpha = if (isLoading) 0.5f else 1f
        }

        // Store effective mode for success handler (accessible outside launch block)
        var effectiveModeForSuccess: OrderEditMode? = null

        lifecycleScope.launch {
            // Resolve existing order inside coroutine (suspend call)
            var effectiveExistingOrderId = args.existingOrderId
            if (effectiveExistingOrderId == null &&
                effectiveOrderType == OrderType.DINE_IN &&
                tableNumber != null
            ) {
                val active = ordersViewModel.getActiveOrderForTable(tableNumber)
                effectiveExistingOrderId = active?.id
            }
            // Only auto-switch to EDIT if mode is NEW and no explicit existingOrderId was passed
            // If existingOrderId is explicitly passed with NEW mode (e.g., from "Add Items"),
            // keep NEW mode so createOrder handles cancel + create
            val effectiveMode = if (effectiveExistingOrderId != null && args.existingOrderId == null && args.editMode == OrderEditMode.NEW) {
                OrderEditMode.EDIT
            } else {
                args.editMode
            }
            val orderId = effectiveExistingOrderId

            // Store for success handler
            effectiveModeForSuccess = effectiveMode

            android.util.Log.d(
                "PreviewOrderFragment",
                "placeOrder mode=$effectiveMode, existingOrderId=$orderId, items=${items.size}, baseItems=${baseOrderItems.size}"
            )

            when (effectiveMode) {
                OrderEditMode.NEW -> {
                    // No existing order detected; proceed with create
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
                    // Should not happen with existingOrderId; stop to avoid partial paths.
                    Toast.makeText(requireContext(), "Add Items uses replace flow; retry", Toast.LENGTH_SHORT).show()
                    binding.placeOrderBar.root.isEnabled = true
                    binding.placeOrderBar.root.alpha = 1f
                    return@launch
                }
                OrderEditMode.EDIT -> {
                    // In EDIT mode, use draft items directly (they already contain all items)
                    // alterOrder will cancel the old order and create a new one atomically via alterOrderV2
                    if (orderId == null) {
                        Toast.makeText(requireContext(), "Order ID is missing. Please try again.", Toast.LENGTH_SHORT).show()
                        binding.placeOrderBar.root.isEnabled = true
                        binding.placeOrderBar.root.alpha = 1f
                        return@launch
                    }

                    // Try to get order status and chef tip if available (for warning dialog)
                    // But don't fail if order is not found - alterOrderV2 will handle that
                    var orderStatus: OrderStatus? = null
                    var chefTip = ""
                    
                    val order = currentOrder ?: ordersViewModel.editableOrders.value?.find { it.id == orderId }
                    if (order != null) {
                        orderStatus = order.status
                        chefTip = order.chefTip ?: ""
                        // Update currentOrder for future reference
                        currentOrder = order
                    } else {
                        // Order might not be in editableOrders (could be canceled already)
                        // Use empty chef tip and proceed - alterOrderV2 will handle errors appropriately
                        chefTip = ""
                    }

                    // Use alterOrder which internally calls alterOrderV2
                    // This will cancel the old order and create a new one atomically
                    // No need to validate order exists - alterOrderV2 will handle errors appropriately
                    if (orderStatus != null && OrderModificationDialog.requiresWarning(orderStatus)) {
                        OrderModificationDialog.showPreparingOrderConfirmation(
                            requireContext(),
                            onConfirm = { 
                                ordersViewModel.alterOrder(orderId, items, chefTip)
                            }
                        )
                    } else {
                        // cancel + recreate via alter flow (new order ID; avoids table occupancy)
                        ordersViewModel.alterOrder(orderId, items, chefTip)
                    }
                }
            }
        }
        
        // Observe success/error for all operations
        ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                ordersViewModel.clearSuccessMessage()

                // Use stored effective mode, fallback to args.editMode if not set yet
                val mode = effectiveModeForSuccess ?: args.editMode
                when (mode) {
                    OrderEditMode.NEW -> {
                        draftViewModel.clearDraft()
                        // If NEW mode has existingOrderId, it came from "Add Items" flow
                        // Navigate back to givenOrderFragment, otherwise to orderTypeFragment
                        if (args.existingOrderId != null) {
                            findNavController().popBackStack(R.id.givenOrderFragment, false)
                        } else {
                            findNavController().popBackStack(R.id.orderTypeFragment, false)
                        }
                    }
                    OrderEditMode.EDIT -> {
                        draftViewModel.clearDraft()
                        findNavController().popBackStack()
                    }
                    OrderEditMode.ADD_ITEMS -> {
                        // Should not happen; align behavior with EDIT for safety
                        draftViewModel.clearDraft()
                        findNavController().popBackStack()
                    }
                }
            }
        }
        
        ordersViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // If createOrder failed because the table is occupied, auto-fall back to add items
                if (editMode == OrderEditMode.NEW &&
                    tableNumber != null &&
                    it.contains("already occupied", ignoreCase = true)
                ) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val active = ordersViewModel.getActiveOrderForTable(tableNumber)
                        if (active != null) {
                            ordersViewModel.addItemsToOrder(active.id, draftViewModel.getDraftItems())
                        } else {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        }
                        ordersViewModel.clearError()
                        binding.placeOrderBar.root.isEnabled = true
                        binding.placeOrderBar.root.alpha = 1f
                    }
                    return@observe
                }

                // Default handling
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                ordersViewModel.clearError()
                binding.placeOrderBar.root.isEnabled = true
                binding.placeOrderBar.root.alpha = 1f
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

