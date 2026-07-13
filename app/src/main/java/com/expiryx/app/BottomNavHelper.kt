package com.expiryx.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavHelper {

    fun setup(
        activity: AppCompatActivity,
        bottomNav: BottomNavigationView,
        selectedItemId: Int,
    ) {
        bottomNav.itemIconSize = activity.resources.getDimensionPixelSize(R.dimen.bottom_nav_icon_size)
        bottomNav.selectedItemId = selectedItemId
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.nav_home -> navigateTo(activity, MainActivity::class.java)
                R.id.nav_history -> navigateTo(activity, HistoryActivity::class.java)
                R.id.nav_stats -> navigateTo(activity, StatsActivity::class.java)
                R.id.nav_settings -> navigateTo(activity, SettingsActivity::class.java)
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun navigateTo(activity: AppCompatActivity, target: Class<*>) {
        val intent = Intent(activity, target)
        // REORDER_TO_FRONT keeps activity instances alive and switches between them efficiently.
        // We REMOVE activity.finish() to maintain the tab states and avoid unrecoverable input channels.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }
}
