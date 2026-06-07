package com.expiryx.app

import kotlin.math.floor

enum class ExpiryTrafficLight {
    EXPIRED,
    URGENT,
    SAFE,
    UNKNOWN,
}

object ExpiryTrafficLightUtils {

    private const val DAY_MS = 86_400_000L

    fun dayDiff(expiryMillis: Long?, now: Long = System.currentTimeMillis()): Long? {
        expiryMillis ?: return null
        val startToday = ExpiryBucketUtils.getStartOfDay(now)
        val startExpiry = ExpiryBucketUtils.getStartOfDay(expiryMillis)
        return floor((startExpiry - startToday).toDouble() / DAY_MS).toLong()
    }

    fun classify(expiryMillis: Long?, now: Long = System.currentTimeMillis()): ExpiryTrafficLight {
        val diff = dayDiff(expiryMillis, now) ?: return ExpiryTrafficLight.UNKNOWN
        return when {
            diff < 0 -> ExpiryTrafficLight.EXPIRED
            diff <= 3 -> ExpiryTrafficLight.URGENT
            else -> ExpiryTrafficLight.SAFE
        }
    }
}
