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
    private var isEditing: Boolean = false
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
        isEditing = navArgs.isEditing
        
        // Add defensive check for EAT_AWAY - recover from saved state if needed
        if (orderType == OrderType.EAT_AWAY && licensePlate.isNullOrBlank()) {
            android.util.Log.w("PreviewOrderFragment", "EAT_AWAY order missing license plate, attempting to recover from saved state")
            licensePlate = savedInstanceState?.getString("licensePlate")
        }
        
        android.util.Log.d("PreviewOrderFragment", "onViewCreated: orderType=$orderType, licensePlate='$licensePlate', tableNumber=$tableNumber, existingOrderId=$existingOrderId, isEditing=$isEditing")
        
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
                // If editing existing order, navigate to item selection with existing order context
                val nextArgs = navArgs.copy(
                    existingOrderId = existingOrderId,
                    orderType = orderType,
                    tableNumber = tableNumber,
                    licensePlate = licensePlate,
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
            adapter = PreviewOrderAdapter(
                items = items,
                onQuantityDecrease = { item ->
                    if (isEditing) {
                        // In edit mode, update order item directly
                        val newQuantity = (item.qty - 1).coerceAtLeast(1)
                        lifecycleScope.launch {
                            ordersViewModel.updateOrderItem(item.id, quantity = newQuantity)
                        }
                    } else {
                        // In draft mode, update draft
                        draftViewModel.updateDraftItem(item.id) { currentItem ->
                            currentItem.copy(qty = (currentItem.qty - 1).coerceAtLeast(1))
                        }
                    }
                },
                onQuantityIncrease = { item ->
                    if (isEditing) {
                        // In edit mode, update order item directly
                        val newQuantity = (item.qty + 1).coerceAtMost(99)
                        lifecycleScope.launch {
                            ordersViewModel.updateOrderItem(item.id, quantity = newQuantity)
                        }
                    } else {
                        // In draft mode, update draft
                        draftViewModel.updateDraftItem(item.id) { currentItem ->
                            currentItem.copy(qty = (currentItem.qty + 1).coerceAtMost(99))
                        }
                    }
                },
                onAlterClick = { item ->
                    if (isEditing) {
                        // In edit mode, open customization and on save, call updateOrderItem
                        openCustomizationForEditInOrder(item)
                    } else {
                        // In draft mode, open customization normally
                        openCustomizationForEdit(item)
                    }
                },
                onRemoveClick = { item ->
                    if (isEditing) {
                        // In edit mode, delete order item with confirmation
                        showDeleteItemConfirmation(item)
                    } else {
                        // In draft mode, remove from draft
                        draftViewModel.removeDraftItem(item.id)
                    }
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
                    baseOrderItems = order.items
                    
                    // Only reload if the order actually changed (items count or IDs changed)
                    // This prevents unnecessary reloads when the same order is emitted again
                    val currentItemIds = order.items.map { it.id }.toSet()
                    if (order.items.size != previousItemCount || 
                        currentItemIds != previousItemIds) {
                        android.util.Log.d("PreviewOrderFragment", "Order updated: ${order.items.size} items (was $previousItemCount)")
                        draftViewModel.loadOrderItems(order.items)
                    }
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
            isEditing = isEditing,
            showHeader = true
        )
        findNavController().navigate(R.id.previewOrderHeaderFragment, nextArgs.toBundle())
    }
    
    private fun performAlterOrder(items: List<OrderItem>, chefTip: String? = null) {
        val orderId = existingOrderId ?: return
        
        // Disable UI during network call
        binding.placeOrderBar.root.isEnabled = false
        binding.placeOrderBar.root.alpha = 0.5f
        
        viewLifecycleOwner.lifecycleScope.launch {
            ordersViewModel.alterOrder(orderId, items, chefTip)
            
            // Observe success/error
            ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    ordersViewModel.clearSuccessMessage()
                    findNavController().popBackStack()
                }
            }
            
            ordersViewModel.error.observe(viewLifecycleOwner) { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    ordersViewModel.clearError()
                    // Re-enable UI on error
                    binding.placeOrderBar.root.isEnabled = true
                    binding.placeOrderBar.root.alpha = 1f
                }
            }
        }
    }
    
    private fun placeOrder() {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one item to the order", Toast.LENGTH_SHORT).show()
            return
        }
        
        val effectiveOrderType = orderType
        val effectiveLicense = when (effectiveOrderType) {
            OrderType.EAT_AWAY -> licensePlate?.filter { it.isDigit() }?.take(4)
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

        lifecycleScope.launch {
            when {
                // Case 1: Editing existing order (isEditing = true)
                isEditing && existingOrderId != null -> {
                    val order = currentOrder
                    if (order == null) {
                        Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show()
                        binding.placeOrderBar.root.isEnabled = true
                        binding.placeOrderBar.root.alpha = 1f
                        return@launch
                    }
                    
                    // Check status and show warning if needed
                    if (OrderModificationDialog.requiresWarning(order.status)) {
                        OrderModificationDialog.showPreparingOrderConfirmation(
                            requireContext(),
                            onConfirm = { performAlterOrder(items, order.chefTip) }
                        )
                    } else {
                        performAlterOrder(items, order.chefTip)
                    }
                }
                
                // Case 2: Adding items to existing order (existingOrderId != null, isEditing = false)
                existingOrderId != null && !isEditing -> {
                    // Use addItemsToOrder which will handle status check internally
                    // It will update existing order if Created/Accepted, or create child order if InKitchen/Prepared/Delivered
                    ordersViewModel.addItemsToOrder(existingOrderId!!, items)
                }
                
                // Case 3: Creating new order - but first check if order already exists for DINE_IN
                else -> {
                    // For DINE_IN orders, double-check if an order already exists for this table
                    // This handles the race condition where editableOrders wasn't loaded yet in OrderItemFragment
                    if (effectiveOrderType == OrderType.DINE_IN && tableNumber != null) {
                        val editableOrders = ordersViewModel.editableOrders.value
                        val existingOrder = editableOrders?.find { order ->
                            order.type == OrderType.DINE_IN && 
                            order.tableNumber == tableNumber &&
                            order.status in listOf(
                                OrderStatus.CREATED,
                                OrderStatus.ACCEPTED,
                                OrderStatus.IN_KITCHEN,
                                OrderStatus.PREPARED
                            )
                        }
                        
                        if (existingOrder != null) {
                            // Found existing order - add items to it instead of creating new order
                            android.util.Log.d("PreviewOrderFragment", "Found existing order ${existingOrder.id} for table $tableNumber when trying to create. Adding items instead.")
                            ordersViewModel.addItemsToOrder(existingOrder.id, items)
                            return@launch
                        }
                    }
                    
                    // No existing order found - proceed with creating new order
                    ordersViewModel.createOrder(
                        orderType = effectiveOrderType,
                        items = items,
                        tableNumber = if (effectiveOrderType == OrderType.DINE_IN) tableNumber else null,
                        licensePlate = effectiveLicense,
                        chefTip = ""
                    )
                }
            }
        }
        
        // Observe success/error for all operations
        ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                ordersViewModel.clearSuccessMessage()
                draftViewModel.clearDraft()
                
                // Navigate based on operation type
                when {
                    isEditing && existingOrderId != null -> {
                        // After editing, go back
                        findNavController().popBackStack()
                    }
                    existingOrderId != null && !isEditing -> {
                        // After adding items, go back to given orders
                        findNavController().popBackStack(R.id.givenOrderFragment, false)
                    }
                    else -> {
                        // After creating new order, go back to order type selection
                        findNavController().popBackStack(R.id.orderTypeFragment, false)
                    }
                }
            }
        }
        
        ordersViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
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

