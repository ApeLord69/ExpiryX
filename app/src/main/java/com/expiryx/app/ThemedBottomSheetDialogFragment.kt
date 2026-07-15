package com.expiryx.app

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Base BottomSheet that dynamically applies the user's selected accent theme
 * while maintaining the correct "overlay" behavior (smooth slide-up + dim background).
 */
abstract class ThemedBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int {
        // Use our specialized BottomSheet overlay theme.
        // This ensures the window is translucent and has a dim backdrop.
        return R.style.ThemeOverlay_ExpiryX_BottomSheetDialog
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        
        // Fetch the user's current accent theme (Aqua, Coral, etc.)
        val accentTheme = ThemeManager.getAccentThemeRes(requireContext())
        
        // Wrap the context with the accent theme. 
        // This ensures that layouts inflated inside the sheet correctly resolve 
        // ?attr/colorPrimary and other accent-dependent colors.
        val themedContext = ContextThemeWrapper(requireContext(), accentTheme)

        return inflater.cloneInContext(themedContext)
    }
}
