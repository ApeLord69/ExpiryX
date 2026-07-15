package com.expiryx.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.BottomSheetProductDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductDetailBottomSheet : ThemedBottomSheetDialogFragment() {

    private var _binding: BottomSheetProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by activityViewModels {
        ProductViewModelFactory((requireActivity().application as ProductApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowInsetsHelper.setupBottomSheetEdgeToEdge(this, binding.root)

        val product = arguments?.getParcelable<Product>(ARG_PRODUCT) ?: return
        populateUI(product)
        setupListeners(product)
    }

    private fun populateUI(p: Product) {
        Glide.with(requireContext())
            .load(if (p.imageUri.isNullOrBlank()) R.drawable.ic_placeholder else Uri.parse(p.imageUri))
            .error(R.drawable.ic_placeholder)
            .centerCrop()
            .into(binding.imgProductDetail)

        binding.txtDetailName.text = p.name
        binding.txtDetailBrand.text = p.brand
        binding.txtDetailBrand.visibility = if (p.brand.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.txtDetailExpiry.text = getString(
            R.string.expires_on_detail,
            ExpiryDisplayUtils.formatExpiryDate(p.expirationDate)
        )

        val daysLabel = ExpiryDisplayUtils.formatDaysRemaining(requireContext(), p.expirationDate)
        binding.txtDetailDaysRemaining.text = daysLabel
        ExpiryDisplayUtils.applyTrafficLightPill(binding.txtDetailDaysRemaining, p.expirationDate)

        binding.txtDetailQuantity.text = getString(R.string.quantity_label, p.quantity)

        if (p.weight != null) {
            binding.txtDetailWeight.text = getString(R.string.weight_label, p.weight, p.weightUnit)
            binding.txtDetailWeight.visibility = View.VISIBLE
        } else {
            binding.txtDetailWeight.visibility = View.GONE
        }

        if (!p.barcode.isNullOrBlank()) {
            binding.txtDetailBarcode.text = "${getString(R.string.barcode_label)} ${p.barcode}"
            binding.txtDetailBarcode.visibility = View.VISIBLE
        } else {
            binding.txtDetailBarcode.visibility = View.GONE
        }

        binding.txtDetailDateAdded.text = "${getString(R.string.added_label)} ${formatDateTime(p.dateAdded)}"

        if (p.dateModified != null) {
            binding.txtDetailDateModified.text = "${getString(R.string.modified_label)} ${formatDateTime(p.dateModified)}"
            binding.txtDetailDateModified.visibility = View.VISIBLE
        } else {
            binding.txtDetailDateModified.visibility = View.GONE
        }

        updateSnoozeButton(p.isSnoozed)
    }

    private fun updateSnoozeButton(isSnoozed: Boolean) {
        binding.btnSnooze.apply {
            text = if (isSnoozed) "Snoozed" else "Snooze"
            alpha = if (isSnoozed) 0.6f else 1.0f
            setIconResource(if (isSnoozed) R.drawable.ic_notification else R.drawable.ic_notification_off)
        }
    }

    private fun setupListeners(p: Product) {
        val hostActivity = activity as? MainActivity
        binding.btnMarkAsUsed.setOnClickListener {
            hostActivity?.markProductAsUsed(p)
            dismiss()
        }
        binding.btnDelete.setOnClickListener {
            hostActivity?.deleteProductWithConfirmation(p)
            dismiss()
        }
        binding.btnEdit.setOnClickListener {
            hostActivity?.editProduct(p)
            dismiss()
        }
        binding.btnSnooze.setOnClickListener {
            val updatedProduct = p.copy(isSnoozed = !p.isSnoozed)
            viewModel.update(updatedProduct)
            updateSnoozeButton(updatedProduct.isSnoozed)
            val msg = if (updatedProduct.isSnoozed) "Notifications muted for this item" else "Notifications enabled"
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
            // NotificationScheduler is handled in ViewModel/Repository update usually, 
            // but we ensure it matches the new state.
            if (updatedProduct.isSnoozed) {
                NotificationScheduler.cancelForProduct(requireContext(), updatedProduct)
            } else {
                NotificationScheduler.scheduleForProduct(requireContext(), updatedProduct)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    companion object {
        private const val ARG_PRODUCT = "product"
        fun newInstance(product: Product) = ProductDetailBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_PRODUCT, product) }
        }
    }
}
