package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.streatfeast.app.R
import com.streatfeast.app.databinding.FragmentOrderTypeBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderTypeFragment : Fragment() {
    
    private var _binding: FragmentOrderTypeBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderTypeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupStepper()
        setupButtons()
        setupAppbar()
        setupLogoutButton()
        setupBottomNavigation()
    }
    
    private fun setupStepper() {
        // Ensure only Type step is active for Screen 1
        // d1 is already active in XML, d2-d5 are inactive
        // v1 and v2 are GONE (no values shown yet)
    }
    
    private fun setupButtons() {
        binding.btnDineIn.setOnClickListener {
            navigateToWhere(OrderType.DINE_IN)
        }
        
        binding.btnParcel.setOnClickListener {
            navigateToWhere(OrderType.PARCEL)
        }
        
        binding.btnEatAway.setOnClickListener {
            navigateToWhere(OrderType.DELIVERY)
        }
    }
    
    private fun navigateToWhere(orderType: OrderType) {
        // Navigate to Screen 2 with order type
        val bundle = Bundle().apply {
            putString("orderType", orderType.name)
        }
        findNavController().navigate(R.id.orderWhereFragment, bundle)
    }
    
    private fun setupAppbar() {
        binding.appbar.btnClose.setOnClickListener {
            // Navigate back or clear draft
            if (!findNavController().popBackStack()) {
                // If we can't pop, navigate to a default screen
                // For now, just finish the activity or navigate to ready orders
                activity?.finish()
            }
        }
        
        // Update date if needed (can be done programmatically)
        // For now, date is set in XML
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
                    android.util.Log.e("OrderTypeFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    try {
                        ServiceLocator.clear()
                    } catch (clearError: Exception) {
                        android.util.Log.e("OrderTypeFragment", "Error clearing ServiceLocator", clearError)
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

