package com.expiryx.app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.ActivityManualEntryBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

class ManualEntryActivity : ThemedAppCompatActivity() {

    private enum class DateInputMode { WHEEL, CALENDAR, TEXT }

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }
    private lateinit var binding: ActivityManualEntryBinding

    private val dateFormat =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }

    private val monthLabels = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    private var editingProduct: Product? = null
    private var expiryMillis: Long? = null
    private var selectedImageUri: String? = null
    private var productBarcode: String? = null
    private var selectedWeightUnit: String = "g"
    private var isInternalUpdate = false
    private var dateInputMode = DateInputMode.WHEEL

    private val safeTextFilter = InputFilter { source, _, _, _, _, _ ->
        val allowed = Pattern.compile("^[a-zA-Z0-9 .,'&()\\-]*$")
        if (!allowed.matcher(source).matches()) "" else source
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* ignore */ }
                selectedImageUri = it.toString()
                Glide.with(this).load(it).into(binding.imageProductPreview)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowInsetsHelper.enableEdgeToEdge(this)

        binding = ActivityManualEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupFilters()
        setupWeightUnitDropdown()
        setupExpiryDateWheel()
        setupDateModeToggle()
        loadProductData()
        updateToolbarTitle()
        setupListeners()
        applyDateInputMode(DateInputMode.WHEEL)
        setupKeyboardDismissal()
    }

    private fun setupKeyboardDismissal() {
        binding.root.setOnTouchListener { _, _ ->
            currentFocus?.let { view ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
            false
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top inset as padding to the AppBarLayout so it draws under the status bar
            binding.appBarManualEntry.setPadding(0, systemBars.top, 0, 0)
            
            // Adjust save button margin for navigation bar
            binding.btnSaveProduct.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = (16 * resources.displayMetrics.density).toInt() + systemBars.bottom
            }

            // Adjust scroll view padding for navigation bar and to not be hidden by the button
            binding.manualEntryScroll.setPadding(0, 0, 0, (80 * resources.displayMetrics.density).toInt() + systemBars.bottom)

            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupToolbar() {
        updateToolbarTitle()

        binding.toolbarManualEntry.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_cancel -> {
                    finish()
                    true
                }
                R.id.action_clear -> {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Reset Form")
                        .setMessage("Are you sure you want to clear all fields?")
                        .setPositiveButton("Reset") { _, _ -> resetForm() }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateToolbarTitle() {
        binding.toolbarManualEntry.title = when {
            editingProduct != null -> getString(R.string.edit_product)
            !productBarcode.isNullOrBlank() || !selectedImageUri.isNullOrBlank() -> getString(R.string.save_product)
            else -> getString(R.string.add_product)
        }
    }

    private fun setupFilters() {
        binding.editTextProductName.filters = arrayOf(safeTextFilter, InputFilter.LengthFilter(50))
        binding.editTextBrand.filters = arrayOf(safeTextFilter, InputFilter.LengthFilter(50))
        binding.editTextQuantity.filters = arrayOf(InputFilter.LengthFilter(4))
        binding.editTextWeight.filters = arrayOf(InputFilter.LengthFilter(5))
    }

    private fun setupWeightUnitDropdown() {
        val weightUnits = arrayOf("g", "ml")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, weightUnits)
        binding.spinnerWeightUnit.setAdapter(adapter)
        binding.spinnerWeightUnit.setText(selectedWeightUnit, false)
        binding.spinnerWeightUnit.setOnItemClickListener { _, _, position, _ ->
            selectedWeightUnit = weightUnits[position]
        }
    }

    private fun setupExpiryDateWheel() {
        val wheel = binding.expiryDateWheel
        val pickerDay = wheel.pickerDay
        val pickerMonth = wheel.pickerMonth
        val pickerYear = wheel.pickerYear

        val today = Calendar.getInstance()
        val startYear = today.get(Calendar.YEAR)
        val endYear = startYear + 10

        pickerYear.minValue = startYear
        pickerYear.maxValue = endYear
        pickerYear.value = startYear

        pickerMonth.minValue = 0
        pickerMonth.maxValue = monthLabels.size - 1
        pickerMonth.displayedValues = monthLabels
        pickerMonth.value = today.get(Calendar.MONTH)

        updateDayPickerMax(pickerDay, pickerMonth.value + 1, pickerYear.value)
        pickerDay.value = today.get(Calendar.DAY_OF_MONTH).coerceAtMost(pickerDay.maxValue)

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            if (isInternalUpdate) return@OnValueChangeListener
            updateDayPickerMax(pickerDay, pickerMonth.value + 1, pickerYear.value)
            syncExpiryFromPickers()
        }

        pickerDay.setOnValueChangedListener(listener)
        pickerMonth.setOnValueChangedListener(listener)
        pickerYear.setOnValueChangedListener(listener)

        syncExpiryFromPickers()
    }

    private fun setupDateModeToggle() {
        val wheel = binding.expiryDateWheel
        wheel.btnToggleDateMode.setOnClickListener { cycleDateInputMode() }
        wheel.btnOpenCalendarPicker.setOnClickListener { showCalendarPicker() }
        wheel.editTextExpiryDate.doAfterTextChanged {
            if (isInternalUpdate) return@doAfterTextChanged
            if (dateInputMode == DateInputMode.TEXT) {
                parseTextExpiryDate(wheel.editTextExpiryDate.text?.toString().orEmpty())
            }
        }
    }

    private fun cycleDateInputMode() {
        val next = when (dateInputMode) {
            DateInputMode.WHEEL -> DateInputMode.CALENDAR
            DateInputMode.CALENDAR -> DateInputMode.TEXT
            DateInputMode.TEXT -> DateInputMode.WHEEL
        }
        applyDateInputMode(next)
        if (next == DateInputMode.CALENDAR) {
            showCalendarPicker()
        }
    }

    private fun applyDateInputMode(mode: DateInputMode) {
        dateInputMode = mode
        val wheel = binding.expiryDateWheel

        val showWheel = mode == DateInputMode.WHEEL
        wheel.containerWheel.visibility = if (showWheel) View.VISIBLE else View.GONE
        wheel.wheelLabelsRow.visibility = if (showWheel) View.VISIBLE else View.GONE
        wheel.containerCalendar.visibility = if (mode == DateInputMode.CALENDAR) View.VISIBLE else View.GONE
        wheel.layoutExpiryText.visibility = if (mode == DateInputMode.TEXT) View.VISIBLE else View.GONE

        wheel.btnToggleDateMode.setImageResource(
            when (mode) {
                DateInputMode.WHEEL -> R.drawable.ic_calendar
                DateInputMode.CALENDAR -> R.drawable.ic_keyboard
                DateInputMode.TEXT -> R.drawable.ic_wheel
            }
        )

        wheel.textDateModeHint.setText(
            when (mode) {
                DateInputMode.WHEEL -> R.string.expiry_date_wheel_hint
                DateInputMode.CALENDAR -> R.string.expiry_date_calendar_hint
                DateInputMode.TEXT -> R.string.expiry_date_text_hint
            }
        )

        expiryMillis?.let { millis ->
            wheel.textSelectedExpiryDate.text = dateFormat.format(Date(millis))
            if (mode == DateInputMode.TEXT) {
                wheel.editTextExpiryDate.setText(dateFormat.format(Date(millis)))
            }
        }
    }

    private fun showCalendarPicker() {
        val cal = Calendar.getInstance()
        expiryMillis?.let { cal.timeInMillis = it }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val calendar = Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                setExpiryFromMillis(calendar.timeInMillis)
            },
            cal[Calendar.YEAR],
            cal[Calendar.MONTH],
            cal[Calendar.DAY_OF_MONTH]
        ).show()
    }

    private fun parseTextExpiryDate(raw: String) {
        val wheel = binding.expiryDateWheel
        if (raw.isBlank()) {
            expiryMillis = null
            wheel.textSelectedExpiryDate.text = getString(R.string.expiry_date_not_set)
            return
        }
        try {
            val parsed = dateFormat.parse(raw.trim())
            if (parsed != null) {
                setExpiryFromMillis(parsed.time)
                wheel.textExpiryDateError.visibility = View.GONE
            }
        } catch (_: ParseException) {
            wheel.textExpiryDateError.text = getString(R.string.expiry_date_invalid)
            wheel.textExpiryDateError.visibility = View.VISIBLE
        }
    }

    private fun setExpiryFromMillis(millis: Long) {
        expiryMillis = millis
        binding.expiryDateWheel.textSelectedExpiryDate.text = dateFormat.format(Date(millis))
        binding.expiryDateWheel.textExpiryDateError.visibility = View.GONE
        setPickersFromMillis(millis)
        
        isInternalUpdate = true
        binding.expiryDateWheel.editTextExpiryDate.setText(dateFormat.format(Date(millis)))
        isInternalUpdate = false
    }

    private fun updateDayPickerMax(pickerDay: NumberPicker, month: Int, year: Int) {
        val maxDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val previousDay = pickerDay.value
        pickerDay.minValue = 1
        pickerDay.maxValue = maxDay
        pickerDay.value = if (previousDay in 1..maxDay) previousDay else maxDay
    }

    private fun syncExpiryFromPickers() {
        val wheel = binding.expiryDateWheel
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, wheel.pickerYear.value)
            set(Calendar.MONTH, wheel.pickerMonth.value)
            set(Calendar.DAY_OF_MONTH, wheel.pickerDay.value)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        expiryMillis = calendar.timeInMillis
        wheel.textSelectedExpiryDate.text = dateFormat.format(calendar.time)
        wheel.textExpiryDateError.visibility = View.GONE
    }

    private fun setPickersFromMillis(millis: Long) {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val wheel = binding.expiryDateWheel

        isInternalUpdate = true
        wheel.pickerYear.value = calendar.get(Calendar.YEAR)
        wheel.pickerMonth.value = calendar.get(Calendar.MONTH)
        updateDayPickerMax(wheel.pickerDay, wheel.pickerMonth.value + 1, wheel.pickerYear.value)
        wheel.pickerDay.value = calendar.get(Calendar.DAY_OF_MONTH)
        isInternalUpdate = false
    }

    private fun loadProductData() {
        editingProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("product")
        }

        selectedImageUri = intent.getStringExtra("imageUri") ?: editingProduct?.imageUri
        productBarcode = intent.getStringExtra("barcode") ?: editingProduct?.barcode

        editingProduct?.let { product ->
            binding.editTextProductName.setText(product.name)
            binding.editTextBrand.setText(product.brand)
            product.expirationDate?.let { setExpiryFromMillis(it) }
            binding.editTextQuantity.setText(product.quantity.toString())
            binding.editTextWeight.setText(product.weight?.toString() ?: "")
            binding.checkboxFavorite.isChecked = product.isFavorite
            selectedWeightUnit = product.weightUnit
            binding.spinnerWeightUnit.setText(product.weightUnit, false)
        }

        updateToolbarTitle()

        if (!productBarcode.isNullOrBlank()) {
            binding.textViewBarcodeValue.text = getString(R.string.barcode_with_value, productBarcode)
            binding.textViewBarcodeValue.visibility = View.VISIBLE
        } else {
            binding.textViewBarcodeValue.visibility = View.GONE
        }

        if (!selectedImageUri.isNullOrBlank()) {
            Glide.with(this).load(Uri.parse(selectedImageUri))
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(binding.imageProductPreview)
        } else {
            binding.imageProductPreview.setImageResource(R.drawable.ic_placeholder)
        }
    }

    private fun setupListeners() {
        binding.btnSaveProduct.setOnClickListener { saveProduct() }
        binding.imageProductPreview.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        binding.imageProductPreview.setOnLongClickListener {
            selectedImageUri = null
            binding.imageProductPreview.setImageResource(R.drawable.ic_placeholder)
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun resetForm() {
        // Clear text fields
        binding.editTextProductName.text?.clear()
        binding.editTextBrand.text?.clear()
        binding.editTextQuantity.text?.clear()
        binding.editTextWeight.text?.clear()
        binding.checkboxFavorite.isChecked = false
        
        // Reset expiry
        expiryMillis = null
        binding.expiryDateWheel.textSelectedExpiryDate.text = getString(R.string.expiry_date_not_set)
        binding.expiryDateWheel.editTextExpiryDate.text?.clear()
        val today = Calendar.getInstance()
        setPickersFromMillis(today.timeInMillis)
        
        // Clear image
        selectedImageUri = null
        binding.imageProductPreview.setImageResource(R.drawable.ic_placeholder)
        
        // Clear barcode
        productBarcode = null
        binding.textViewBarcodeValue.text = ""
        binding.textViewBarcodeValue.visibility = View.GONE
        
        // Reset errors
        binding.layoutProductName.error = null
        binding.layoutQuantity.error = null
        binding.layoutWeight.error = null
        binding.expiryDateWheel.textExpiryDateError.visibility = View.GONE
        
        // Update title (will likely revert to "Add Product")
        updateToolbarTitle()
        
        Toast.makeText(this, "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun saveProduct() {
        val name = binding.editTextProductName.text.toString().trim()
        if (name.isEmpty()) {
            binding.layoutProductName.error = "Product name is required"
            return
        }
        binding.layoutProductName.error = null

        if (dateInputMode == DateInputMode.TEXT) {
            parseTextExpiryDate(binding.expiryDateWheel.editTextExpiryDate.text?.toString().orEmpty())
        }

        val finalExpiryMillis = expiryMillis
        if (finalExpiryMillis == null) {
            binding.expiryDateWheel.textExpiryDateError.text = getString(R.string.error_expiry_date_required)
            binding.expiryDateWheel.textExpiryDateError.visibility = View.VISIBLE
            return
        }
        binding.expiryDateWheel.textExpiryDateError.visibility = View.GONE

        val brand = binding.editTextBrand.text.toString().trim().takeIf { it.isNotBlank() }

        val qtyString = binding.editTextQuantity.text.toString().trim()
        val qtyInt = if (qtyString.isBlank()) {
            1
        } else {
            qtyString.toIntOrNull()?.takeIf { it in 1..9999 } ?: run {
                binding.layoutQuantity.error = "Quantity must be between 1 and 9999"
                return
            }
        }
        binding.layoutQuantity.error = null

        val weightString = binding.editTextWeight.text.toString()
        val parsedWeight = weightString.toIntOrNull()
        val finalWeight: Int?
        if (weightString.isNotBlank()) {
            if (parsedWeight == null || parsedWeight < 1 || parsedWeight > 99999) {
                binding.layoutWeight.error = "Weight must be between 1 and 99999"
                return
            }
            finalWeight = parsedWeight
        } else {
            finalWeight = null
        }
        binding.layoutWeight.error = null

        val currentTime = System.currentTimeMillis()
        val isEditing = editingProduct != null && editingProduct!!.id != 0

        val product = Product(
            id = editingProduct?.id ?: 0,
            uuid = editingProduct?.uuid ?: UUID.randomUUID().toString(),
            name = name,
            brand = brand,
            expirationDate = finalExpiryMillis,
            quantity = qtyInt,
            weight = finalWeight,
            weightUnit = selectedWeightUnit,
            imageUri = selectedImageUri,
            isFavorite = binding.checkboxFavorite.isChecked,
            barcode = productBarcode,
            dateAdded = editingProduct?.dateAdded ?: currentTime,
            dateModified = if (isEditing) currentTime else null
        )

        if (isEditing) {
            productViewModel.update(product)
            editingProduct?.let { NotificationScheduler.cancelForProduct(this, it) }
            NotificationScheduler.scheduleForProduct(this, product)
            Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
        } else {
            productViewModel.insert(product)
            NotificationScheduler.scheduleForProduct(this, product)
            Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
