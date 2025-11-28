package com.streatfeast.app.fragments

import android.os.Bundle
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

class ItemCustomizeBottomSheet : Fragment() {
    
    private var _binding: FragmentItemCustomizeBinding? = null
    private val binding get() = _binding!!
    
    // Access the included sheet content views directly
    private val sheetBinding: SheetItemCustomizeBinding by lazy {
        val includedView = binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.sheetContent)
        SheetItemCustomizeBinding.bind(includedView)
    }
    
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    
    private var currentStep = 1
    private var selectedSize: String? = null
    private var chefTip: String = ""
    private var quantity: Int = 1
    private var menuItem: MenuItem? = null
    private var orderItem: OrderItem? = null // For edit mode
    private var isEditMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get menu item and order item from arguments
        arguments?.let { args ->
            menuItem = args.getParcelable("menuItem")
            orderItem = args.getParcelable("orderItem")
            isEditMode = orderItem != null
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
            
            // Pre-populate size
            selectedSize = item.size
            when (item.size?.lowercase()) {
                "small" -> {
                    sheetBinding.rbSmall.isChecked = true
                    sheetBinding.rbMedium.isChecked = false
                    sheetBinding.rbLarge.isChecked = false
                }
                "medium" -> {
                    sheetBinding.rbSmall.isChecked = false
                    sheetBinding.rbMedium.isChecked = true
                    sheetBinding.rbLarge.isChecked = false
                }
                "large" -> {
                    sheetBinding.rbSmall.isChecked = false
                    sheetBinding.rbMedium.isChecked = false
                    sheetBinding.rbLarge.isChecked = true
                }
                else -> {
                    // No size selected, default to Small
                    sheetBinding.rbSmall.isChecked = true
                    selectedSize = "Small"
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
                
                // Set default size if available
                if (item.sizes.isNotEmpty()) {
                    selectedSize = item.sizes[0]
                }
            }
        }
        
        updateStepVisibility()
        updateStepIndicator()
    }
    
    private fun setupStep1() {
        // Set up individual listeners to ensure single selection
        sheetBinding.rbSmall.setOnClickListener {
            sheetBinding.rbSmall.isChecked = true
            sheetBinding.rbMedium.isChecked = false
            sheetBinding.rbLarge.isChecked = false
            selectedSize = "Small"
        }
        
        sheetBinding.rbMedium.setOnClickListener {
            sheetBinding.rbSmall.isChecked = false
            sheetBinding.rbMedium.isChecked = true
            sheetBinding.rbLarge.isChecked = false
            selectedSize = "Medium"
        }
        
        sheetBinding.rbLarge.setOnClickListener {
            sheetBinding.rbSmall.isChecked = false
            sheetBinding.rbMedium.isChecked = false
            sheetBinding.rbLarge.isChecked = true
            selectedSize = "Large"
        }
        
        // Also use RadioGroup listener as backup
        sheetBinding.rgSize.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSmall -> selectedSize = "Small"
                R.id.rbMedium -> selectedSize = "Medium"
                R.id.rbLarge -> selectedSize = "Large"
            }
        }
        
        // Set default selection only if not in edit mode (edit mode is handled in setupInitialState)
        if (!isEditMode) {
            sheetBinding.rbSmall.isChecked = true
            selectedSize = "Small"
        }
    }
    
    private fun setupStep2() {
        sheetBinding.chipSour.setOnClickListener {
            toggleChip(sheetBinding.chipSour)
            if (sheetBinding.chipSour.isChecked) {
                chefTip = "Make it extra sour"
                sheetBinding.etChefTip.setText("Make it extra sour")
            } else {
                sheetBinding.etChefTip.setText("")
                chefTip = ""
            }
        }
        
        sheetBinding.chipSpicy.setOnClickListener {
            toggleChip(sheetBinding.chipSpicy)
            if (sheetBinding.chipSpicy.isChecked) {
                chefTip = "Make it extra spicy"
                sheetBinding.etChefTip.setText("Make it extra spicy")
            } else {
                sheetBinding.etChefTip.setText("")
                chefTip = ""
            }
        }
        
        sheetBinding.etChefTip.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Clear chip selection when user types
                clearChipSelection()
            }
        }
    }
    
    private fun setupStep3() {
        sheetBinding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                sheetBinding.tvQty.text = quantity.toString()
            }
        }
        
        sheetBinding.btnPlus.setOnClickListener {
            if (quantity < 99) {
                quantity++
                sheetBinding.tvQty.text = quantity.toString()
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
        if (isEditMode && orderItem != null) {
            // Update existing item
            val item = orderItem!!
            draftViewModel.updateDraftItem(item.id) { currentItem ->
                currentItem.copy(
                    size = selectedSize,
                    qty = quantity,
                    chefTip = chefTip
                )
            }
        } else {
            // Add new item
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
    
    private fun toggleChip(chip: Chip) {
        val isSelected = chip.isChecked
        clearChipSelection()
        
        if (!isSelected) {
            chip.isChecked = true
            chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_primary)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_white))
        }
    }
    
    private fun clearChipSelection() {
        sheetBinding.chipSour.isChecked = false
        sheetBinding.chipSpicy.isChecked = false
        
        sheetBinding.chipSour.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_chip_bg)
        sheetBinding.chipSpicy.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_chip_bg)
        
        sheetBinding.chipSour.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_text_secondary))
        sheetBinding.chipSpicy.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_text_secondary))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
}

