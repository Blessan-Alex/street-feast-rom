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
    
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    private var licensePlate: String? = null
    private var existingOrderId: String? = null
    private var isEditing: Boolean = false
    private var currentOrder: Order? = null
    private var baseOrderItems: List<OrderItem> = emptyList()
    
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
        // Handle "+" button click - navigate to item selection
        binding.btnAdd.setOnClickListener {
            if (existingOrderId != null) {
                // If editing existing order, navigate to item selection with existing order context
                val bundle = Bundle().apply {
                    putString("existingOrderId", existingOrderId)
                    putString("orderType", orderType.name)
                    putInt("tableNumber", tableNumber)
                }
                findNavController().navigate(R.id.orderItemFragment, bundle)
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
                    currentOrder = order
                    baseOrderItems = order.items
                    draftViewModel.loadOrderItems(order.items)
                    // We only need to load once
                    ordersViewModel.editableOrders.removeObservers(viewLifecycleOwner)
                }
            }
        }
    }
    
    private fun navigateToHeaderView() {
        val bundle = Bundle().apply {
            putInt("tableNumber", tableNumber)
            putString("orderType", orderType.name)
            licensePlate?.let { putString("licensePlate", it) }
            existingOrderId?.let { putString("existingOrderId", it) }
            putBoolean("isEditing", isEditing)
        }
        findNavController().navigate(R.id.previewOrderHeaderFragment, bundle)
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
    
    private fun placeOrder() {
        val items = draftViewModel.getDraftItems()
        if (items.isEmpty()) {
            // Show error message
            android.util.Log.e("PreviewOrderFragment", "Cannot place order: no items")
            Toast.makeText(requireContext(), "Please add at least one item to the order", Toast.LENGTH_SHORT).show()
            return
        }
        val effectiveOrderType = orderType
        val effectiveLicense = when (effectiveOrderType) {
            OrderType.EAT_AWAY -> licensePlate
                ?.filter { it.isDigit() }
                ?.take(4)
            else -> null
        }

        if (effectiveOrderType == OrderType.EAT_AWAY &&
            (effectiveLicense.isNullOrEmpty() || !effectiveLicense.matches(Regex("^\\d{4}$")))
        ) {
            Toast.makeText(requireContext(), "Enter 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("PreviewOrderFragment", "Placing order type=$effectiveOrderType license=$effectiveLicense table=$tableNumber items=${items.size}")

        if (isEditing && existingOrderId != null) {
            // Edit mode: always alter existing order, even if only quantities changed
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
            return
        } else if (existingOrderId != null) {
            // Add items mode: Add items to existing order
            val baseIds = baseOrderItems.map { it.id }.toSet()
            val newItems = items.filter { it.id !in baseIds }
            android.util.Log.d("PreviewOrderFragment", "Adding ${newItems.size} items to existing order $existingOrderId (total draft ${items.size})")

            if (newItems.isEmpty()) {
                Toast.makeText(requireContext(), "No new items to add", Toast.LENGTH_SHORT).show()
                return
            }

            lifecycleScope.launch {
                ordersViewModel.addItemsToOrder(existingOrderId!!, newItems)
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
                    orderType = effectiveOrderType,
                    items = items,
                    tableNumber = if (effectiveOrderType == OrderType.DINE_IN) tableNumber else null,
                    licensePlate = effectiveLicense,
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

