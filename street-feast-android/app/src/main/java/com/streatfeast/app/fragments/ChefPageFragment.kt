package com.streatfeast.app.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.fragment.findNavController
import com.streatfeast.app.databinding.ChefPageBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.utils.DateTimeUtils
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Helper data class for item view configuration
private data class ItemViewConfig(
    val itemView: LinearLayout,
    val nameView: TextView,
    val qtyView: TextView,
    val sizeView: TextView,
    val tipsView: TextView,
    val switchView: androidx.appcompat.widget.SwitchCompat,
    val colorBar: View
)

@RequiresApi(Build.VERSION_CODES.O)
class ChefPageFragment : Fragment() {
    
    private var _binding: ChefPageBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    
    private var currentOrder: Order? = null
    private val itemPreparedStates = mutableMapOf<String, Boolean>() // itemId -> isPrepared
    private val tabViews = mutableMapOf<String, LinearLayout>() // orderId -> tab view
    private var selectedTabOrderId: String? = null
    
    // Combine new orders and preparing orders into single list
    // Initialize in onViewCreated after fragment is attached
    private lateinit var combinedOrders: MediatorLiveData<List<Order>>
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChefPageBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize combinedOrders here (after fragment is attached)
        combinedOrders = MediatorLiveData<List<Order>>().apply {
            var newOrdersList = emptyList<Order>()
            var preparingOrdersList = emptyList<Order>()
            
            fun update() {
                // Combine and remove duplicates
                val allOrders = (newOrdersList + preparingOrdersList)
                    .distinctBy { it.id }
                    .sortedWith(
                        compareByDescending<Order> {
                            it.orderNumber.takeIf { num -> num > 0 } ?: Int.MIN_VALUE
                        }.thenByDescending {
                            it.createdAt
                        }
                    )
                value = allOrders
            }
            
            addSource(viewModel.newOrders) { orders ->
                newOrdersList = orders
                update()
            }
            
            addSource(viewModel.preparingOrders) { orders ->
                preparingOrdersList = orders
                update()
            }
        }
        
        setupCloseButton()
        setupDate()
        setupTabs()
        setupOrderCard()
        setupBottomButtons()
        observeOrders()
        observeMessages()
        
        viewModel.refresh()
    }
    
    private fun setupCloseButton() {
        // Hide close button since chef page is now the main page
        binding.btnClose.visibility = View.GONE
    }
    
    private fun setupDate() {
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())
    }
    
    private fun setupTabs() {
        combinedOrders.observe(viewLifecycleOwner) { orders ->
            val currentBinding = _binding ?: return@observe
            
            // Orders are already sorted by combinedOrders
            val sorted = orders
            
            // Clear existing tabs (except the template ones)
            val tabsContainer = currentBinding.tabsContainer
            tabsContainer.removeAllViews()
            tabViews.clear()
            
            // Create tabs for each order
            sorted.forEach { order ->
                val tabView = createTabView(order)
                tabsContainer.addView(tabView)
                tabViews[order.id] = tabView
            }
            
            // Select first order if available
            if (sorted.isNotEmpty() && selectedTabOrderId == null) {
                selectTab(sorted.first())
            } else if (selectedTabOrderId != null) {
                // Try to maintain selection
                sorted.find { it.id == selectedTabOrderId }?.let {
                    selectTab(it)
                } ?: run {
                    if (sorted.isNotEmpty()) {
                        selectTab(sorted.first())
                    }
                }
            }
            
            // Show/hide tabs container and order card
            if (sorted.isEmpty()) {
                currentBinding.tabsScroll.visibility = View.GONE
                currentBinding.orderCard.visibility = View.GONE
                // Show empty state message in card area
                showEmptyState()
            } else {
                currentBinding.tabsScroll.visibility = View.VISIBLE
                currentBinding.orderCard.visibility = View.VISIBLE
                hideEmptyState()
            }
        }
    }
    
    private fun createTabView(order: Order): LinearLayout {
        val context = requireContext()
        val tabView = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (44 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (10 * resources.displayMetrics.density).toInt()
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(
                (14 * resources.displayMetrics.density).toInt(),
                0,
                (14 * resources.displayMetrics.density).toInt(),
                0
            )
            // Set background based on order status
            val backgroundRes = when (order.status) {
                OrderStatus.CREATED -> {
                    // New order - use unselected style (will add visual indicator)
                    com.streatfeast.app.R.drawable.bg_tab_unselected
                }
                OrderStatus.IN_KITCHEN -> {
                    // Preparing order - normal style
                    com.streatfeast.app.R.drawable.bg_tab_unselected
                }
                else -> com.streatfeast.app.R.drawable.bg_tab_unselected
            }
            setBackgroundResource(backgroundRes)
            setOnClickListener { selectTab(order) }
        }
        
        // Icon
        val icon = android.widget.ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (22 * resources.displayMetrics.density).toInt(),
                (22 * resources.displayMetrics.density).toInt()
            )
            setImageResource(
                when (order.type) {
                    OrderType.DINE_IN -> com.streatfeast.app.R.drawable.ic_cutlery
                    OrderType.PARCEL -> com.streatfeast.app.R.drawable.ic_bag
                    else -> com.streatfeast.app.R.drawable.ic_cutlery
                }
            )
            // Use different color for new orders
            val iconColor = when (order.status) {
                OrderStatus.CREATED -> 0xFFFF9800.toInt() // Orange for new orders
                else -> 0xFF8D96AA.toInt() // Grey for preparing orders
            }
            setColorFilter(iconColor)
        }
        tabView.addView(icon)
        
        // Label
        val label = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (6 * resources.displayMetrics.density).toInt()
            }
            text = "Table ${formatTableNumber(order.orderNumber)}"
            // Use different color for new orders
            val textColor = when (order.status) {
                OrderStatus.CREATED -> 0xFFFF9800.toInt() // Orange for new orders
                else -> 0xFF8D96AA.toInt() // Grey for preparing orders
            }
            setTextColor(textColor)
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        tabView.addView(label)
        
        // Number
        val number = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (4 * resources.displayMetrics.density).toInt()
            }
            text = "#${order.orderNumber}"
            // Use different color for new orders
            val numberColor = when (order.status) {
                OrderStatus.CREATED -> 0xFFFF9800.toInt() // Orange for new orders
                else -> 0xFF8D96AA.toInt() // Grey for preparing orders
            }
            setTextColor(numberColor)
            textSize = 13f
        }
        tabView.addView(number)
        
        return tabView
    }
    
    private fun selectTab(order: Order) {
        val currentBinding = _binding ?: return
        
        selectedTabOrderId = order.id
        
        // Find the order for each tab to get its status
        val allOrders = combinedOrders.value ?: emptyList()
        
        // Update all tabs appearance
        tabViews.forEach { (orderId, tabView) ->
            val tabOrder = allOrders.find { it.id == orderId }
            if (orderId == order.id) {
                // Selected tab
                tabView.setBackgroundResource(com.streatfeast.app.R.drawable.bg_tab_selected)
                // Update text colors to white
                (0 until tabView.childCount).forEach { i ->
                    val child = tabView.getChildAt(i)
                    when (child) {
                        is TextView -> child.setTextColor(android.graphics.Color.WHITE)
                        is android.widget.ImageView -> child.setColorFilter(android.graphics.Color.WHITE)
                    }
                }
            } else {
                // Unselected tab - preserve status-based colors
                tabView.setBackgroundResource(com.streatfeast.app.R.drawable.bg_tab_unselected)
                val unselectedColor = when (tabOrder?.status) {
                    OrderStatus.CREATED -> 0xFFFF9800.toInt() // Orange for new orders
                    else -> 0xFF8D96AA.toInt() // Grey for preparing orders
                }
                val unselectedIconColor = when (tabOrder?.status) {
                    OrderStatus.CREATED -> 0xFFFF9800.toInt() // Orange for new orders
                    else -> 0xFFC3CADB.toInt() // Grey for preparing orders
                }
                (0 until tabView.childCount).forEach { i ->
                    val child = tabView.getChildAt(i)
                    when (child) {
                        is TextView -> child.setTextColor(unselectedColor)
                        is android.widget.ImageView -> child.setColorFilter(unselectedIconColor)
                    }
                }
            }
        }
        
        // Update order card
        bindOrderToCard(order)
    }
    
    private fun setupOrderCard() {
        // Button click handler will be set in bindOrderToCard based on order status
    }
    
    private fun bindOrderToCard(order: Order) {
        val currentBinding = _binding ?: return
        
        currentOrder = order
        
        // Log order-level tip for debugging
        android.util.Log.d("ChefPageFragment", "Order #${order.orderNumber} chefTip: '${order.chefTip}'")
        
        // Header
        binding.tvTableHeader.text = "Table ${formatTableNumber(order.orderNumber)} - #${order.orderNumber}"
        binding.tvLastUpdated.text = DateTimeUtils.getTimeAgo(order.updatedAt)
        
        // Running late badge
        val isLate = isOrderRunningLate(order)
        binding.badgeRunningLate.visibility = if (isLate) View.VISIBLE else View.GONE
        
        // Items - show up to 4 items
        val itemsToShow = order.items.take(4)
        val itemConfigs = listOf(
            ItemViewConfig(binding.item1, binding.tvItem1Name, binding.tvItem1Qty, binding.tvItem1Size, binding.tvItem1Tips, binding.switchItem1, binding.colorBar1),
            ItemViewConfig(binding.item2, binding.tvItem2Name, binding.tvItem2Qty, binding.tvItem2Size, binding.tvItem2Tips, binding.switchItem2, binding.colorBar2),
            ItemViewConfig(binding.item3, binding.tvItem3Name, binding.tvItem3Qty, binding.tvItem3Size, binding.tvItem3Tips, binding.switchItem3, binding.colorBar3),
            ItemViewConfig(binding.item4, binding.tvItem4Name, binding.tvItem4Qty, binding.tvItem4Size, binding.tvItem4Tips, binding.switchItem4, binding.colorBar4)
        )
        
        itemsToShow.forEachIndexed { index, item ->
            if (index < itemConfigs.size) {
                val config = itemConfigs[index]
                // Log item tip for debugging
                android.util.Log.d("ChefPageFragment", "Item ${item.nameSnapshot} chefTip: '${item.chefTip}'")
                bindItemToView(item, config.itemView, config.nameView, config.qtyView, config.sizeView, config.tipsView, config.switchView, config.colorBar, order.chefTip)
                config.itemView.visibility = View.VISIBLE
            }
        }
        
        // Hide unused item views
        (itemsToShow.size until 4).forEach { index ->
            if (index < itemConfigs.size) {
                itemConfigs[index].itemView.visibility = View.GONE
            }
        }
        
        // Update bottom buttons
        updateBottomButtonsForOrderType(order.type)
        
        // Update button visibility and text based on order status
        when (order.status) {
            OrderStatus.CREATED -> {
                // New order - show accept button
                binding.btnMarkAllPrepared.visibility = View.VISIBLE
                binding.btnMarkAllPrepared.text = "Accept Order"
                binding.btnMarkAllPrepared.setOnClickListener {
                    viewModel.acceptOrder(order.id)
                }
                // Hide item switches for new orders (can't mark items prepared until accepted)
                itemConfigs.take(itemsToShow.size).forEach { config ->
                    config.switchView.visibility = View.GONE
                }
            }
            OrderStatus.IN_KITCHEN -> {
                // Preparing order - show mark prepared button
                binding.btnMarkAllPrepared.visibility = View.VISIBLE
                binding.btnMarkAllPrepared.text = "Mark All Prepared"
                binding.btnMarkAllPrepared.setOnClickListener {
                    viewModel.markPrepared(order.id)
                }
                // Show item switches for preparing orders
                itemConfigs.take(itemsToShow.size).forEach { config ->
                    config.switchView.visibility = View.VISIBLE
                }
            }
            else -> {
                binding.btnMarkAllPrepared.visibility = View.GONE
            }
        }
    }
    
    private fun bindItemToView(
        item: OrderItem,
        itemView: LinearLayout,
        nameView: TextView,
        qtyView: TextView,
        sizeView: TextView,
        tipsView: TextView,
        switchView: androidx.appcompat.widget.SwitchCompat,
        colorBar: View,
        globalTip: String
    ) {
        nameView.text = item.nameSnapshot
        qtyView.text = "x${item.qty}"
        sizeView.text = "Size: ${item.size ?: "-"}"
        
        // Show item-specific tip first, then fall back to order-level tip
        val tipToShow = when {
            item.chefTip.isNotBlank() -> item.chefTip
            globalTip.isNotBlank() -> globalTip
            else -> null
        }
        
        if (tipToShow != null) {
            tipsView.text = "Tips: $tipToShow"
            tipsView.visibility = View.VISIBLE
        } else {
            tipsView.text = "Tips: -"
            tipsView.visibility = View.VISIBLE
        }
        
        // Get prepared state for this item
        val isPrepared = itemPreparedStates[item.id] ?: false
        switchView.isChecked = isPrepared
        
        // Update color bar
        colorBar.setBackgroundColor(
            if (isPrepared) 0xFF2ECC71.toInt() else 0xFFF5515F.toInt()
        )
        
        // Set switch listener
        switchView.setOnCheckedChangeListener(null) // Remove previous listener
        switchView.setOnCheckedChangeListener { _, isChecked ->
            itemPreparedStates[item.id] = isChecked
            colorBar.setBackgroundColor(
                if (isChecked) 0xFF2ECC71.toInt() else 0xFFF5515F.toInt()
            )
            checkIfAllPrepared()
        }
    }
    
    private fun checkIfAllPrepared() {
        val order = currentOrder ?: return
        val allPrepared = order.items.all { item ->
            itemPreparedStates[item.id] == true
        }
        
        if (allPrepared && order.items.isNotEmpty()) {
            // Auto-mark as prepared when all items are done
            viewModel.markPrepared(order.id)
            // Clear prepared states for this order
            order.items.forEach { item ->
                itemPreparedStates.remove(item.id)
            }
        }
    }
    
    private fun setupBottomButtons() {
        binding.btnEatAway.setOnClickListener {
            Toast.makeText(requireContext(), "Eat away selected", Toast.LENGTH_SHORT).show()
            // TODO: Update order type to DINE_IN (backend later)
        }
        
        binding.btnParcel.setOnClickListener {
            Toast.makeText(requireContext(), "Parcel selected", Toast.LENGTH_SHORT).show()
            // TODO: Update order type to PARCEL (backend later)
        }
    }
    
    private fun updateBottomButtonsForOrderType(type: OrderType) {
        when (type) {
            OrderType.DINE_IN -> {
                binding.btnEatAway.setBackgroundResource(com.streatfeast.app.R.drawable.bg_black_pill)
                binding.btnEatAway.setTextColor(android.graphics.Color.WHITE)
                binding.btnParcel.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
                binding.btnParcel.setTextColor(0xFF111111.toInt())
            }
            OrderType.PARCEL -> {
                binding.btnParcel.setBackgroundResource(com.streatfeast.app.R.drawable.bg_black_pill)
                binding.btnParcel.setTextColor(android.graphics.Color.WHITE)
                binding.btnEatAway.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
                binding.btnEatAway.setTextColor(0xFF111111.toInt())
            }
            else -> {
                binding.btnEatAway.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
                binding.btnEatAway.setTextColor(0xFF111111.toInt())
                binding.btnParcel.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
                binding.btnParcel.setTextColor(0xFF111111.toInt())
            }
        }
    }
    
    private fun observeOrders() {
        // Already handled in setupTabs() via combinedOrders
        
        // Observe order acceptance to refresh data
        viewModel.orderAccepted.observe(viewLifecycleOwner) { orderId ->
            // Order was accepted, refresh to get updated status
            viewModel.refresh()
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
    
    // Helper functions
    private fun formatTableNumber(orderNumber: Int): String {
        return String.format("%02d", orderNumber % 100)
    }
    
    private fun isOrderRunningLate(order: Order): Boolean {
        val now = Instant.now()
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(
            now.toEpochMilli() - order.updatedAt.toEpochMilli()
        )
        return diffMinutes > 15
    }
    
    private fun showEmptyState() {
        // Hide all item views
        binding.item1.visibility = View.GONE
        binding.item2.visibility = View.GONE
        binding.item3.visibility = View.GONE
        binding.item4.visibility = View.GONE
        binding.btnMarkAllPrepared.visibility = View.GONE
        
        // Show empty message in card
        binding.tvTableHeader.text = "No orders in preparation"
        binding.tvLastUpdated.visibility = View.GONE
        binding.badgeRunningLate.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        binding.tvLastUpdated.visibility = View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        itemPreparedStates.clear()
        tabViews.clear()
        currentOrder = null
        selectedTabOrderId = null
    }
}

