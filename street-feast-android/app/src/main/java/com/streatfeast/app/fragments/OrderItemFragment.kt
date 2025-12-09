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
import com.streatfeast.app.R
import com.streatfeast.app.databinding.FragmentOrderItemBinding
import com.streatfeast.app.ui.AppBarController

import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.Category
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import com.streatfeast.app.viewmodels.MenuViewModel
import com.streatfeast.app.viewmodels.MenuViewModelFactory
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
    
    private var orderType: OrderType = OrderType.DINE_IN
    private var tableNumber: Int = 4
    private var showHeader: Boolean = false
    private var currentSearchTerm: String = ""
    private var allMenuItems: List<MenuItem> = emptyList()
    private var frequentItemIds: List<String> = emptyList()
    private var allCategories: List<Category> = emptyList()
    private var appBarController: AppBarController? = null
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    private val menuViewModel: MenuViewModel by viewModels {
        val repository = ServiceLocator.provideMenuRepository(requireContext().applicationContext)
        val db = StreetFeastDatabase.getInstance(requireContext())
        val localDataSource = com.streatfeast.app.storage.MenuLocalDataSource(db)
        MenuViewModelFactory(repository, localDataSource, Constants.DEFAULT_STORE_ID)
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
        
        // Get arguments
        arguments?.let { args ->
            args.getString("orderType")?.let { typeString ->
                orderType = try {
                    OrderType.valueOf(typeString)
                } catch (e: IllegalArgumentException) {
                    OrderType.DINE_IN
                }
            }
            tableNumber = args.getInt("tableNumber", 4)
            showHeader = args.getBoolean("showHeader", false)
        }
        
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
        setupAppbarHandle()
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
            binding.btnGoBack.visibility = View.GONE // Hide go back button
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
            binding.btnGoBack.visibility = View.GONE
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
        ivDown?.setOnClickListener {
            // Navigate to Screen 4 (same fragment with showHeader=true)
            val bundle = Bundle().apply {
                putString("orderType", orderType.name)
                putInt("tableNumber", tableNumber)
                putBoolean("showHeader", true)
            }
            findNavController().navigate(R.id.orderItemFragment, bundle)
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
        val mostBoughtCards = listOf(
            binding.mb1.root to 0,
            binding.mb2.root to 1,
            binding.mb3.root to 2,
            binding.mb4.root to 3
        )
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
        val categoryCards = listOf(
            binding.c1.root to 0,
            binding.c2.root to 1,
            binding.c3.root to 2,
            binding.c4.root to 3,
            binding.c5.root to 4,
            binding.c6.root to 5,
            binding.c7.root to 6,
            binding.c8.root to 7,
            binding.c9.root to 8
        )
        
        menuViewModel.categories.observe(viewLifecycleOwner) { categories ->
            allCategories = categories
            filterCategories(currentSearchTerm)
        }
        menuViewModel.items.observe(viewLifecycleOwner) { items ->
            allMenuItems = items
            filterCategories(currentSearchTerm)
        }
    }
    
    private fun setupUpArrow() {
        binding.ivUp.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Add swipe gesture detector for upward arrow
        binding.ivUp.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0f
            private val swipeThreshold = 100f // Minimum distance for swipe
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = startY - event.y // Positive if swiped up
                        if (deltaY > swipeThreshold) {
                            // Swiped up - collapse breadcrumbs
                            collapseBreadcrumbs()
                            return true
                        }
                    }
                }
                return false
            }
        })
    }
    
    private fun collapseBreadcrumbs() {
        // Navigate back to Screen 3 (same fragment with showHeader=false)
        val bundle = Bundle().apply {
            putString("orderType", orderType.name)
            putInt("tableNumber", tableNumber)
            putBoolean("showHeader", false)
        }
        findNavController().navigate(R.id.orderItemFragment, bundle)
    }
    
    private fun setupAppbar() {
        val navBack = binding.appbar.root.findViewById<View>(R.id.ivNavBack)
        navBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupAppbarHandle() {
        val appbarView = binding.appbar.root
        val handle = appbarView.findViewById<View>(R.id.ivAppbarHandle)
        appBarController = AppBarController(
            appBar = appbarView,
            handleView = handle
        )
        appBarController?.attach()
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
            val bundle = Bundle().apply {
                putInt("tableNumber", tableNumber)
                putString("orderType", orderType.name)
            }
            findNavController().navigate(R.id.previewOrderFragment, bundle)
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
        val filteredItems = allMenuItems.filter { item ->
            frequentItemIds.contains(item.id) &&
                (normalized.isBlank() ||
                    item.name.lowercase().contains(normalized) ||
                    item.description.lowercase().contains(normalized))
        }.take(4)
        
        val mostBoughtCards = listOf(
            binding.mb1.root,
            binding.mb2.root,
            binding.mb3.root,
            binding.mb4.root
        )
        
        mostBoughtCards.forEachIndexed { index, cardView ->
            if (index < filteredItems.size) {
                val menuItem = filteredItems[index]
                val tvName = cardView.findViewById<android.widget.TextView>(R.id.tvName)
                val tvQty = cardView.findViewById<android.widget.TextView>(R.id.tvQty)
                val btnAdd = cardView.findViewById<ViewGroup>(R.id.btnAdd)
                
                tvName?.text = menuItem.name
                if (menuItem.sizes.isNullOrEmpty()) {
                    tvQty?.visibility = View.GONE
                } else {
                    tvQty?.visibility = View.VISIBLE
                    tvQty?.text = "qnty: ${menuItem.sizes.joinToString(",")}"
                }
                
                val dotGreen = cardView.findViewById<View>(R.id.dotGreen)
                val dotRed = cardView.findViewById<View>(R.id.dotRed)
                when (menuItem.vegFlag) {
                    "Veg" -> {
                        dotGreen?.visibility = View.VISIBLE
                        dotRed?.visibility = View.GONE
                    }
                    "NonVeg" -> {
                        dotGreen?.visibility = View.GONE
                        dotRed?.visibility = View.VISIBLE
                    }
                    else -> {
                        dotGreen?.visibility = View.VISIBLE
                        dotRed?.visibility = View.VISIBLE
                    }
                }
                
                btnAdd?.setOnClickListener {
                    openCustomizationModal(menuItem)
                }
                
                cardView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
            }
        }
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
        
        val categoryCards = listOf(
            binding.c1.root,
            binding.c2.root,
            binding.c3.root,
            binding.c4.root,
            binding.c5.root,
            binding.c6.root,
            binding.c7.root,
            binding.c8.root,
            binding.c9.root
        )
        
        categoryCards.forEachIndexed { index, cardView ->
            if (index < filteredCategories.size) {
                val category = filteredCategories[index]
                val tvCat = cardView.findViewById<android.widget.TextView>(R.id.tvCat)
                val tvItems = cardView.findViewById<android.widget.TextView>(R.id.tvItems)
                
                tvCat?.text = category.name
                val itemCount = allMenuItems.count { it.categoryId == category.id }
                tvItems?.text = "Items $itemCount"
                
                cardView.setOnClickListener {
                    navigateToCategoryItems(category.name, category.id)
                }
                
                cardView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

