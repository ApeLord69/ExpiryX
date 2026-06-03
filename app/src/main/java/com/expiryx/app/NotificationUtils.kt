package com.expiryx.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object NotificationUtils {
    private const val CHANNEL_ID = "expiry_notifications"
    private const val CHANNEL_NAME = "Expiry Reminders"
    private const val CHANNEL_DESC = "Notifications about product expirations"

    fun showExpiryNotification(
        context: Context,
        title: String,
        message: String,
        productId: Int = 0,
        imageUri: String? = null
    ) {
        createChannel(context)

        // Open app when tapped and trigger detail view
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_product_id", productId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            productId.takeIf { it > 0 } ?: 0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Fallback small icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        // If image available, load as Large Icon
        if (!imageUri.isNullOrBlank()) {
            Glide.with(context)
                .asBitmap()
                .load(Uri.parse(imageUri))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        builder.setLargeIcon(resource)
                        sendNotification(context, builder.build(), productId, title, message)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // If load fails, send without large icon
                        sendNotification(context, builder.build(), productId, title, message)
                    }
                })
        } else {
            sendNotification(context, builder.build(), productId, title, message)
        }
    }

    private fun sendNotification(context: Context, notification: android.app.Notification, productId: Int, title: String, message: String) {
        val stableKey = if (productId > 0) "${productId}_${message}" else "global_${title}_${message}"
        val notifyId = stableKey.hashCode()

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notifyId, notification)
            }
        } catch (e: SecurityException) {
            Log.e("NotificationUtils", "Permission missing for notification", e)
        }
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CHANNEL_DESC }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
