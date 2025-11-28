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
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderItemFragment : Fragment() {
    
    private var _binding: FragmentOrderItemBinding? = null
    private val binding get() = _binding!!
    
    private var orderType: OrderType = OrderType.DINE_IN
    private var tableNumber: Int = 4
    private var showHeader: Boolean = false
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    
    // Mock data for now - will be replaced with ViewModel/Repository later
    private val mockCategories = listOf(
        "Chinese", "Indian", "Desserts", "Italian", "Mexican",
        "Thai", "Japanese", "American", "Mediterranean"
    )
    
    private val mockMostBoughtItems = listOf(
        "Ramen", "Paneer Tikka", "Spring Rolls", "Butter Chicken"
    )
    
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
        setupLogoutButton()
        setupBottomNavigation()
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
            OrderType.DELIVERY -> "Eat away"
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
                // Filter items/categories based on search term
                // TODO: Implement search filtering when menu data is available
                val searchTerm = s?.toString() ?: ""
                android.util.Log.d("OrderItemFragment", "Search: $searchTerm")
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
        
        mostBoughtCards.forEach { (cardView, index) ->
            if (index < mockMostBoughtItems.size) {
                val itemName = mockMostBoughtItems[index]
                val tvName = cardView.findViewById<android.widget.TextView>(R.id.tvName)
                val tvQty = cardView.findViewById<android.widget.TextView>(R.id.tvQty)
                val btnAdd = cardView.findViewById<ViewGroup>(R.id.btnAdd)
                
                tvName?.text = itemName
                tvQty?.text = "qnty: Small,mid,large"
                
                // Set dot visibility based on item type (mock - green for veg, red for non-veg)
                val dotGreen = cardView.findViewById<View>(R.id.dotGreen)
                val dotRed = cardView.findViewById<View>(R.id.dotRed)
                dotGreen?.visibility = if (index % 2 == 0) View.VISIBLE else View.GONE
                dotRed?.visibility = if (index % 2 == 1) View.VISIBLE else View.GONE
                
                // Handle add button click
                btnAdd?.setOnClickListener {
                    openCustomizationModal(itemName)
                }
                
                cardView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
            }
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
        
        categoryCards.forEach { (cardView, index) ->
            if (index < mockCategories.size) {
                val categoryName = mockCategories[index]
                val tvCat = cardView.findViewById<android.widget.TextView>(R.id.tvCat)
                val tvItems = cardView.findViewById<android.widget.TextView>(R.id.tvItems)
                
                tvCat?.text = categoryName
                tvItems?.text = "Items ${(index + 1) * 3}" // Mock item count
                
                // Handle category click
                cardView.setOnClickListener {
                    navigateToCategoryItems(categoryName)
                }
                
                cardView.visibility = View.VISIBLE
            } else {
                cardView.visibility = View.GONE
            }
        }
    }
    
    private fun setupUpArrow() {
        binding.ivUp.setOnClickListener {
            collapseBreadcrumbs()
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
    
    private fun openCustomizationModal(itemName: String) {
        // Create mock menu item for now
        val menuItem = MenuItem(
            id = "mock_$itemName",
            name = itemName,
            sizes = listOf("Small", "medium", "Large"),
            vegFlag = "Veg"
        )
        
        val bundle = Bundle().apply {
            putParcelable("menuItem", menuItem)
        }
        findNavController().navigate(R.id.itemCustomizeFragment, bundle)
    }
    
    private fun navigateToCategoryItems(categoryName: String) {
        val bundle = Bundle().apply {
            putString("categoryName", categoryName)
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

