package com.streatfeast.app.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.streatfeast.app.R
import com.streatfeast.app.adapters.ReadyOrderCardAdapter
import com.streatfeast.app.databinding.FragmentReadyOrderBinding
import com.streatfeast.app.di.ServiceLocator
import com.streatfeast.app.models.Order
import com.streatfeast.app.viewmodels.AuthViewModel
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ReadyOrderFragment : Fragment() {

    private var _binding: FragmentReadyOrderBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val viewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }

    private lateinit var adapter: ReadyOrderCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadyOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppbar()
        setupLogoutButton()
        setupRecyclerView()
        setupBottomNavigation()
        observeReadyOrders()
        observeMessages()

        viewModel.refresh()
    }

    private fun setupAppbar() {
        val appbarView = binding.appbar.root
        val btnClose = appbarView.findViewById<View>(R.id.btnClose)
        btnClose?.setOnClickListener {
            // Navigate back or close
            findNavController().popBackStack()
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
                    android.util.Log.e("ReadyOrderFragment", "Error during logout", e)
                    // Even if there's an error, try to clear
                    try {
                        ServiceLocator.clear()
                    } catch (clearError: Exception) {
                        android.util.Log.e("ReadyOrderFragment", "Error clearing ServiceLocator", clearError)
                    }
                }
            }
            // MainActivity will handle navigation when currentUser becomes null
        }
    }

    private fun setupRecyclerView() {
        adapter = ReadyOrderCardAdapter { order ->
            viewModel.markDelivered(order.id)
        }

        binding.rvReady.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReady.adapter = adapter
    }

    private fun setupBottomNavigation() {
        val bottomNavView = binding.bottomNav.root
        val navNewOrder = bottomNavView.findViewById<ViewGroup>(R.id.navNewOrder)
        val navGivenOrder = bottomNavView.findViewById<ViewGroup>(R.id.navGivenOrder)

        navNewOrder?.setOnClickListener {
            findNavController().navigate(R.id.orderTypeFragment)
        }

        navGivenOrder?.setOnClickListener {
            findNavController().navigate(R.id.givenOrderFragment)
        }
    }

    private fun observeReadyOrders() {
        viewModel.readyOrders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

