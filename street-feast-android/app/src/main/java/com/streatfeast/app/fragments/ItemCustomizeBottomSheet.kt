package com.streatfeast.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.streatfeast.app.R
import com.streatfeast.app.databinding.SheetItemCustomizeBinding
import com.streatfeast.app.models.MenuItem
import com.streatfeast.app.models.OrderItem
import com.streatfeast.app.viewmodels.OrderDraftViewModel

class ItemCustomizeBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: SheetItemCustomizeBinding? = null
    private val binding get() = _binding!!
    
    private val draftViewModel: OrderDraftViewModel by viewModels({ requireActivity() })
    
    private var currentStep = 1
    private var selectedSize: String? = null
    private var chefTip: String = ""
    private var quantity: Int = 1
    private var menuItem: MenuItem? = null
    
    var onItemAdded: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_StreetFeast)
        
        // Get menu item from arguments
        arguments?.let { args ->
            menuItem = args.getParcelable("menuItem")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetItemCustomizeBinding.inflate(inflater, container, false)
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
        menuItem?.let { item ->
            binding.tvSheetTitle.text = item.name
            
            // Set default size if available
            if (item.sizes.isNotEmpty()) {
                selectedSize = item.sizes[0]
            }
        }
        
        updateStepVisibility()
        updateStepIndicator()
    }
    
    private fun setupStep1() {
        binding.rbSmall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedSize = "Small"
        }
        
        binding.rbMedium.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedSize = "medium"
        }
        
        binding.rbLarge.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedSize = "Large"
        }
    }
    
    private fun setupStep2() {
        binding.chipSour.setOnClickListener {
            toggleChip(binding.chipSour)
            if (binding.chipSour.isChecked) {
                chefTip = "Sour"
                binding.etChefTip.setText("Sour")
            }
        }
        
        binding.chipSpicy.setOnClickListener {
            toggleChip(binding.chipSpicy)
            if (binding.chipSpicy.isChecked) {
                chefTip = "Spicy"
                binding.etChefTip.setText("Spicy")
            }
        }
        
        binding.chipCreamy.setOnClickListener {
            toggleChip(binding.chipCreamy)
            if (binding.chipCreamy.isChecked) {
                chefTip = "Creamy"
                binding.etChefTip.setText("Creamy")
            }
        }
        
        binding.etChefTip.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Clear chip selection when user types
                clearChipSelection()
            }
        }
    }
    
    private fun setupStep3() {
        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQty.text = quantity.toString()
            }
        }
        
        binding.btnPlus.setOnClickListener {
            if (quantity < 99) {
                quantity++
                binding.tvQty.text = quantity.toString()
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        binding.btnBack.setOnClickListener {
            handleBack()
        }
        
        binding.btnNext.setOnClickListener {
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
            dismiss()
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
                chefTip = binding.etChefTip.text.toString().trim()
                currentStep = 3
            }
            3 -> {
                // Add item to draft and dismiss
                addItemToDraft()
                dismiss()
            }
        }
        
        updateStepVisibility()
        updateStepIndicator()
        updateButtonConstraints()
    }
    
    private fun addItemToDraft() {
        menuItem?.let { item ->
            val orderItem = OrderItem(
                itemId = item.id,
                nameSnapshot = item.name,
                size = selectedSize,
                vegFlagSnapshot = item.vegFlag,
                qty = quantity,
                chefTip = chefTip
            )
            
            draftViewModel.addDraftItem(orderItem)
            onItemAdded?.invoke()
        }
    }
    
    private fun updateStepVisibility() {
        when (currentStep) {
            1 -> {
                binding.amountBox.visibility = View.VISIBLE
                binding.stepChefTip.visibility = View.GONE
                binding.stepQty.visibility = View.GONE
            }
            2 -> {
                binding.amountBox.visibility = View.GONE
                binding.stepChefTip.visibility = View.VISIBLE
                binding.stepQty.visibility = View.GONE
            }
            3 -> {
                binding.amountBox.visibility = View.GONE
                binding.stepChefTip.visibility = View.GONE
                binding.stepQty.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateStepIndicator() {
        // Update step label colors
        binding.l1.setTextColor(ContextCompat.getColor(requireContext(),
            if (currentStep == 1) R.color.sf_text_primary else R.color.sf_text_secondary))
        binding.l2.setTextColor(ContextCompat.getColor(requireContext(),
            if (currentStep == 2) R.color.sf_text_primary else R.color.sf_text_secondary))
        binding.l3.setTextColor(ContextCompat.getColor(requireContext(),
            if (currentStep == 3) R.color.sf_text_primary else R.color.sf_text_secondary))
        
        // Update step dots
        binding.sd1.background = ContextCompat.getDrawable(requireContext(),
            if (currentStep >= 1) R.drawable.sf_step_dot_active else R.drawable.sf_step_dot_inactive)
        binding.sd2.background = ContextCompat.getDrawable(requireContext(),
            if (currentStep >= 2) R.drawable.sf_step_dot_active else R.drawable.sf_step_dot_inactive)
        binding.sd3.background = ContextCompat.getDrawable(requireContext(),
            if (currentStep >= 3) R.drawable.sf_step_dot_active else R.drawable.sf_step_dot_inactive)
    }
    
    private fun updateButtonConstraints() {
        // Update btnBack constraint based on current step
        val params = binding.btnBack.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.topToBottom = when (currentStep) {
            1 -> R.id.amountBox
            2 -> R.id.stepChefTip
            3 -> R.id.stepQty
            else -> R.id.amountBox
        }
        binding.btnBack.layoutParams = params
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
        binding.chipSour.isChecked = false
        binding.chipSpicy.isChecked = false
        binding.chipCreamy.isChecked = false
        
        binding.chipSour.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_chip_bg)
        binding.chipSpicy.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_chip_bg)
        binding.chipCreamy.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.sf_chip_bg)
        
        binding.chipSour.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_text_secondary))
        binding.chipSpicy.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_text_secondary))
        binding.chipCreamy.setTextColor(ContextCompat.getColor(requireContext(), R.color.sf_text_secondary))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(menuItem: MenuItem): ItemCustomizeBottomSheet {
            return ItemCustomizeBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable("menuItem", menuItem)
                }
            }
        }
    }
}

