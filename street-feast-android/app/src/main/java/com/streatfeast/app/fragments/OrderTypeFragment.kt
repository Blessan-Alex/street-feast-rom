package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.streatfeast.app.R
import com.streatfeast.app.databinding.FragmentOrderTypeBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.OrderType
import com.streatfeast.app.navigation.OrderNavArgs
import com.streatfeast.app.viewmodels.AuthViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderTypeFragment : Fragment() {
    
    private var _binding: FragmentOrderTypeBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private var dateUpdateJob: Job? = null
    
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
            navigateToWhere(OrderType.EAT_AWAY)
        }
    }
    
    private fun navigateToWhere(orderType: OrderType) {
        val args = OrderNavArgs(orderType = orderType)
        if (orderType == OrderType.PARCEL) {
            // Parcel skips table selection
            val parcelArgs = args.copy(tableNumber = 0, showHeader = false)
            findNavController().navigate(
                R.id.orderItemFragment,
                parcelArgs.toBundle()
            )
        } else {
            findNavController().navigate(R.id.orderWhereFragment, args.toBundle())
        }
    }
    
    private fun setupAppbar() {
        // Hide back button since this is the home page - users can't go back from here
        val navBack = binding.appbar.root.findViewById<View>(R.id.ivNavBack)
        navBack?.visibility = View.GONE
        
        // Setup real-time date
        setupDate()
    }
    
    private fun setupDate() {
        val tvDate = binding.appbar.root.findViewById<TextView>(R.id.tvDate)
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        
        // Update date immediately
        fun updateDate() {
            tvDate?.text = dateFormat.format(Date())
        }
        
        updateDate()
        
        // Update date every minute
        dateUpdateJob = lifecycleScope.launch {
            while (true) {
                delay(60000) // 1 minute
                if (isAdded && view != null) {
                    updateDate()
                }
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
        dateUpdateJob?.cancel()
        dateUpdateJob = null
        _binding = null
    }
}

