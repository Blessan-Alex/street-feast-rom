package com.streatfeast.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class OrderCategoryItemsFragment : Fragment() {
    
    private var _binding: FragmentOrderCategoryItemsBinding? = null
    private val binding get() = _binding!!
    
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    
    private var categoryName: String = "Chinese"
    private var tableNumber: Int = 4
    private var orderType: OrderType = OrderType.DINE_IN
    
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
            tableNumber = args.getInt("tableNumber", 4)
            args.getString("orderType")?.let { typeString ->
                orderType = try {
                    OrderType.valueOf(typeString)
                } catch (e: IllegalArgumentException) {
                    OrderType.DINE_IN
                }
            }
        }
        
        setupTableHandle()
        setupSearch()
        setupRecyclerView()
        setupPreviewBar()
        setupBottomNavigation()
        loadMenuItems()
    }
    
    private fun setupTableHandle() {
        val tableHandleView = binding.tableHandle.root
        val tvTable = tableHandleView.findViewById<android.widget.TextView>(R.id.tvTable)
        tvTable?.text = "Table $tableNumber"
        
        // Handle down chevron click - navigate back
        val ivDown = tableHandleView.findViewById<android.widget.ImageView>(R.id.ivDown)
        ivDown?.setOnClickListener {
            findNavController().popBackStack()
        }
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
            val bundle = Bundle().apply {
                putInt("tableNumber", tableNumber)
                putString("orderType", orderType.name)
            }
            findNavController().navigate(R.id.previewOrderFragment, bundle)
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
    
    private fun loadMenuItems() {
        // Mock data for now - will be replaced with ViewModel/Repository later
        allItems.clear()
        allItems.addAll(generateMockItems())
        filteredItems.clear()
        filteredItems.addAll(allItems)
        adapter.notifyDataSetChanged()
        
        binding.tvCategory.text = categoryName
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
        val bottomSheet = ItemCustomizeBottomSheet.newInstance(item)
        bottomSheet.onItemAdded = {
            // Preview bar will update automatically via LiveData
        }
        bottomSheet.show(parentFragmentManager, "ItemCustomize")
    }
    
    private fun generateMockItems(): List<MenuItem> {
        return listOf(
            MenuItem(id = "1", name = "Ramen", sizes = listOf("Small", "medium", "Large"), vegFlag = "Veg"),
            MenuItem(id = "2", name = "Spring Roll", sizes = listOf("Small", "medium", "Large"), vegFlag = "Veg"),
            MenuItem(id = "3", name = "Dumplings", sizes = listOf("Small", "medium", "Large"), vegFlag = "NonVeg"),
            MenuItem(id = "4", name = "Fried Rice", sizes = listOf("Small", "medium", "Large"), vegFlag = "Both"),
            MenuItem(id = "5", name = "Noodles", sizes = listOf("Small", "medium", "Large"), vegFlag = "Veg"),
            MenuItem(id = "6", name = "Soup", sizes = listOf("Small", "medium", "Large"), vegFlag = "Veg"),
            MenuItem(id = "7", name = "Chicken", sizes = listOf("Small", "medium", "Large"), vegFlag = "NonVeg"),
            MenuItem(id = "8", name = "Beef", sizes = listOf("Small", "medium", "Large"), vegFlag = "NonVeg"),
            MenuItem(id = "9", name = "Pork", sizes = listOf("Small", "medium", "Large"), vegFlag = "NonVeg"),
            MenuItem(id = "10", name = "Tofu", sizes = listOf("Small", "medium", "Large"), vegFlag = "Veg"),
            MenuItem(id = "11", name = "Vegetables", sizes = listOf("Small", "medium", "Large"), vegFlag = "Veg"),
            MenuItem(id = "12", name = "Seafood", sizes = listOf("Small", "medium", "Large"), vegFlag = "NonVeg")
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

