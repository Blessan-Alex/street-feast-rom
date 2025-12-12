package com.streatfeast.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.CategoryAdapter
import com.streatfeast.app.adapters.MostBoughtAdapter
import com.streatfeast.app.databinding.FragmentOrderItemBinding

import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.Category
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.models.OrderStatus
import com.streatfeast.app.navigation.OrderEditMode
import com.streatfeast.app.navigation.OrderNavArgs
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import com.streatfeast.app.viewmodels.MenuViewModel
import com.streatfeast.app.viewmodels.MenuViewModelFactory
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.storage.StreetFeastDatabase
import com.streatfeast.app.utils.Constants
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderItemFragment : Fragment() {
    
    private var _binding: FragmentOrderItemBinding? = null
    private val binding get() = _binding!!
    
    private var navArgs: OrderNavArgs = OrderNavArgs()
    private var orderType: OrderType = OrderType.DINE_IN
    private var tableNumber: Int = Constants.DEFAULT_TABLE_COUNT
    private var licensePlate: String? = null
    private var showHeader: Boolean = false
    private var editMode: OrderEditMode = OrderEditMode.NEW
    private var currentSearchTerm: String = ""
    private var allMenuItems: List<MenuItem> = emptyList()
    private var frequentItemIds: List<String> = emptyList()
    private var allCategories: List<Category> = emptyList()
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var mostBoughtAdapter: MostBoughtAdapter
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    private val menuViewModel: MenuViewModel by viewModels {
        val repository = ServiceLocator.provideMenuRepository(requireContext().applicationContext)
        val db = StreetFeastDatabase.getInstance(requireContext())
        val localDataSource = com.streatfeast.app.storage.MenuLocalDataSource(db)
        MenuViewModelFactory(repository, localDataSource, Constants.DEFAULT_STORE_ID)
    }
    private val ordersViewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    private lateinit var menuLoadingView: ProgressBar
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderItemBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addLoadingSpinner()
        
        navArgs = OrderNavArgs.from(arguments)
        orderType = navArgs.orderType
        tableNumber = navArgs.tableNumber ?: Constants.DEFAULT_TABLE_COUNT
        licensePlate = navArgs.licensePlate
        showHeader = navArgs.showHeader
        editMode = navArgs.editMode
        
        setupScreenState()
        setupStepper()
        setupTableHandle()
        setupSearch()
        setupMostBoughtItems()
        setupCategories()
        setupPreviewBar()
        setupUpArrow()
        setupAppbar()
        setupMenuRefreshButton()
        setupLogoutButton()
        setupBottomNavigation()
        
        // Force menu refresh to debug categories issue
        menuViewModel.loadMenu()
    }

    private fun addLoadingSpinner() {
        menuLoadingView = ProgressBar(requireContext()).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
        (binding.root as ConstraintLayout).addView(menuLoadingView, params)
    }
    
    private fun setupScreenState() {
        val scrollParams = binding.scroll.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        
        if (showHeader) {
            // Screen 4: Show header, hide table handle
            binding.appbar.root.visibility = View.VISIBLE
            binding.tvCreateOrder.visibility = View.VISIBLE
            binding.stepper.root.visibility = View.VISIBLE
            binding.ivUp.visibility = View.VISIBLE
            binding.tableHandle.root.visibility = View.GONE
            
            // Update scroll constraint to ivUp
            scrollParams.topToBottom = R.id.ivUp
            scrollParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
        } else {
            // Screen 3: Hide header, show table handle
            binding.appbar.root.visibility = View.GONE
            binding.tvCreateOrder.visibility = View.GONE
            binding.stepper.root.visibility = View.GONE
            binding.ivUp.visibility = View.GONE
            binding.tableHandle.root.visibility = View.VISIBLE
            
            // Update scroll constraint to tableHandle
            scrollParams.topToBottom = R.id.tableHandle
            scrollParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
        }
        
        binding.scroll.layoutParams = scrollParams
    }
    
    private fun setupStepper() {
        if (!showHeader) return // Only update stepper for Screen 4
        
        val stepperView = binding.stepper.root
        val d1 = stepperView.findViewById<View>(R.id.d1)
        val d2 = stepperView.findViewById<View>(R.id.d2)
        val d3 = stepperView.findViewById<View>(R.id.d3)
        val v1 = stepperView.findViewById<android.widget.TextView>(R.id.v1)
        val v2 = stepperView.findViewById<android.widget.TextView>(R.id.v2)
        
        // Set d1, d2, d3 to active
        d1?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        d2?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        d3?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        
        // Set v1 and v2 to visible with values
        v1?.visibility = View.VISIBLE
        v1?.text = getOrderTypeDisplayText(orderType)
        
        v2?.visibility = View.VISIBLE
        v2?.text = "No $tableNumber"
    }
    
    private fun getOrderTypeDisplayText(type: OrderType): String {
        return when (type) {
            OrderType.DINE_IN -> "Dine in"
            OrderType.PARCEL -> "Parcel"
            OrderType.EAT_AWAY -> "Eat away"
        }
    }
    
    private fun setupTableHandle() {
        val tableHandleView = binding.tableHandle.root
        val tvTable = tableHandleView.findViewById<android.widget.TextView>(R.id.tvTable)
        tvTable?.text = "Table $tableNumber"
        
        // Handle down chevron click - expand to Screen 4
        val ivDown = tableHandleView.findViewById<android.widget.ImageView>(R.id.ivDown)
        
        // Click handler
        ivDown?.setOnClickListener {
            expandBreadcrumbs()
        }
        
        // Swipe down gesture on table handle
        var startY = 0f
        var startTime = 0L
        var hasMoved = false
        
        tableHandleView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    startTime = System.currentTimeMillis()
                    hasMoved = false
                    false // Don't consume - allow click to work
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - startY
                    // Track if user actually moved (swipe, not just tap)
                    if (Math.abs(deltaY) > 10) {
                        hasMoved = true
                    }
                    false // Don't consume during move
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.y - startY
                    val deltaTime = System.currentTimeMillis() - startTime
                    
                    // Swipe down: positive deltaY, reasonable speed and distance
                    if (hasMoved && deltaY > 100 && deltaTime < 300) {
                        expandBreadcrumbs()
                        true
                    } else {
                        false // Let click handler work for taps
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    false
                }
                else -> false
            }
        }
    }
    
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchTerm = s?.toString()?.trim() ?: ""
                filterMostBought(currentSearchTerm)
                filterCategories(currentSearchTerm)
            }
        })
    }
    
    private fun setupMostBoughtItems() {
        // Create adapter for most bought items
        mostBoughtAdapter = MostBoughtAdapter(
            onItemClick = { menuItem ->
                openCustomizationModal(menuItem)
            },
            onAddClick = { menuItem ->
                openCustomizationModal(menuItem)
            }
        )
        
        // Setup horizontal RecyclerView
        binding.rvMostBought.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvMostBought.adapter = mostBoughtAdapter
        
        // Observe frequent ids and items; render via filter helper
        menuViewModel.frequentItemIds.observe(viewLifecycleOwner) { ids ->
            frequentItemIds = ids
            filterMostBought(currentSearchTerm)
        }
        menuViewModel.items.observe(viewLifecycleOwner) { items ->
            allMenuItems = items
            filterMostBought(currentSearchTerm)
        }
    }
    
    private fun setupCategories() {
        // Create adapter for categories
        categoryAdapter = CategoryAdapter { category ->
            navigateToCategoryItems(category.name, category.id)
        }
        
        // Setup RecyclerView with 2 columns (matching the original Flow layout)
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategories.adapter = categoryAdapter
        
        menuViewModel.categories.observe(viewLifecycleOwner) { categories ->
            android.util.Log.d("OrderItemFragment", "Categories observed: ${categories.size}")
            categories.forEach { cat ->
                android.util.Log.d("OrderItemFragment", "Category: ${cat.name} (id: ${cat.id}, active: ${cat.isActive})")
            }
            allCategories = categories
            filterCategories(currentSearchTerm)
        }
        
        menuViewModel.items.observe(viewLifecycleOwner) { items ->
            android.util.Log.d("OrderItemFragment", "Items observed: ${items.size}")
            allMenuItems = items
            // Update item counts in adapter
            categoryAdapter.updateItemCounts(items)
            filterCategories(currentSearchTerm)
        }
        
        // Also check loading and error states
        menuViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            android.util.Log.d("OrderItemFragment", "Menu loading: $isLoading")
        }
        
        menuViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.util.Log.e("OrderItemFragment", "Menu error: $it")
            }
        }
    }
    
    private fun setupUpArrow() {
        // Allow tap to collapse header
        binding.ivUp.setOnClickListener { collapseBreadcrumbs() }
        
        // Keep only the swipe gesture detector
        var startY = 0f
        var startTime = 0L
        var hasMoved = false
        
        binding.ivUp.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    startTime = System.currentTimeMillis()
                    hasMoved = false
                    false // Don't consume
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.y
                    // Track if user actually moved (swipe, not just tap)
                    if (Math.abs(deltaY) > 10) {
                        hasMoved = true
                    }
                    false // Don't consume during move
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = startY - event.y
                    val deltaTime = System.currentTimeMillis() - startTime
                    
                    // Swipe up: negative deltaY (startY > event.y), reasonable speed and distance
                    if (hasMoved && deltaY > 100 && deltaTime < 300) {
                        collapseBreadcrumbs()
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    false
                }
                else -> false
            }
        }
    }
    
    private fun expandBreadcrumbs() {
        // Only navigate if we're not already showing header
        if (!showHeader) {
            val nextArgs = navArgs.copy(
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = licensePlate,
                showHeader = true
            )
            findNavController().navigate(R.id.orderItemFragment, nextArgs.toBundle())
        }
    }
    
    private fun collapseBreadcrumbs() {
        // Only navigate if we're currently showing header
        if (showHeader) {
            val nextArgs = navArgs.copy(
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = licensePlate,
                showHeader = false
            )
            findNavController().navigate(R.id.orderItemFragment, nextArgs.toBundle())
        }
    }
    
    private fun setupAppbar() {
        val navBack = binding.appbar.root.findViewById<View>(R.id.ivNavBack)
        navBack?.setOnClickListener {
            // Navigate directly to orderWhereFragment
            val bundle = navArgs.copy(
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = licensePlate
            ).toBundle()
            findNavController().navigate(R.id.orderWhereFragment, bundle)
        }
    }

    private fun setupMenuRefreshButton() {
        val refreshButton = binding.appbar.root.findViewById<ViewGroup>(R.id.btnRefreshMenu)
        val refreshIcon = binding.appbar.root.findViewById<android.widget.ImageView>(R.id.ivRefreshMenu)
        
        // Show button only when header is visible
        refreshButton?.visibility = if (showHeader) View.VISIBLE else View.GONE
        
        var isRefreshing = false
        
        refreshButton?.setOnClickListener {
            if (!isRefreshing) {
                isRefreshing = true
                refreshIcon?.animate()
                    ?.rotationBy(360f)
                    ?.setDuration(500)
                    ?.withEndAction {
                        refreshIcon?.rotation = 0f
                        isRefreshing = false
                    }
                    ?.start()
                
                android.widget.Toast.makeText(requireContext(), "Refreshing menu...", android.widget.Toast.LENGTH_SHORT).show()
                menuViewModel.loadMenu()
            }
        }
        
        // Observe menu loading state to show feedback
        menuViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading && isRefreshing) {
                android.widget.Toast.makeText(requireContext(), "Menu refreshed", android.widget.Toast.LENGTH_SHORT).show()
                isRefreshing = false
            }
            menuLoadingView.isVisible = isLoading
            binding.scroll.isVisible = !isLoading
        }
        
        // Observe menu errors
        menuViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(requireContext(), "Menu error: $it", android.widget.Toast.LENGTH_LONG).show()
                isRefreshing = false
            }
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
                    android.util.Log.e("OrderItemFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    try {
                        ServiceLocator.clear()
                    } catch (clearError: Exception) {
                        android.util.Log.e("OrderItemFragment", "Error clearing ServiceLocator", clearError)
                    }
                }
            }
            // MainActivity will handle navigation when currentUser becomes null
        }
    }
    
    private fun setupPreviewBar() {
        // Observe draft item count
        draftViewModel.itemCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.previewBar.root.visibility = View.VISIBLE
                val badgeCount = binding.previewBar.root.findViewById<android.widget.TextView>(R.id.tvBadgeCount)
                badgeCount?.text = count.toString()
            } else {
                binding.previewBar.root.visibility = View.GONE
            }
        }
        
        // Handle preview bar click - navigate to preview order (Screen 10)
        binding.previewBar.root.setOnClickListener {
            val nextArgs = navArgs.copy(
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = licensePlate,
                editMode = editMode,              // keep ADD_ITEMS/EDIT context
                existingOrderId = navArgs.existingOrderId
            )
            findNavController().navigate(R.id.previewOrderFragment, nextArgs.toBundle())
        }
    }
    
    private fun openCustomizationModal(menuItem: MenuItem) {
        val bundle = Bundle().apply {
            putParcelable("menuItem", menuItem)
        }
        findNavController().navigate(R.id.itemCustomizeFragment, bundle)
    }
    
    private fun navigateToCategoryItems(categoryName: String, categoryId: String) {
        val bundle = Bundle().apply {
            putString("categoryName", categoryName)
            putString("categoryId", categoryId)
            putInt("tableNumber", tableNumber)
            putString("orderType", orderType.name)
            putBoolean("showHeader", showHeader)
            licensePlate?.let { putString("licensePlate", it) }
        }
        findNavController().navigate(R.id.orderCategoryItemsFragment, bundle)
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

    private fun filterMostBought(term: String) {
        val normalized = term.lowercase()
        
        // Create a map for quick lookup
        val itemsMap = allMenuItems.associateBy { it.id }
        
        // Filter and preserve order from frequentItemIds (ordered by orderIndex from database)
        val filteredItems = frequentItemIds.mapNotNull { itemId ->
            itemsMap[itemId]?.takeIf { item ->
                normalized.isBlank() ||
                    item.name.lowercase().contains(normalized) ||
                    item.description.lowercase().contains(normalized)
            }
        } // Removed .take(4) to show all frequent items
        
        // Update adapter with filtered items (preserving database order)
        mostBoughtAdapter.submitList(filteredItems)
    }

    private fun filterCategories(term: String) {
        val normalized = term.lowercase()
        val filteredCategories = if (normalized.isBlank()) {
            allCategories
        } else {
            allCategories.filter { category ->
                category.name.lowercase().contains(normalized) ||
                    allMenuItems.any { it.categoryId == category.id && (it.name.lowercase().contains(normalized) || it.description.lowercase().contains(normalized)) }
            }
        }
        
        // Update adapter with filtered categories
        categoryAdapter.submitList(filteredCategories)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

