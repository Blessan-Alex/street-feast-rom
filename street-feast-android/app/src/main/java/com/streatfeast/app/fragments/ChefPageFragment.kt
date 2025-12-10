package com.streatfeast.app.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import com.streatfeast.app.databinding.ChefPageBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.Order
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.utils.DateTimeUtils
import com.streatfeast.app.ui.OrderDisplayMapper
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper data class for item view configuration
private data class ItemViewConfig(
    val itemView: LinearLayout,
    val nameView: TextView,
    val qtyView: TextView,
    val sizeView: TextView,
    val tipsView: TextView,
    val switchView: androidx.appcompat.widget.SwitchCompat,
    val colorBar: View,
    val preparedIcon: ImageView
)

@RequiresApi(Build.VERSION_CODES.O)
class ChefPageFragment : Fragment() {
    
    private var _binding: ChefPageBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    
    private var currentOrder: Order? = null
    private val itemPreparedStates = mutableMapOf<String, Boolean>() // itemId -> isPrepared
    private val itemLoadingStates = mutableSetOf<String>() // itemId loading RPC
    private val tabViews = mutableMapOf<String, LinearLayout>() // orderId -> tab view
    private var selectedTabOrderId: String? = null
    private lateinit var viewPager: ViewPager2
    private var pagerAdapter: OrdersPagerAdapter? = null
    
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
        setupRefreshButton()
        setupLogoutButton()
        setupTabs()
        setupViewPager()
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
    
    private fun setupRefreshButton() {
        val refreshButton = binding.btnRefresh
        val refreshIcon = binding.root.findViewById<android.widget.ImageView>(com.streatfeast.app.R.id.ivRefresh)
        var isRefreshing = false
        
        refreshButton.setOnClickListener {
            if (!isRefreshing) {
                isRefreshing = true
                refreshIcon.animate()
                    .rotationBy(360f)
                    .setDuration(500)
                    .withEndAction {
                        refreshIcon.rotation = 0f
                        isRefreshing = false
                    }
                    .start()
                
                Toast.makeText(requireContext(), "Refreshing orders...", Toast.LENGTH_SHORT).show()
                viewModel.refresh()
            }
        }
        
        // Observe loading state to show feedback
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading && isRefreshing) {
                Toast.makeText(requireContext(), "Orders refreshed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
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
                    android.util.Log.e("ChefPageFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    try {
                        ServiceLocator.clear()
                    } catch (clearError: Exception) {
                        android.util.Log.e("ChefPageFragment", "Error clearing ServiceLocator", clearError)
                    }
                }
            }
            // MainActivity will handle navigation when currentUser becomes null
        }
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
            
            // Show/hide tabs container and viewpager
            if (sorted.isEmpty()) {
                currentBinding.tabsScroll.visibility = View.GONE
                currentBinding.viewPagerOrders.visibility = View.GONE
                // Show empty state message in card area
                showEmptyState()
            } else {
                currentBinding.tabsScroll.visibility = View.VISIBLE
                currentBinding.viewPagerOrders.visibility = View.VISIBLE
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
                    OrderType.EAT_AWAY -> com.streatfeast.app.R.drawable.ic_bag
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
            text = OrderDisplayMapper.locationLabel(order)
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
        
        // Update order card (will be handled by ViewPager adapter)
        // Sync ViewPager position
        val orderIndex = allOrders.indexOfFirst { it.id == order.id }
        if (orderIndex >= 0 && ::viewPager.isInitialized) {
            if (viewPager.currentItem != orderIndex) {
                viewPager.setCurrentItem(orderIndex, false)
            }
        }
    }
    
    private fun setupViewPager() {
        viewPager = binding.viewPagerOrders
        
        // Create adapter once with empty list if not already created
        if (pagerAdapter == null) {
            pagerAdapter = OrdersPagerAdapter(mutableListOf()) { order: Order, view: View ->
                bindOrderToCard(order, view)
            }
            viewPager.adapter = pagerAdapter
        }
        
        combinedOrders.observe(viewLifecycleOwner) { orders ->
            // Get current order ID before updating to preserve position
            val currentOrderId = combinedOrders.value?.getOrNull(viewPager.currentItem)?.id
            
            // Update adapter with new orders
            pagerAdapter?.updateOrders(orders.toMutableList())
            
            // Preserve ViewPager position when updating
            // Try to find current order by ID in new list
            val newIndex = currentOrderId?.let { id -> orders.indexOfFirst { it.id == id } }
            if (newIndex != null && newIndex >= 0 && viewPager.currentItem != newIndex) {
                viewPager.setCurrentItem(newIndex, false)
            } else {
                // Fall back to selected tab position
                val currentIndex = orders.indexOfFirst { it.id == selectedTabOrderId }
                if (currentIndex >= 0 && viewPager.currentItem != currentIndex) {
                    viewPager.setCurrentItem(currentIndex, false)
                } else if (orders.isNotEmpty() && selectedTabOrderId == null) {
                    viewPager.setCurrentItem(0, false)
                }
            }
        }
        
        // Update selected tab when swiping
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                combinedOrders.value?.getOrNull(position)?.let { order ->
                    if (order.id != selectedTabOrderId) {
                        selectTab(order)
                    }
                }
            }
        })
    }
    
    private fun setupOrderCard() {
        // Button click handler will be set in bindOrderToCard based on order status
    }
    
    private fun bindOrderToCard(order: Order, cardView: View) {
        val currentBinding = _binding ?: return
        
        currentOrder = order
        
        // Log order-level tip for debugging
        android.util.Log.d("ChefPageFragment", "Order #${order.orderNumber} chefTip: '${order.chefTip}'")
        
        // Header
        val tvTableHeader = cardView.findViewById<TextView>(com.streatfeast.app.R.id.tvTableHeader)
        val tvLastUpdated = cardView.findViewById<TextView>(com.streatfeast.app.R.id.tvLastUpdated)
        val badgeRunningLate = cardView.findViewById<TextView>(com.streatfeast.app.R.id.badgeRunningLate)
        val tvMoreItems = cardView.findViewById<TextView>(com.streatfeast.app.R.id.tvMoreItems)
        
        tvTableHeader.text = OrderDisplayMapper.headerLabel(order)
        tvLastUpdated.text = DateTimeUtils.getTimeAgo(order.updatedAt)
        
        // Running late badge
        val isLate = isOrderRunningLate(order)
        badgeRunningLate.visibility = if (isLate) View.VISIBLE else View.GONE
        
        // Items - show up to 4 items, plus overflow indicator
        val itemsToShow = order.items.take(4)
        val itemConfigs = listOf(
            ItemViewConfig(
                cardView.findViewById(com.streatfeast.app.R.id.item1),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem1Name),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem1Qty),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem1Size),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem1Tips),
                cardView.findViewById(com.streatfeast.app.R.id.switchItem1),
                cardView.findViewById(com.streatfeast.app.R.id.colorBar1),
                cardView.findViewById(com.streatfeast.app.R.id.preparedIcon1)
            ),
            ItemViewConfig(
                cardView.findViewById(com.streatfeast.app.R.id.item2),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem2Name),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem2Qty),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem2Size),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem2Tips),
                cardView.findViewById(com.streatfeast.app.R.id.switchItem2),
                cardView.findViewById(com.streatfeast.app.R.id.colorBar2),
                cardView.findViewById(com.streatfeast.app.R.id.preparedIcon2)
            ),
            ItemViewConfig(
                cardView.findViewById(com.streatfeast.app.R.id.item3),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem3Name),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem3Qty),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem3Size),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem3Tips),
                cardView.findViewById(com.streatfeast.app.R.id.switchItem3),
                cardView.findViewById(com.streatfeast.app.R.id.colorBar3),
                cardView.findViewById(com.streatfeast.app.R.id.preparedIcon3)
            ),
            ItemViewConfig(
                cardView.findViewById(com.streatfeast.app.R.id.item4),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem4Name),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem4Qty),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem4Size),
                cardView.findViewById(com.streatfeast.app.R.id.tvItem4Tips),
                cardView.findViewById(com.streatfeast.app.R.id.switchItem4),
                cardView.findViewById(com.streatfeast.app.R.id.colorBar4),
                cardView.findViewById(com.streatfeast.app.R.id.preparedIcon4)
            )
        )
        
        itemsToShow.forEachIndexed { index, item ->
            if (index < itemConfigs.size) {
                val config = itemConfigs[index]
                // Log item tip for debugging
                android.util.Log.d("ChefPageFragment", "Item ${item.nameSnapshot} chefTip: '${item.chefTip}'")
                bindItemToView(
                    order.id,
                    item,
                    config.itemView,
                    config.nameView,
                    config.qtyView,
                    config.sizeView,
                    config.tipsView,
                    config.switchView,
                    config.colorBar,
                    config.preparedIcon,
                    order.chefTip
                )
                config.itemView.visibility = View.VISIBLE
            }
        }
        
        // Hide unused item views
        (itemsToShow.size until 4).forEach { index ->
            if (index < itemConfigs.size) {
                itemConfigs[index].itemView.visibility = View.GONE
            }
        }

        val remainingCount = order.items.size - itemsToShow.size
        if (remainingCount > 0) {
            tvMoreItems.visibility = View.VISIBLE
            tvMoreItems.text = "+$remainingCount more"
        } else {
            tvMoreItems.visibility = View.GONE
        }
        
        // Update bottom buttons
        // Button styles are now managed by updateButtonStyles() based on selected filter
        
        // Update button visibility and text based on order status
        val btnMarkAllPrepared = cardView.findViewById<android.widget.Button>(com.streatfeast.app.R.id.btnMarkAllPrepared)
        when (order.status) {
            OrderStatus.CREATED -> {
                // New order - show accept button
                btnMarkAllPrepared.visibility = View.VISIBLE
                btnMarkAllPrepared.text = "Accept Order"
                btnMarkAllPrepared.setOnClickListener {
                    viewModel.acceptOrder(order.id)
                }
                // Hide item switches for new orders (can't mark items prepared until accepted)
                itemConfigs.take(itemsToShow.size).forEach { config ->
                    config.switchView.visibility = View.GONE
                }
            }
            OrderStatus.IN_KITCHEN -> {
                // Preparing order - show mark prepared button
                btnMarkAllPrepared.visibility = View.VISIBLE
                btnMarkAllPrepared.text = "Mark All Prepared"
                btnMarkAllPrepared.setOnClickListener {
                    viewModel.markPrepared(order.id)
                }
                // Show item switches for preparing orders
                itemConfigs.take(itemsToShow.size).forEach { config ->
                    config.switchView.visibility = View.VISIBLE
                }
            }
            else -> {
                btnMarkAllPrepared.visibility = View.GONE
            }
        }
    }
    
    private fun bindItemToView(
        orderId: String,
        item: OrderItem,
        itemView: LinearLayout,
        nameView: TextView,
        qtyView: TextView,
        sizeView: TextView,
        tipsView: TextView,
        switchView: androidx.appcompat.widget.SwitchCompat,
        colorBar: View,
        preparedIcon: ImageView,
        globalTip: String
    ) {
        nameView.text = item.nameSnapshot
        nameView.visibility = View.VISIBLE
        qtyView.text = "x${item.qty}"
        qtyView.visibility = View.VISIBLE
        
        // Only show Size if it has a value
        if (item.size != null && item.size.isNotBlank()) {
            sizeView.text = "Size: ${item.size}"
            sizeView.visibility = View.VISIBLE
        } else {
            sizeView.visibility = View.GONE
        }
        
        // Show item-specific tip first, then fall back to order-level tip
        val tipToShow = when {
            item.chefTip.isNotBlank() -> item.chefTip
            globalTip.isNotBlank() -> globalTip
            else -> null
        }
        
        // Only show Tips if there's a tip to show
        if (tipToShow != null) {
            tipsView.text = "Tips: $tipToShow"
            tipsView.visibility = View.VISIBLE
        } else {
            tipsView.visibility = View.GONE
        }
        
        // Veg indicator is independent of prepared state
        val vegColor = if (item.isVeg) 0xFF2ECC71.toInt() else 0xFFF5515F.toInt()
        colorBar.setBackgroundColor(vegColor)

        // Prepared state (persisted where available)
        val preparedInitial = itemPreparedStates[item.id] ?: item.isPrepared
        itemPreparedStates[item.id] = preparedInitial

        fun applyPreparedUi(prepared: Boolean, loading: Boolean = false) {
            itemView.alpha = when {
                loading -> 0.4f
                prepared -> 0.6f
                else -> 1f
            }
            preparedIcon.visibility = if (prepared) View.VISIBLE else View.GONE
            switchView.isEnabled = !loading
        }

        switchView.setOnCheckedChangeListener(null)
        switchView.isChecked = preparedInitial
        applyPreparedUi(preparedInitial, itemLoadingStates.contains(item.id))

        lateinit var listener: CompoundButton.OnCheckedChangeListener
        listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            // Only handle toggle on; prevent accidental off toggle
            if (!isChecked) {
                switchView.setOnCheckedChangeListener(null)
                switchView.isChecked = itemPreparedStates[item.id] == true
                switchView.setOnCheckedChangeListener(listener)
                return@OnCheckedChangeListener
            }

            val previous = itemPreparedStates[item.id] ?: preparedInitial
            itemLoadingStates.add(item.id)
            applyPreparedUi(previous, loading = true)
            switchView.isEnabled = false

            if (!isAdded) return@OnCheckedChangeListener

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.markItemPrepared(orderId, item.id) { result ->
                    if (!isAdded) return@markItemPrepared

                    itemLoadingStates.remove(item.id)
                    val success = result.getOrNull()

                    if (result.isSuccess) {
                        itemPreparedStates[item.id] = true
                        switchView.setOnCheckedChangeListener(null)
                        switchView.isChecked = true
                        switchView.setOnCheckedChangeListener(listener)
                        applyPreparedUi(true, loading = false)

                        // Show local toast
                        Toast.makeText(
                            requireContext(),
                            "${item.nameSnapshot} has been prepared",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (success?.allPrepared == true) {
                            viewModel.markPrepared(orderId)
                        }
                    } else {
                        itemPreparedStates[item.id] = previous
                        switchView.setOnCheckedChangeListener(null)
                        switchView.isChecked = previous
                        switchView.setOnCheckedChangeListener(listener)
                        applyPreparedUi(previous, loading = false)
                        Toast.makeText(
                            requireContext(),
                            "Could not mark prepared. Try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    switchView.isEnabled = true
                }
            }
        }

        switchView.setOnCheckedChangeListener(listener)
    }
    
    private fun checkIfAllPrepared() {
        val order = currentOrder ?: return
        val allPrepared = order.items.all { item ->
            val prepared = itemPreparedStates[item.id] ?: item.isPrepared
            itemPreparedStates[item.id] = prepared
            prepared
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
            viewModel.setOrderTypeFilter(OrderType.DINE_IN)
            updateButtonStyles(OrderType.DINE_IN)
        }
        
        binding.btnParcel.setOnClickListener {
            viewModel.setOrderTypeFilter(OrderType.PARCEL)
            updateButtonStyles(OrderType.PARCEL)
        }
        
        binding.btnDelivery.setOnClickListener {
            viewModel.setOrderTypeFilter(OrderType.EAT_AWAY)
            updateButtonStyles(OrderType.EAT_AWAY)
        }
        
        // Observe filter changes and update button styles
        viewModel.selectedOrderTypeFilter.observe(viewLifecycleOwner) { filter ->
            updateButtonStyles(filter)
        }
        
        // Set initial button style
        updateButtonStyles(OrderType.DINE_IN)
    }
    
    private fun updateButtonStyles(selectedFilter: OrderType?) {
        // Style Dine In button
        if (selectedFilter == OrderType.DINE_IN) {
            binding.btnEatAway.setBackgroundResource(com.streatfeast.app.R.drawable.bg_black_pill)
            binding.btnEatAway.setTextColor(android.graphics.Color.WHITE)
            binding.btnEatAway.elevation = 4f // Add shadow/elevation for selected
        } else {
            binding.btnEatAway.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
            binding.btnEatAway.setTextColor(0xFF111111.toInt()) // Black text
            binding.btnEatAway.elevation = 2f // Add shadow/elevation for unselected
        }
        
        // Style Parcel button
        if (selectedFilter == OrderType.PARCEL) {
            binding.btnParcel.setBackgroundResource(com.streatfeast.app.R.drawable.bg_black_pill)
            binding.btnParcel.setTextColor(android.graphics.Color.WHITE)
            binding.btnParcel.elevation = 4f // Add shadow/elevation for selected
        } else {
            binding.btnParcel.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
            binding.btnParcel.setTextColor(0xFF111111.toInt()) // Black text
            binding.btnParcel.elevation = 2f // Add shadow/elevation for unselected
        }
        
        // Style Delivery button
        if (selectedFilter == OrderType.EAT_AWAY) {
            binding.btnDelivery.setBackgroundResource(com.streatfeast.app.R.drawable.bg_black_pill)
            binding.btnDelivery.setTextColor(android.graphics.Color.WHITE)
            binding.btnDelivery.elevation = 4f // Add shadow/elevation for selected
        } else {
            binding.btnDelivery.setBackgroundResource(com.streatfeast.app.R.drawable.bg_white_outline_pill)
            binding.btnDelivery.setTextColor(0xFF111111.toInt()) // Black text
            binding.btnDelivery.elevation = 2f // Add shadow/elevation for unselected
        }
    }
    
    private fun observeOrders() {
        // Already handled in setupTabs() via combinedOrders
        
        // Observe new order detection to refresh data immediately
        viewModel.newOrderDetected.observe(viewLifecycleOwner) { (orderId, orderNumber) ->
            android.util.Log.d("ChefPageFragment", "New order detected: id=$orderId, number=$orderNumber - refreshing orders")
            // Force refresh to get the new order into the list
            viewModel.refresh()
            
            // Auto-scroll to the new order after refresh completes
            viewLifecycleOwner.lifecycleScope.launch {
                delay(800) // Increased delay to ensure refresh completes
                // Check if viewPager is initialized before using it
                if (::viewPager.isInitialized) {
                    // Try to find and select the new order
                    val orders = combinedOrders.value ?: emptyList()
                    android.util.Log.d("ChefPageFragment", "After refresh delay, checking for order $orderId in ${orders.size} orders")
                    val newOrderIndex = orders.indexOfFirst { it.id == orderId }
                    if (newOrderIndex >= 0) {
                        android.util.Log.d("ChefPageFragment", "Auto-scrolling to new order at index $newOrderIndex")
                        viewPager.setCurrentItem(newOrderIndex, true)
                        selectTab(orders[newOrderIndex])
                    } else {
                        android.util.Log.d("ChefPageFragment", "New order not found in list yet, may need more time. Current filter: ${viewModel.selectedOrderTypeFilter.value}")
                    }
                }
            }
        }
        
        // Observe order acceptance to refresh data
        viewModel.orderAccepted.observe(viewLifecycleOwner) { orderId ->
            // Order was accepted, refresh to get updated status
            android.util.Log.d("ChefPageFragment", "Order accepted: $orderId - refreshing")
            viewModel.refresh()
        }
        
        // Observe loading state to know when refresh completes
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                // Refresh completed, ensure UI updates
                android.util.Log.d("ChefPageFragment", "Refresh completed, checking for updates. Current orders count: ${combinedOrders.value?.size ?: 0}")
            } else {
                android.util.Log.d("ChefPageFragment", "Refresh started")
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
    
    private fun isOrderRunningLate(order: Order): Boolean {
        val now = Instant.now()
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(
            now.toEpochMilli() - order.updatedAt.toEpochMilli()
        )
        return diffMinutes > 15
    }
    
    private fun showEmptyState() {
        // Empty state is handled by ViewPager2 having no items
        // Individual views are in order_card_item.xml and managed by the adapter
    }
    
    private fun hideEmptyState() {
        // Empty state is handled by ViewPager2 having items
        // Individual views are in order_card_item.xml and managed by the adapter
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

// ViewPager2 Adapter for orders
private class OrdersPagerAdapter(
    private var orders: MutableList<Order>,
    private val onBindOrder: (Order, View) -> Unit
) : RecyclerView.Adapter<OrdersPagerAdapter.OrderViewHolder>() {
    
    class OrderViewHolder(val view: View) : RecyclerView.ViewHolder(view)
    
    fun updateOrders(newOrders: List<Order>) {
        val oldSize = orders.size
        orders.clear()
        orders.addAll(newOrders)
        if (oldSize == orders.size) {
            notifyItemRangeChanged(0, orders.size)
        } else {
            notifyDataSetChanged()
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.streatfeast.app.R.layout.order_card_item, parent, false)
        return OrderViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        onBindOrder(order, holder.view)
    }
    
    override fun getItemCount() = orders.size
}

