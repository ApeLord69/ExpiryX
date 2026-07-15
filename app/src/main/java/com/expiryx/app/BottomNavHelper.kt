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
        
        // Remove listener temporarily to update selection without triggering it
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) return@setOnItemSelectedListener true
            
            val targetClass = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_history -> HistoryActivity::class.java
                R.id.nav_stats -> StatsActivity::class.java
                R.id.nav_settings -> SettingsActivity::class.java
                else -> null
            }
            
            if (targetClass != null && activity.javaClass != targetClass) {
                navigateTo(activity, targetClass)
                true
            } else {
                // If it's the same class, just return true (already handled by id check but for safety)
                true
            }
        }
    }

    private fun navigateTo(activity: AppCompatActivity, target: Class<*>) {
        val intent = Intent(activity, target)
        // REORDER_TO_FRONT + SINGLE_TOP ensures we don't keep creating new activities
        // and brings the existing one to the front.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
        // Remove transition animations to make tab switching feel instant and reduce stutter
        activity.overridePendingTransition(0, 0)
    }
}
