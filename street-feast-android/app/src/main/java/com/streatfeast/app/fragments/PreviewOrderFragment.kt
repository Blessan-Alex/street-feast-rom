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
                    baseOrderItems = order.items
                    
                    val currentItemIds = order.items.map { it.id }.toSet()
                    val orderChanged = order.items.size != previousItemCount || currentItemIds != previousItemIds
                    
                    if (editMode == OrderEditMode.EDIT) {
                        if (orderChanged || previousItemCount == 0) {
                            android.util.Log.d("PreviewOrderFragment", "Order updated: ${order.items.size} items (was $previousItemCount)")
                            draftViewModel.loadOrderItems(order.items)
                        }
                    } else if (editMode == OrderEditMode.ADD_ITEMS) {
                        // Keep draft focused on newly added items; clear only on first load
                        if (previousItemCount == 0) {
                            draftViewModel.clearDraft()
                        }
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
            editMode = editMode,
            showHeader = true
        )
        findNavController().navigate(R.id.previewOrderHeaderFragment, nextArgs.toBundle())
    }
    
    private fun performAlterOrder(items: List<OrderItem>, chefTip: String? = null) {
        val orderId = existingOrderId ?: return
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

        android.util.Log.d(
            "PreviewOrderFragment",
            "placeOrder mode=${args.editMode}, existingOrderId=${args.existingOrderId}, items=${items.size}, baseItems=${baseOrderItems.size}"
        )

        // Disable UI during network call
        binding.placeOrderBar.root.isEnabled = false
        binding.placeOrderBar.root.alpha = 0.5f

        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.placeOrderBar.root.isEnabled = !isLoading
            binding.placeOrderBar.root.alpha = if (isLoading) 0.5f else 1f
        }

        lifecycleScope.launch {
            when (args.editMode) {
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
                    val orderId = args.existingOrderId
                    if (orderId == null) {
                        Toast.makeText(requireContext(), "No base order to add items to", Toast.LENGTH_SHORT).show()
                        binding.placeOrderBar.root.isEnabled = true
                        binding.placeOrderBar.root.alpha = 1f
                        return@launch
                    }

                    // Only send newly added items; base items stay untouched
                    val newItems = items.filter { draftItem ->
                        baseOrderItems.none { it.id == draftItem.id }
                    }
                    if (newItems.isEmpty()) {
                        Toast.makeText(requireContext(), "Add at least one new item", Toast.LENGTH_SHORT).show()
                        binding.placeOrderBar.root.isEnabled = true
                        binding.placeOrderBar.root.alpha = 1f
                        return@launch
                    }

                    ordersViewModel.addItemsToOrder(orderId, newItems)
                }
                OrderEditMode.EDIT -> {
                    val orderId = args.existingOrderId
                    val order = currentOrder
                    if (orderId == null || order == null) {
                        Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show()
                        binding.placeOrderBar.root.isEnabled = true
                        binding.placeOrderBar.root.alpha = 1f
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
        
        // Observe success/error for all operations
        ordersViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                ordersViewModel.clearSuccessMessage()
                draftViewModel.clearDraft()
                
                // Navigate based on operation type
                when (args.editMode) {
                    OrderEditMode.EDIT -> findNavController().popBackStack()
                    OrderEditMode.ADD_ITEMS -> findNavController().popBackStack(R.id.givenOrderFragment, false)
                    OrderEditMode.NEW -> findNavController().popBackStack(R.id.orderTypeFragment, false)
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

