package com.streatfeast.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.MenuItemAdapter
import com.streatfeast.app.databinding.FragmentOrderCategoryItemsBinding
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import com.streatfeast.app.viewmodels.MenuViewModel
import com.streatfeast.app.viewmodels.MenuViewModelFactory
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.storage.StreetFeastDatabase
import com.streatfeast.app.utils.Constants
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.streatfeast.app.navigation.OrderNavArgs

class OrderCategoryItemsFragment : Fragment() {
    
    private var _binding: FragmentOrderCategoryItemsBinding? = null
    private val binding get() = _binding!!
    
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    private val menuViewModel: MenuViewModel by viewModels {
        val repository = ServiceLocator.provideMenuRepository(requireContext().applicationContext)
        val db = StreetFeastDatabase.getInstance(requireContext())
        val localDataSource = com.streatfeast.app.storage.MenuLocalDataSource(db)
        MenuViewModelFactory(repository, localDataSource, Constants.DEFAULT_STORE_ID)
    }
    
    private var categoryName: String = "Chinese"
    private var categoryId: String? = null
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    private var showHeader: Boolean = false
    private var licensePlate: String? = null
    
    private lateinit var adapter: MenuItemAdapter
    private val allItems = mutableListOf<MenuItem>()
    private val filteredItems = mutableListOf<MenuItem>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderCategoryItemsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get arguments
        arguments?.let { args ->
            categoryName = args.getString("categoryName", "Chinese")
            categoryId = args.getString("categoryId")
            tableNumber = args.getInt("tableNumber", 4)
            args.getString("orderType")?.let { typeString ->
                orderType = try {
                    OrderType.valueOf(typeString)
                } catch (e: IllegalArgumentException) {
                    OrderType.DINE_IN
                }
            }
            showHeader = args.getBoolean("showHeader", false)
            licensePlate = args.getString("licensePlate")
        }
        
        // When header is shown, pad the appbar for status bar insets like OrderTypeFragment
        if (showHeader) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.appbar.root) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, sys.top + v.paddingTop, v.paddingRight, v.paddingBottom)
                insets
            }
        }

        setupScreenState()
        setupTableHandle()
        setupStepper()
        setupUpArrow()
        setupSearch()
        setupRecyclerView()
        setupPreviewBar()
        setupBottomNavigation()
        setupAppbar()
        loadMenuItems()
    }
    
    private fun setupScreenState() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root as androidx.constraintlayout.widget.ConstraintLayout)
        
        if (showHeader) {
            // Update searchWrap constraint to be below ivUp (apply constraint first)
            constraintSet.clear(R.id.searchWrap, ConstraintSet.TOP)
            constraintSet.connect(R.id.searchWrap, ConstraintSet.TOP, R.id.ivUp, ConstraintSet.BOTTOM, 8)
            constraintSet.applyTo(binding.root as androidx.constraintlayout.widget.ConstraintLayout)
            
            // Show breadcrumbs, hide table handle
            binding.appbar.root.visibility = View.VISIBLE
            binding.tvCreateOrder.visibility = View.VISIBLE
            binding.stepper.root.visibility = View.VISIBLE
            binding.ivUp.visibility = View.VISIBLE
            binding.tableHandle.root.visibility = View.GONE
        } else {
            // Update searchWrap constraint to be below tableHandle (apply constraint first)
            constraintSet.clear(R.id.searchWrap, ConstraintSet.TOP)
            constraintSet.connect(R.id.searchWrap, ConstraintSet.TOP, R.id.tableHandle, ConstraintSet.BOTTOM, 8)
            constraintSet.applyTo(binding.root as androidx.constraintlayout.widget.ConstraintLayout)
            
            // Hide breadcrumbs, show table handle
            binding.appbar.root.visibility = View.GONE
            binding.tvCreateOrder.visibility = View.GONE
            binding.stepper.root.visibility = View.GONE
            binding.ivUp.visibility = View.GONE
            binding.tableHandle.root.visibility = View.VISIBLE
        }
    }
    
    private fun setupTableHandle() {
        val tableHandleView = binding.tableHandle.root
        val tvTable = tableHandleView.findViewById<android.widget.TextView>(R.id.tvTable)
        tvTable?.text = "Table $tableNumber"
        
        // Handle down chevron click - show breadcrumbs (don't navigate back)
        val ivDown = tableHandleView.findViewById<android.widget.ImageView>(R.id.ivDown)
        ivDown?.setOnClickListener {
            // Show breadcrumbs by navigating to same fragment with showHeader=true
            val bundle = Bundle().apply {
                putString("categoryName", categoryName)
                putInt("tableNumber", tableNumber)
                putString("orderType", orderType.name)
                putBoolean("showHeader", true)
            }
            findNavController().navigate(R.id.orderCategoryItemsFragment, bundle)
        }
    }
    
    private fun setupStepper() {
        if (!showHeader) return // Only update stepper when breadcrumbs are shown
        
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
    
    private fun setupUpArrow() {
        // Remove the click listener - only keep swipe gesture
        binding.ivUp.setOnClickListener(null)
        
        // Keep only the swipe gesture detector
        binding.ivUp.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0f
            private val swipeThreshold = 100f // Minimum distance for swipe
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        return false // Don't consume DOWN event
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = startY - event.y // Positive if swiped up
                        if (deltaY > swipeThreshold) {
                            // Swiped up - collapse breadcrumbs
                            collapseBreadcrumbs()
                            return true
                        }
                        return false
                    }
                }
                return false
            }
        })
    }
    
    private fun collapseBreadcrumbs() {
        // Navigate back to same fragment with showHeader=false
        val bundle = Bundle().apply {
            putString("categoryName", categoryName)
            putInt("tableNumber", tableNumber)
            putString("orderType", orderType.name)
            putBoolean("showHeader", false)
        }
        findNavController().navigate(R.id.orderCategoryItemsFragment, bundle)
    }
    
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterItems(s?.toString() ?: "")
            }
        })
    }
    
    private fun setupRecyclerView() {
        adapter = MenuItemAdapter(
            items = filteredItems,
            onItemClick = { item ->
                openCustomizationModal(item)
            },
            onAddClick = { item ->
                openCustomizationModal(item)
            }
        )
        
        binding.rvMenu.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMenu.adapter = adapter
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
            val navArgs = OrderNavArgs(
                orderType = orderType,
                tableNumber = tableNumber,
                licensePlate = licensePlate,
                showHeader = false
            )
            findNavController().navigate(R.id.previewOrderFragment, navArgs.toBundle())
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
    
    private fun setupAppbar() {
        val navBack = binding.appbar.root.findViewById<View>(R.id.ivNavBack)
        navBack?.setOnClickListener {
            // Navigate directly to orderItemFragment
            val bundle = Bundle().apply {
                putString("orderType", orderType.name)
                putInt("tableNumber", tableNumber)
                putBoolean("showHeader", showHeader) // Keep current state
            }
            findNavController().navigate(R.id.orderItemFragment, bundle)
        }
    }
    
    private fun loadMenuItems() {
        binding.tvCategory.text = categoryName
        
        // Load items from MenuViewModel based on categoryId
        if (categoryId != null) {
            menuViewModel.getItemsByCategory(categoryId!!).observe(viewLifecycleOwner) { items ->
                allItems.clear()
                allItems.addAll(items)
                filterItems(binding.etSearch.text?.toString() ?: "")
            }
        } else {
            // Fallback: find category by name if categoryId is not provided
            menuViewModel.categories.observe(viewLifecycleOwner) { categories ->
                val category = categories.find { it.name == categoryName }
                if (category != null) {
                    menuViewModel.getItemsByCategory(category.id).observe(viewLifecycleOwner) { items ->
                        allItems.clear()
                        allItems.addAll(items)
                        filterItems(binding.etSearch.text?.toString() ?: "")
                    }
                } else {
                    // If category not found, show empty list
                    allItems.clear()
                    filterItems(binding.etSearch.text?.toString() ?: "")
                }
            }
        }
    }
    
    private fun filterItems(searchTerm: String) {
        filteredItems.clear()
        if (searchTerm.isBlank()) {
            filteredItems.addAll(allItems)
        } else {
            val term = searchTerm.lowercase()
            filteredItems.addAll(allItems.filter {
                it.name.lowercase().contains(term) ||
                it.description.lowercase().contains(term)
            })
        }
        adapter.notifyDataSetChanged()
    }
    
    private fun openCustomizationModal(item: MenuItem) {
        val bundle = Bundle().apply {
            putParcelable("menuItem", item)
        }
        findNavController().navigate(R.id.itemCustomizeFragment, bundle)
    }
    
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

