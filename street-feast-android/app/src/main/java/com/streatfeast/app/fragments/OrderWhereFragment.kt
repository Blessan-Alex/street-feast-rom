package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.streatfeast.app.R
import com.streatfeast.app.databinding.FragmentOrderWhereBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderWhereFragment : Fragment() {
    
    private var _binding: FragmentOrderWhereBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private var selectedTableNumber: Int = 4 // Default to table 4 as shown in screenshot
    private var orderType: OrderType = OrderType.DINE_IN
    
    
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
        
        // Get order type from arguments
        arguments?.getString("orderType")?.let { typeString ->
            orderType = try {
                OrderType.valueOf(typeString)
            } catch (e: IllegalArgumentException) {
                OrderType.DINE_IN // Default fallback
            }
        }
        
        setupStepper()
        setupTableButtons()
        setupGoBackButton()
        setupAppbar()
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
            OrderType.DINE_IN -> "Din in"
            OrderType.PARCEL -> "Parcel"
            OrderType.DELIVERY -> "Eat away"
        }
    }
    
    private fun setupTableButtons() {
        val tableButtons = listOf(
            binding.t1 to 1,
            binding.t2 to 2,
            binding.t3 to 3,
            binding.t4 to 4,
            binding.t5 to 5,
            binding.t6 to 6,
            binding.t7 to 7
        )
        
        // Set initial selection (table 4)
        updateTableSelection(selectedTableNumber)
        
        // Set click listeners
        tableButtons.forEach { (cardView, tableNumber) ->
            cardView.setOnClickListener {
                selectedTableNumber = tableNumber
                updateTableSelection(tableNumber)
                
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
    
    private fun updateTableSelection(selectedTable: Int) {
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
            val textView = cardView.getChildAt(0) as? android.widget.TextView
            if (tableNumber == selectedTable) {
                // Selected: blue background, white text
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_primary)
                )
                textView?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_white)
                )
                textView?.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // Unselected: grey background, dark text
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_surface_soft2)
                )
                textView?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.sf_text_primary)
                )
                textView?.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }
    
    private fun setupGoBackButton() {
        binding.btnGoBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    
    private fun setupAppbar() {
        binding.appbar.btnClose.setOnClickListener {
            // Navigate back or clear draft
            if (!findNavController().popBackStack()) {
                // If we can't pop, navigate to a default screen
                activity?.finish()
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
        super.onDestroyView()
        _binding = null
    }
}

