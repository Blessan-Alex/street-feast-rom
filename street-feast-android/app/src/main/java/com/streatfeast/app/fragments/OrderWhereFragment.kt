package com.streatfeast.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.streatfeast.app.R
import com.streatfeast.app.databinding.FragmentOrderWhereBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.repositories.SupabaseOrderRepository
import com.streatfeast.app.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderWhereFragment : Fragment() {
    
    private var _binding: FragmentOrderWhereBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private var selectedTableNumber: Int? = null // No default selection
    private var orderType: OrderType = OrderType.DINE_IN
    private var licensePlate: String = ""
    private var occupiedTables: List<Int> = emptyList()
    private lateinit var repository: SupabaseOrderRepository
    private var tableStatusCallback: ((String, Int?) -> Unit)? = null
    
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderWhereBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize repository after fragment is attached to context
        repository = ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        
        // Get order type from arguments
        arguments?.getString("orderType")?.let { typeString ->
            orderType = try {
                OrderType.valueOf(typeString)
            } catch (e: IllegalArgumentException) {
                OrderType.DINE_IN // Default fallback
            }
        }
        
        setupStepper()
        
        // Handle different order types
        when (orderType) {
            OrderType.DINE_IN -> {
                setupTableButtons()
                fetchOccupiedTables()
                setupTableStatusRealtime()
            }
            OrderType.EAT_AWAY -> {
                setupLicensePlateInput()
            }
            OrderType.PARCEL -> {
                // Navigate directly to item selection for PARCEL
                val bundle = Bundle().apply {
                    putString("orderType", orderType.name)
                    putInt("tableNumber", 0)  // Use 0 for non-DINE_IN orders
                    putBoolean("showHeader", false)
                }
                findNavController().navigate(R.id.orderItemFragment, bundle)
                return
            }
        }
        
        setupGoBackButton()
        setupAppbar()
        setupRefreshButton()
        setupLogoutButton()
        setupBottomNavigation()
    }
    
    private fun setupStepper() {
        // Update stepper state for Screen 2:
        // - d1 (Type) is already active in XML
        // - d2 (Where) should be active
        // - v1 should show order type text
        
        val stepperView = binding.stepper.root
        val d2 = stepperView.findViewById<View>(R.id.d2)
        val v1 = stepperView.findViewById<android.widget.TextView>(R.id.v1)
        
        // Set d2 to active
        d2?.background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_step_dot_active)
        
        // Set v1 to visible with order type text
        v1?.visibility = View.VISIBLE
        v1?.text = getOrderTypeDisplayText(orderType)
    }
    
    private fun getOrderTypeDisplayText(type: OrderType): String {
        return when (type) {
            OrderType.DINE_IN -> "Dine in"
            OrderType.PARCEL -> "Parcel"
            OrderType.EAT_AWAY -> "Eat away"
        }
    }
    
    private fun fetchOccupiedTables() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val storeId = repository.getStoreIdForFragment()
                repository.getOccupiedTables(storeId).getOrElse { emptyList() }
            }.onSuccess { tables ->
                occupiedTables = tables
                android.util.Log.d("OrderWhereFragment", "Fetched occupied tables: $occupiedTables")
                // Refresh UI if table buttons are already set up
                // Check if view is still available before accessing binding
                if (view != null && _binding != null && binding.tableFlow.visibility == View.VISIBLE) {
                    updateTableButtons()
                }
            }.onFailure { e ->
                android.util.Log.e("OrderWhereFragment", "Failed to fetch occupied tables", e)
            }
        }
    }
    
    private fun setupTableStatusRealtime() {
        tableStatusCallback = callback@ { orderId, orderNumber ->
            if (!isAdded || view == null || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                android.util.Log.d("OrderWhereFragment", "Ignoring table refresh; view not active")
                return@callback
            }
            android.util.Log.d("OrderWhereFragment", "Order changed (ID: $orderId, #$orderNumber), refreshing occupied tables")
            fetchOccupiedTables()
        }
        tableStatusCallback?.let { cb ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.startRealtime(viewLifecycleOwner.lifecycleScope, cb)
            }
        }
    }
    
    private fun updateTableButtons() {
        if (binding.tableFlow.visibility != View.VISIBLE) return
        
        val tableButtons = listOf(
            binding.t1 to 1,
            binding.t2 to 2,
            binding.t3 to 3,
            binding.t4 to 4,
            binding.t5 to 5,
            binding.t6 to 6,
            binding.t7 to 7
        )
        
        tableButtons.forEach { (cardView, tableNumber) ->
            val isOccupied = occupiedTables.contains(tableNumber)
            updateTableButtonState(cardView, tableNumber, isOccupied, tableNumber == selectedTableNumber)
        }
    }
    
    private fun setupTableButtons() {
        // Show table buttons
        binding.tableFlow.visibility = View.VISIBLE
        
        // Update label text
        val tvTableNo = binding.root.findViewById<TextView>(R.id.tvTableNo)
        tvTableNo?.text = "Table no."
        
        val tableButtons = listOf(
            binding.t1 to 1,
            binding.t2 to 2,
            binding.t3 to 3,
            binding.t4 to 4,
            binding.t5 to 5,
            binding.t6 to 6,
            binding.t7 to 7
        )
        
        // Don't set initial selection - user must explicitly choose
        
        // Set click listeners
        tableButtons.forEach { (cardView, tableNumber) ->
            val isOccupied = occupiedTables.contains(tableNumber)
            
            // Update visual state
            updateTableButtonState(cardView, tableNumber, isOccupied, tableNumber == selectedTableNumber)
            
            cardView.setOnClickListener {
                if (!isOccupied) {
                    selectedTableNumber = tableNumber
                    updateTableSelection(selectedTableNumber)
                    
                    // Navigate to Screen 3 (compact state)
                    val bundle = Bundle().apply {
                        putString("orderType", orderType.name)
                        putInt("tableNumber", tableNumber)
                        putBoolean("showHeader", false)
                    }
                    findNavController().navigate(R.id.orderItemFragment, bundle)
                }
            }
        }
    }
    
    private fun setupLicensePlateInput() {
        // Hide table buttons, show license plate input
        binding.tableFlow.visibility = View.GONE
        
        // Update label text
        val tvTableNo = binding.root.findViewById<TextView>(R.id.tvTableNo)
        tvTableNo?.text = "License Plate"
        
        // Create license plate input container
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = R.id.tvTableNo
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = resources.getDimensionPixelSize(R.dimen.sf_pad_screen_h)
                marginEnd = resources.getDimensionPixelSize(R.dimen.sf_pad_screen_h)
                topMargin = (18 * resources.displayMetrics.density).toInt()
            }
            setPadding(
                resources.getDimensionPixelSize(R.dimen.sf_pad_screen_h),
                0,
                resources.getDimensionPixelSize(R.dimen.sf_pad_screen_h),
                0
            )
        }
        
        val input = EditText(requireContext()).apply {
            hint = "Enter 4-digit license plate"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            textSize = 18f
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt()
            )
            background = ContextCompat.getDrawable(requireContext(), R.drawable.sf_bg_chip)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // Add text watcher to validate and navigate
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString() ?: ""
                    val digits = text.filter { it.isDigit() }.take(4)
                    if (digits != text) {
                        // keep input constrained to digits
                        val editText = this@apply
                        editText.setText(digits)
                        editText.setSelection(digits.length)
                    }
                    licensePlate = digits
                    
                    // Only navigate when exactly 4 digits are entered
                    if (digits.length == 4) {
                        // Validate it's all digits
                        if (digits.matches(Regex("^[0-9]{4}$"))) {
                            // Use post to ensure navigation happens after text change is complete
                            view?.post {
                                val bundle = Bundle().apply {
                                    putString("orderType", orderType.name)
                                    putString("licensePlate", licensePlate)
                                    putInt("tableNumber", 0)  // Use 0 for EAT_AWAY orders
                                    putBoolean("showHeader", false)
                                }
                                findNavController().navigate(R.id.orderItemFragment, bundle)
                            }
                        }
                    }
                }
            })
        }
        
        container.addView(input)
        (binding.root as? ConstraintLayout)?.addView(container)
    }
    
    private fun updateTableSelection(selectedTable: Int?) {
        val tableButtons = listOf(
            binding.t1 to 1,
            binding.t2 to 2,
            binding.t3 to 3,
            binding.t4 to 4,
            binding.t5 to 5,
            binding.t6 to 6,
            binding.t7 to 7
        )
        
        tableButtons.forEach { (cardView, tableNumber) ->
            val isOccupied = occupiedTables.contains(tableNumber)
            updateTableButtonState(cardView, tableNumber, isOccupied, tableNumber == selectedTable)
        }
    }
    
    private fun updateTableButtonState(cardView: View, tableNumber: Int, isOccupied: Boolean, isSelected: Boolean) {
        val card = cardView as? CardView
        // CardView extends FrameLayout (ViewGroup), so we can safely cast to ViewGroup
        val viewGroup = cardView as? ViewGroup
        val textView = viewGroup?.getChildAt(0) as? TextView
        textView?.isSingleLine = true
        textView?.ellipsize = TextUtils.TruncateAt.END
        textView?.textSize = 12f
        
        when {
            isOccupied -> {
                // Occupied: gray background, gray text, disabled
                card?.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_surface_soft2)
                )
                textView?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_text_secondary)
                )
                // Use single-line occupied label to avoid clipping on narrow widths
                textView?.text = "Table $tableNumber · Occupied"
                textView?.setTypeface(null, android.graphics.Typeface.NORMAL)
                cardView.isEnabled = false
                cardView.alpha = 0.6f
            }
            isSelected -> {
                // Selected: blue background, white text
                card?.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_primary)
                )
                textView?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_white)
                )
                textView?.text = tableNumber.toString()
                textView?.setTypeface(null, android.graphics.Typeface.BOLD)
                cardView.isEnabled = true
                cardView.alpha = 1.0f
            }
            else -> {
                // Available: white/gray background, dark text
                card?.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_surface_soft2)
                )
                textView?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_text_primary)
                )
                textView?.text = tableNumber.toString()
                textView?.setTypeface(null, android.graphics.Typeface.NORMAL)
                cardView.isEnabled = true
                cardView.alpha = 1.0f
            }
        }
    }
    
    private fun setupGoBackButton() {
        binding.btnGoBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    
    private fun setupAppbar() {
        // btnClose removed from app bar
        // Users can use system back button instead
    }
    
    private fun setupRefreshButton() {
        // Only show refresh button for DineIn order type (where tables are shown)
        val refreshButton = binding.appbar.root.findViewById<ViewGroup>(R.id.btnRefreshTables)
        val refreshIcon = binding.appbar.root.findViewById<android.widget.ImageView>(R.id.ivRefreshTables)
        
        if (orderType == OrderType.DINE_IN) {
            refreshButton?.visibility = View.VISIBLE
            
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
                    
                    android.widget.Toast.makeText(requireContext(), "Refreshing table status...", android.widget.Toast.LENGTH_SHORT).show()
                    fetchOccupiedTables()
                }
            }
        } else {
            refreshButton?.visibility = View.GONE
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
                    android.util.Log.e("OrderWhereFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    try {
                        ServiceLocator.clear()
                    } catch (clearError: Exception) {
                        android.util.Log.e("OrderWhereFragment", "Error clearing ServiceLocator", clearError)
                    }
                }
            }
            // MainActivity will handle navigation when currentUser becomes null
        }
    }
    
    private fun setupBottomNavigation() {
        // Access the included bottom nav views
        val bottomNavView = binding.bottomNav.root
        val navReadyOrder = bottomNavView.findViewById<ViewGroup>(R.id.navReadyOrder)
        val navGivenOrder = bottomNavView.findViewById<ViewGroup>(R.id.navGivenOrder)
        
        navReadyOrder?.setOnClickListener {
            findNavController().navigate(R.id.readyOrderFragment)
        }
        
        navGivenOrder?.setOnClickListener {
            findNavController().navigate(R.id.givenOrderFragment)
        }
        
        // New Order is already selected (shown as blue pill)
        // No action needed for navNewOrder click
    }
    
    override fun onDestroyView() {
        tableStatusCallback?.let { cb ->
            lifecycleScope.launch {
                runCatching { repository.removeCallback(cb) }
            }
        }
        super.onDestroyView()
        _binding = null
    }
}

