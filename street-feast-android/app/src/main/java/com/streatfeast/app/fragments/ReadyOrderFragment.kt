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
import com.streatfeast.app.ui.AppBarController
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
    private var appBarController: AppBarController? = null
    private val seenOrderIds = mutableSetOf<String>()
    private var previousOrdersList: List<Order> = emptyList()
    private val deliveringOrderIds = mutableSetOf<String>()
    private var allReadyOrders: List<Order> = emptyList()
    private var filteredReadyOrders: List<Order> = emptyList()

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
        setupSearch()
        setupBottomNavigation()
        setupAppbarHandle()
        observeReadyOrders()
        observeMessages()

        viewModel.refresh()
    }

    private fun setupAppbar() {
        // btnClose removed from app bar
        // Users can use system back button instead
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
        adapter = ReadyOrderCardAdapter(
            onDeliverClick = { order ->
                deliveringOrderIds.add(order.id)
                // Notify adapter that loading state changed
                val position = adapter.currentList.indexOfFirst { it.id == order.id }
                if (position >= 0) {
                    adapter.notifyItemChanged(position)
                }
                viewModel.markDelivered(order.id)
            },
            isLoadingOrderId = { orderId -> deliveringOrderIds.contains(orderId) }
        )

        binding.rvReady.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReady.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterReadyOrders()
            }
        })
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
            // Update cached list and filter (badge removed)
            allReadyOrders = orders
            filterReadyOrders()
            previousOrdersList = orders
        }
    }

    private fun filterReadyOrders() {
        val term = binding.etSearch.text?.toString()?.lowercase()?.trim() ?: ""
        filteredReadyOrders = allReadyOrders.filter { order ->
            val tableMatch = order.tableNumber?.toString()?.contains(term) == true
            val orderNumMatch = order.orderNumber.toString().contains(term)
            val itemMatch = order.items.any { item ->
                val name = item.nameSnapshot.lowercase()
                val tip = item.chefTip.lowercase()
                name.contains(term) || tip.contains(term)
            }
            term.isBlank() || tableMatch || orderNumMatch || itemMatch
        }
        adapter.submitList(filteredReadyOrders)
    }
    
    // Notification badge removed (not used)

    private fun observeMessages() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
                // Clear loading state on error
                val clearedIds = deliveringOrderIds.toSet()
                deliveringOrderIds.clear()
                // Notify adapter of changes
                clearedIds.forEach { orderId ->
                    val position = adapter.currentList.indexOfFirst { it.id == orderId }
                    if (position >= 0) {
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
                // Clear loading state on success (order will be removed from list automatically)
                deliveringOrderIds.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

