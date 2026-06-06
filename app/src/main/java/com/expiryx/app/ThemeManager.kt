package com.expiryx.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.expiryx.app.R

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT = "accent_theme"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    const val ACCENT_AQUA = 0
    const val ACCENT_CORAL = 1
    const val ACCENT_SKY = 2
    const val ACCENT_VIOLET = 3

    data class AccentOption(
        val id: Int,
        val label: String,
        @StyleRes val themeRes: Int,
        val previewColorRes: Int,
    )

    val accentOptions: List<AccentOption> = listOf(
        AccentOption(ACCENT_AQUA, "Lighter Aqua", R.style.Theme_ExpiryX_Aqua, R.color.teal_200),
        AccentOption(ACCENT_CORAL, "Coral", R.style.Theme_ExpiryX_Coral, R.color.coral_primary),
        AccentOption(ACCENT_SKY, "Sky Blue", R.style.Theme_ExpiryX_Sky, R.color.sky_primary),
        AccentOption(ACCENT_VIOLET, "Violet", R.style.Theme_ExpiryX_Violet, R.color.violet_primary),
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): Int =
        prefs(context).getInt(KEY_THEME_MODE, THEME_SYSTEM)

    fun setThemeMode(context: Context, themeMode: Int) {
        prefs(context).edit().putInt(KEY_THEME_MODE, themeMode).apply()
        applyNightMode(themeMode)
    }

    fun getAccentTheme(context: Context): Int =
        prefs(context).getInt(KEY_ACCENT, ACCENT_AQUA)

    fun setAccentTheme(context: Context, accent: Int) {
        prefs(context).edit().putInt(KEY_ACCENT, accent).apply()
    }

    fun getAccentLabel(context: Context): String {
        val accent = getAccentTheme(context)
        return accentOptions.firstOrNull { it.id == accent }?.label ?: accentOptions.first().label
    }

    @StyleRes
    fun getAccentThemeRes(context: Context): Int {
        return accentOptions.firstOrNull { it.id == getAccentTheme(context) }?.themeRes
            ?: R.style.Theme_ExpiryX_Aqua
    }

    fun applyActivityTheme(activity: Activity) {
        activity.setTheme(getAccentThemeRes(activity))
    }

    fun applyNightMode(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun initializeTheme(context: Context) {
        applyNightMode(getThemeMode(context))
    }

    fun isDarkMode(context: Context): Boolean {
        return when (getThemeMode(context)) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
