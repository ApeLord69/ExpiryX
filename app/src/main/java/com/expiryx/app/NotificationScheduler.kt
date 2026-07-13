package com.expiryx.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

/**
 * Modern Notification Scheduler using AlarmManager for exact timing.
 * Supports:
 * 1. Multiple reminder intervals (Today, 1 day before, 3 days before, etc.)
 * 2. Exact user-defined time of day.
 * 3. Master on/off switch.
 * 4. Snooze functionality.
 */
object NotificationScheduler {
    private const val TAG = "NotifScheduler"

    // Intervals: 0 (today), 1 (tomorrow), 3, 7, 14, 30 days before
    val POSSIBLE_INTERVAL_VALUES = arrayOf("0", "1", "3", "7", "14", "30")

    /**
     * Schedules all enabled reminders for a specific product.
     */
    fun scheduleForProduct(context: Context, product: Product) {
        if (!Prefs.isNotificationsEnabled(context) || product.isSnoozed) {
            cancelForProduct(context, product)
            return
        }

        val expiryMillis = product.expirationDate ?: return
        val intervals = Prefs.getReminderIntervals(context)
        val targetHour = Prefs.getDefaultHour(context)
        val targetMinute = Prefs.getDefaultMinute(context)

        // Clear existing alarms for this product to prevent duplicates/ghosts
        cancelForProduct(context, product)

        intervals.forEach { intervalStr ->
            val daysBefore = intervalStr.toIntOrNull() ?: return@forEach
            val triggerTime = calculateTriggerTime(expiryMillis, daysBefore, targetHour, targetMinute)

            // Only schedule if the trigger time is in the future
            if (triggerTime > System.currentTimeMillis()) {
                scheduleAlarm(context, product, daysBefore, triggerTime)
            }
        }
    }

    /**
     * Cancels all scheduled alarms for a product.
     */
    fun cancelForProduct(context: Context, product: Product) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        POSSIBLE_INTERVAL_VALUES.forEach { interval ->
            val pendingIntent = createPendingIntent(context, product.id, interval.toInt())
            alarmManager.cancel(pendingIntent)
        }
        Log.d(TAG, "Cancelled all alarms for product: ${product.name}")
    }

    fun rescheduleAll(context: Context, products: List<Product>) {
        products.forEach { scheduleForProduct(context, it) }
        Log.d(TAG, "Rescheduled all notifications for ${products.size} products")
    }

    private fun scheduleAlarm(context: Context, product: Product, daysBefore: Int, triggerTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, product.id, daysBefore)

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm for ${product.name} at $daysBefore days before ($triggerTime)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    private fun createPendingIntent(context: Context, productId: Int, daysBefore: Int): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("product_id", productId)
            putExtra("days_before", daysBefore)
        }
        // Unique request code based on product and interval to avoid overwriting
        val requestCode = productId * 100 + daysBefore
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun calculateTriggerTime(expiryMillis: Long, daysBefore: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = expiryMillis
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
