package com.expiryx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class ThemedAppCompatActivity : AppCompatActivity() {

    private var currentThemeRes: Int = -1
    private var isHighContrastActive: Boolean = false
    private var isColorblindActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        currentThemeRes = ThemeManager.getAccentThemeRes(this)
        isHighContrastActive = Prefs.isHighContrastEnabled(this)
        isColorblindActive = Prefs.isColorblindModeEnabled(this)
        ThemeManager.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
    }

    override fun onRestart() {
        super.onRestart()
        val newTheme = ThemeManager.getAccentThemeRes(this)
        val newHC = Prefs.isHighContrastEnabled(this)
        val newCB = Prefs.isColorblindModeEnabled(this)
        if (newTheme != currentThemeRes || newHC != isHighContrastActive || newCB != isColorblindActive) {
            recreate()
        }
    }
}
