package com.expiryx.app

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

object WindowInsetsHelper {

    fun enableEdgeToEdge(activity: AppCompatActivity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    fun applyPadding(
        view: View,
        applyLeft: Boolean = true,
        applyTop: Boolean = true,
        applyRight: Boolean = true,
        applyBottom: Boolean = true,
    ) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = if (applyLeft) initialLeft + bars.left else initialLeft,
                top = if (applyTop) initialTop + bars.top else initialTop,
                right = if (applyRight) initialRight + bars.right else initialRight,
                bottom = if (applyBottom) initialBottom + bars.bottom else initialBottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun applyBottomMargin(view: View, extraBottomDp: Int = 0) {
        val extraPx = (extraBottomDp * view.resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + extraPx
            }
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun setupBottomSheetEdgeToEdge(fragment: BottomSheetDialogFragment, rootView: View) {
        fragment.dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as BottomSheetDialog
            WindowCompat.setDecorFitsSystemWindows(sheetDialog.window!!, false)
            val bottomSheet = sheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                it.setBackgroundResource(R.drawable.bottom_sheet_surface_bg)
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
            }
            applyPadding(rootView, applyTop = false, applyBottom = true)
        }
    }
}
