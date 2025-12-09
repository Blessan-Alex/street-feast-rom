package com.streatfeast.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.streatfeast.app.R
import com.streatfeast.app.databinding.FragmentItemCustomizeBinding
import com.streatfeast.app.databinding.SheetItemCustomizeBinding
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.viewmodels.OrderDraftViewModel
import com.streatfeast.app.viewmodels.OrdersViewModel
import com.streatfeast.app.viewmodels.OrdersViewModelFactory
import com.streatfeast.app.di.ServiceLocator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ItemCustomizeBottomSheet : Fragment() {
    
    private var _binding: FragmentItemCustomizeBinding? = null
    private val binding get() = _binding!!
    
    // Access the included sheet content views directly
    private val sheetBinding: SheetItemCustomizeBinding by lazy {
        val includedView = binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.sheetContent)
        SheetItemCustomizeBinding.bind(includedView)
    }
    
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    private val ordersViewModel: OrdersViewModel by viewModels {
        OrdersViewModelFactory(
            ServiceLocator.provideOrderRepository(requireContext().applicationContext)
        )
    }
    
    private var currentStep = 1
    private var selectedSize: String? = null
    private var chefTip: String = ""
    private var quantity: Int = 1
    private var menuItem: MenuItem? = null
    private var orderItem: OrderItem? = null // For edit mode
    private var isEditMode = false
    private var isEditingOrder = false // Flag to indicate we're editing an order item (not draft)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get menu item and order item from arguments
        arguments?.let { args ->
            menuItem = args.getParcelable("menuItem")
            orderItem = args.getParcelable("orderItem")
            isEditMode = orderItem != null
            isEditingOrder = args.getBoolean("isEditingOrder", false)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemCustomizeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInitialState()
        setupStep1()
        setupStep2()
        setupStep3()
        setupButtons()
    }
    
    private fun setupInitialState() {
        if (isEditMode && orderItem != null) {
            // Edit mode: pre-populate with existing OrderItem data
            val item = orderItem!!
            sheetBinding.tvSheetTitle.text = item.nameSnapshot
            
            // Pre-populate size (only if present); avoid forcing defaults
            selectedSize = item.size
            when (item.size?.lowercase()) {
                "small" -> sheetBinding.rbSmall.isChecked = true
                "medium" -> sheetBinding.rbMedium.isChecked = true
                "large" -> sheetBinding.rbLarge.isChecked = true
                else -> {
                    sheetBinding.rbSmall.isChecked = false
                    sheetBinding.rbMedium.isChecked = false
                    sheetBinding.rbLarge.isChecked = false
                }
            }
            
            // Pre-populate chef tip
            chefTip = item.chefTip
            sheetBinding.etChefTip.setText(item.chefTip)
            
            // Pre-populate quantity
            quantity = item.qty
            sheetBinding.tvQty.text = item.qty.toString()
            
            // Pre-populate suggestion chips
            if (item.chefTip.contains("spicy", ignoreCase = true)) {
                sheetBinding.chipSpicy.isChecked = true
                sheetBinding.chipSpicy.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_primary)
                sheetBinding.chipSpicy.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_white))
            }
            if (item.chefTip.contains("sour", ignoreCase = true)) {
                sheetBinding.chipSour.isChecked = true
                sheetBinding.chipSour.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_primary)
                sheetBinding.chipSour.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_white))
            }
        } else {
            // New item mode: use MenuItem
            menuItem?.let { item ->
                sheetBinding.tvSheetTitle.text = item.name
                // Do not preselect size; wait for user choice
                selectedSize = null
                sheetBinding.rgSize.clearCheck()
            }
        }
        
        updateStepVisibility()
        updateStepIndicator()
        updateChipStyles()
    }
    
    private fun setupStep1() {
        // Set up individual listeners to ensure single selection
        sheetBinding.rbSmall.setOnClickListener {
            sheetBinding.rbSmall.isChecked = true
            sheetBinding.rbMedium.isChecked = false
            sheetBinding.rbLarge.isChecked = false
            selectedSize = "Small"
            updateStepSummary()
        }
        
        sheetBinding.rbMedium.setOnClickListener {
            sheetBinding.rbSmall.isChecked = false
            sheetBinding.rbMedium.isChecked = true
            sheetBinding.rbLarge.isChecked = false
            selectedSize = "Medium"
            updateStepSummary()
        }
        
        sheetBinding.rbLarge.setOnClickListener {
            sheetBinding.rbSmall.isChecked = false
            sheetBinding.rbMedium.isChecked = false
            sheetBinding.rbLarge.isChecked = true
            selectedSize = "Large"
            updateStepSummary()
        }
        
        // Also use RadioGroup listener as backup
        sheetBinding.rgSize.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSmall -> selectedSize = "Small"
                R.id.rbMedium -> selectedSize = "Medium"
                R.id.rbLarge -> selectedSize = "Large"
            }
            updateStepSummary()
        }
        
        // Do not auto-select by default; wait for explicit user choice
    }
    
    private fun setupStep2() {
        sheetBinding.chipSour.setOnClickListener {
            if (sheetBinding.chipSour.isChecked) {
                applyChefTipPreset("Make it extra sour")
            }
            updateChipStyles()
            updateStepSummary()
        }
        
        sheetBinding.chipSpicy.setOnClickListener {
            if (sheetBinding.chipSpicy.isChecked) {
                applyChefTipPreset("Make it extra spicy")
            }
            updateChipStyles()
            updateStepSummary()
        }

        sheetBinding.chipLessSalty.setOnClickListener {
            if (sheetBinding.chipLessSalty.isChecked) {
                applyChefTipPreset("Make it less salty")
            }
            updateChipStyles()
            updateStepSummary()
        }

        sheetBinding.chipCrispier.setOnClickListener {
            if (sheetBinding.chipCrispier.isChecked) {
                applyChefTipPreset("Make it crispier")
            }
            updateChipStyles()
            updateStepSummary()
        }

        sheetBinding.chipNoOnions.setOnClickListener {
            if (sheetBinding.chipNoOnions.isChecked) {
                applyChefTipPreset("No onions please")
            }
            updateChipStyles()
            updateStepSummary()
        }

        sheetBinding.chipExtraSauce.setOnClickListener {
            if (sheetBinding.chipExtraSauce.isChecked) {
                applyChefTipPreset("Add extra sauce")
            }
            updateChipStyles()
            updateStepSummary()
        }

        sheetBinding.chipMildSpice.setOnClickListener {
            if (sheetBinding.chipMildSpice.isChecked) {
                applyChefTipPreset("Make it mild spice")
            }
            updateChipStyles()
            updateStepSummary()
        }
        
        sheetBinding.etChefTip.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                chefTip = s?.toString()?.trim() ?: ""
                updateStepSummary()
            }
        })
        
        sheetBinding.etChefTip.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearChipSelection()
                updateChipStyles()
            }
        }
    }
    
    private fun setupStep3() {
        sheetBinding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                sheetBinding.tvQty.text = quantity.toString()
                updateStepSummary()
            }
        }
        
        sheetBinding.btnPlus.setOnClickListener {
            if (quantity < 99) {
                quantity++
                sheetBinding.tvQty.text = quantity.toString()
                updateStepSummary()
            }
        }
    }
    
    private fun setupButtons() {
        sheetBinding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }
        
        sheetBinding.btnBack.setOnClickListener {
            handleBack()
        }
        
        sheetBinding.btnNext.setOnClickListener {
            handleNext()
        }
    }
    
    private fun handleBack() {
        if (currentStep > 1) {
            currentStep--
            updateStepVisibility()
            updateStepIndicator()
            updateButtonConstraints()
        } else {
            findNavController().popBackStack()
        }
    }
    
    private fun handleNext() {
        when (currentStep) {
            1 -> {
                // Validate size selection
                if (selectedSize == null && menuItem?.sizes?.isNotEmpty() == true) {
                    return // Don't advance if no size selected
                }
                currentStep = 2
            }
            2 -> {
                // Get chef tip from EditText
                chefTip = sheetBinding.etChefTip.text.toString().trim()
                currentStep = 3
            }
            3 -> {
                // Add item to draft and navigate back
                addItemToDraft()
                findNavController().popBackStack()
            }
        }
        
        updateStepVisibility()
        updateStepIndicator()
        updateButtonConstraints()
    }
    
    private fun addItemToDraft() {
        if (isEditingOrder && orderItem != null) {
            // Update order item directly (not draft)
            val item = orderItem!!
            lifecycleScope.launch {
                ordersViewModel.updateOrderItem(
                    itemId = item.id,
                    quantity = quantity,
                    size = selectedSize,
                    chefTip = chefTip
                )
            }
            // Navigate back after update
            findNavController().popBackStack()
        } else if (isEditMode && orderItem != null) {
            // Update existing item in draft
            val item = orderItem!!
            draftViewModel.updateDraftItem(item.id) { currentItem ->
                currentItem.copy(
                    size = selectedSize,
                    qty = quantity,
                    chefTip = chefTip
                )
            }
        } else {
            // Add new item to draft
            menuItem?.let { item ->
                val newOrderItem = OrderItem(
                    itemId = item.id,
                    nameSnapshot = item.name,
                    size = selectedSize,
                    vegFlagSnapshot = item.vegFlag,
                    qty = quantity,
                    chefTip = chefTip
                )
                
                draftViewModel.addDraftItem(newOrderItem)
            }
        }
        // ViewModel LiveData will automatically update observers
    }
    
    private fun updateStepVisibility() {
        when (currentStep) {
            1 -> {
                sheetBinding.tvStepTitle1.visibility = View.VISIBLE
                sheetBinding.amountBox.visibility = View.VISIBLE
                sheetBinding.stepChefTip.visibility = View.GONE
                sheetBinding.stepQty.visibility = View.GONE
            }
            2 -> {
                sheetBinding.tvStepTitle1.visibility = View.GONE
                sheetBinding.amountBox.visibility = View.GONE
                sheetBinding.stepChefTip.visibility = View.VISIBLE
                sheetBinding.stepQty.visibility = View.GONE
            }
            3 -> {
                sheetBinding.tvStepTitle1.visibility = View.GONE
                sheetBinding.amountBox.visibility = View.GONE
                sheetBinding.stepChefTip.visibility = View.GONE
                sheetBinding.stepQty.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateStepIndicator() {
        // Update step label colors
        sheetBinding.l1.setTextColor(ContextCompat.getColor(requireContext(),
            if (currentStep == 1) R.color.sf_text_primary else R.color.sf_text_secondary))
        sheetBinding.l2.setTextColor(ContextCompat.getColor(requireContext(),
            if (currentStep == 2) R.color.sf_text_primary else R.color.sf_text_secondary))
        sheetBinding.l3.setTextColor(ContextCompat.getColor(requireContext(),
            if (currentStep == 3) R.color.sf_text_primary else R.color.sf_text_secondary))
        
        // Update step dots
        sheetBinding.sd1.background = ContextCompat.getDrawable(requireContext(),
            if (currentStep >= 1) R.drawable.sf_step_dot_active else R.drawable.sf_step_dot_inactive)
        sheetBinding.sd2.background = ContextCompat.getDrawable(requireContext(),
            if (currentStep >= 2) R.drawable.sf_step_dot_active else R.drawable.sf_step_dot_inactive)
        sheetBinding.sd3.background = ContextCompat.getDrawable(requireContext(),
            if (currentStep >= 3) R.drawable.sf_step_dot_active else R.drawable.sf_step_dot_inactive)

        updateStepSummary()
    }

    private fun updateStepSummary() {
        val parts = mutableListOf<String>()
        if (!selectedSize.isNullOrBlank()) {
            parts.add("Size: $selectedSize")
        }
        if (chefTip.isNotBlank()) {
            parts.add("Tip: $chefTip")
        }
        parts.add("Qty: $quantity")
        sheetBinding.tvStepSummary.text = parts.joinToString(" • ")
    }
    
    private fun updateButtonConstraints() {
        // Get the ConstraintLayout parent of btnBack (it's inside the CardView)
        val constraintLayout = sheetBinding.btnBack.parent as? androidx.constraintlayout.widget.ConstraintLayout
            ?: return // Safety check
        
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        
        when (currentStep) {
            1 -> {
                constraintSet.clear(R.id.btnBack, ConstraintSet.TOP)
                constraintSet.connect(R.id.btnBack, ConstraintSet.TOP, R.id.amountBox, ConstraintSet.BOTTOM, 18)
            }
            2 -> {
                constraintSet.clear(R.id.btnBack, ConstraintSet.TOP)
                constraintSet.connect(R.id.btnBack, ConstraintSet.TOP, R.id.stepChefTip, ConstraintSet.BOTTOM, 18)
            }
            3 -> {
                constraintSet.clear(R.id.btnBack, ConstraintSet.TOP)
                constraintSet.connect(R.id.btnBack, ConstraintSet.TOP, R.id.stepQty, ConstraintSet.BOTTOM, 18)
            }
        }
        
        constraintSet.applyTo(constraintLayout)
    }
    
    private fun applyChefTipPreset(preset: String) {
        chefTip = preset
        sheetBinding.etChefTip.setText(preset)
    }

    private fun updateChipStyles() {
        val selectedColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_primary)
        val unselectedColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_chip_bg)
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.sf_white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.sf_text_secondary)

        listOf(
            sheetBinding.chipSour,
            sheetBinding.chipSpicy,
            sheetBinding.chipLessSalty,
            sheetBinding.chipCrispier,
            sheetBinding.chipNoOnions,
            sheetBinding.chipExtraSauce,
            sheetBinding.chipMildSpice
        ).forEach { chip ->
            if (chip.isChecked) {
                chip.chipBackgroundColor = selectedColor
                chip.setTextColor(selectedTextColor)
            } else {
                chip.chipBackgroundColor = unselectedColor
                chip.setTextColor(unselectedTextColor)
            }
        }
    }
    
    private fun clearChipSelection() {
        sheetBinding.chipSour.isChecked = false
        sheetBinding.chipSpicy.isChecked = false
        sheetBinding.chipLessSalty.isChecked = false
        sheetBinding.chipCrispier.isChecked = false
        sheetBinding.chipNoOnions.isChecked = false
        sheetBinding.chipExtraSauce.isChecked = false
        sheetBinding.chipMildSpice.isChecked = false
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
}

