package com.expiryx.app

import android.content.Context
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExpiryDisplayUtils {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun formatDaysRemaining(context: Context, expiryMillis: Long?): String {
        if (expiryMillis == null) return context.getString(R.string.expiry_none)
        val diff = ExpiryTrafficLightUtils.dayDiff(expiryMillis)
        return when {
            diff == null -> context.getString(R.string.expiry_none)
            diff < 0 -> context.getString(R.string.expiry_expired)
            diff == 0L -> context.getString(R.string.expiry_today)
            diff == 1L -> context.getString(R.string.expiry_tomorrow)
            else -> context.getString(R.string.expiry_days_left, diff.toInt())
        }
    }

    fun formatExpiryDate(expiryMillis: Long?): String {
        expiryMillis ?: return "N/A"
        return dateFormat.format(Date(expiryMillis))
    }

    fun applyTrafficLightPill(textView: TextView, expiryMillis: Long?) {
        val context = textView.context
        val theme = context.theme
        val typedValue = android.util.TypedValue()

        fun getThemeColor(attr: Int): Int {
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        if (expiryMillis == null) {
            textView.setBackgroundResource(R.drawable.pill_expiry_safe_bg)
            textView.setTextColor(getThemeColor(R.attr.expiryTextUnknown))
            return
        }

        when (ExpiryTrafficLightUtils.classify(expiryMillis)) {
            ExpiryTrafficLight.EXPIRED -> {
                textView.setBackgroundResource(R.drawable.pill_expiry_expired_list_bg)
                textView.setTextColor(getThemeColor(R.attr.expiryTextExpired))
            }
            ExpiryTrafficLight.URGENT -> {
                textView.setBackgroundResource(R.drawable.pill_expiry_urgent_bg)
                textView.setTextColor(getThemeColor(R.attr.expiryTextUrgent))
            }
            ExpiryTrafficLight.SAFE -> {
                textView.setBackgroundResource(R.drawable.pill_expiry_safe_bg)
                textView.setTextColor(getThemeColor(R.attr.expiryTextSafe))
            }
            ExpiryTrafficLight.UNKNOWN -> {
                textView.setBackgroundResource(R.drawable.pill_expiry_safe_bg)
                textView.setTextColor(getThemeColor(R.attr.expiryTextUnknown))
            }
        }
    }
}
