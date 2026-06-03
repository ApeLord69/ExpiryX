package com.expiryx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val productId = intent.getIntExtra("product_id", -1)
        val daysBefore = intent.getIntExtra("days_before", -1)

        if (productId == -1) return

        // Check if snooze is active
        if (Prefs.isSnoozeActive(context)) {
            Log.d("NotifReceiver", "Snooze active, skipping notification for product $productId")
            return
        }

        val app = context.applicationContext as ProductApplication
        val repo = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            val product = repo.getProductById(productId)
            if (product != null) {
                val message = when (daysBefore) {
                    0 -> "${product.name} expires today!"
                    1 -> "${product.name} expires tomorrow."
                    else -> "${product.name} expires in $daysBefore days."
                }
                
                NotificationUtils.showExpiryNotification(
                    context,
                    product.name,
                    message,
                    product.id,
                    product.imageUri
                )
            }
        }
    }
}
