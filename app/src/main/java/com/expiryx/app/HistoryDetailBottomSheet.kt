package com.expiryx.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.BottomsheetHistoryDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryDetailBottomSheet : ThemedBottomSheetDialogFragment() {

    private var _binding: BottomsheetHistoryDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var history: History
    private lateinit var viewModel: HistoryViewModel

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = (requireActivity() as HistoryActivity).viewModel
        arguments?.let {
            history = it.getParcelable("history")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowInsetsHelper.setupBottomSheetEdgeToEdge(this, binding.root)
        populateUI(history)
        setupListeners(history)
    }

    private fun populateUI(h: History) {
        // Image
        Glide.with(requireContext())
            .load(h.imageUri)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(binding.imageHistoryDetail)

        // Core fields
        binding.textHistoryName.text = h.productName
        binding.textHistoryBrand.text = h.brand.takeIf { !it.isNullOrBlank() } ?: "No brand"
        binding.textHistoryBrand.visibility = if (h.brand.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.textHistoryExpiry.text = getString(R.string.expiry_label, h.expirationDate?.let { formatDate(it) } ?: "N/A")
        binding.textHistoryQuantity.text = getString(R.string.quantity_label, h.quantity)
        
        if (h.weight != null) {
            binding.textHistoryWeight.text = getString(R.string.weight_label, h.weight, h.weightUnit)
            binding.textHistoryWeight.visibility = View.VISIBLE
        } else {
            binding.textHistoryWeight.visibility = View.GONE
        }
        binding.textHistoryFavourite.text = getString(R.string.favourite_label, if (h.isFavorite) getString(R.string.yes) else getString(R.string.no))

        // Barcode display
        if (!h.barcode.isNullOrBlank()) {
            binding.txtHistoryBarcode.text = "${getString(R.string.barcode_label)} ${h.barcode}"
            binding.txtHistoryBarcode.visibility = View.VISIBLE
        } else {
            binding.txtHistoryBarcode.visibility = View.GONE
        }

        // Timestamps
        binding.txtHistoryDateAdded.text = "${getString(R.string.added_label)} ${formatDateTime(h.dateAdded)}"
        
        if (h.dateModified != null) {
            binding.txtHistoryDateModified.text = "${getString(R.string.modified_label)} ${formatDateTime(h.dateModified)}"
            binding.txtHistoryDateModified.visibility = View.VISIBLE
        } else {
            binding.txtHistoryDateModified.visibility = View.GONE
        }
    }

    private fun setupListeners(history: History) {
        // Actions based on type
        when (history.action) {
            "Deleted" -> {
                binding.btnPrimary.text = getString(R.string.restore)
                binding.btnPrimary.setOnClickListener {
                    viewModel.restoreDeleted(history)
                    dismiss()
                }
            }
            "Used" -> {
                binding.btnPrimary.text = getString(R.string.history_action_unuse)
                binding.btnPrimary.setOnClickListener {
                    viewModel.unuse(history)
                    dismiss()
                }
            }
            "Expired" -> {
                binding.btnPrimary.text = getString(R.string.history_action_restore_expiry)
                binding.btnPrimary.setOnClickListener { showDatePicker(history) } 
            }
        }

        // Permanent delete
        binding.btnSecondary.text = getString(R.string.history_permanently_delete_title)
        binding.btnSecondary.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.history_permanently_delete_title))
                .setMessage(getString(R.string.history_permanently_delete_msg, history.productName))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    viewModel.permanentlyDelete(history)
                    dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun showDatePicker(historyForRestore: History) { 
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val calendar = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                val newExpiryMillis = calendar.timeInMillis
                viewModel.changeExpiry(historyForRestore, newExpiryMillis) 
                Toast.makeText(requireContext(), getString(R.string.history_toast_restored), Toast.LENGTH_SHORT).show()
                dismiss()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(history: History): HistoryDetailBottomSheet {
            return HistoryDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable("history", history)
                }
            }
        }
    }
}
