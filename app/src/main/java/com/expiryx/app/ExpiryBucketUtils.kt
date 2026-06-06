package com.expiryx.app

import kotlin.math.floor

object ExpiryBucketUtils {

    private const val DAY_MS = 86_400_000L

    data class BucketDefinition(
        val label: String,
        val colorRes: Int,
        val matches: (dayDiff: Long?) -> Boolean,
    )

    private val bucketDefinitions: List<BucketDefinition> = listOf(
        BucketDefinition("Expired", R.color.red) { it != null && it < 0 },
        BucketDefinition("Expiring today", R.color.orange) { it == 0L },
        BucketDefinition("Expiring tomorrow", R.color.yellow) { it == 1L },
        BucketDefinition("Expiring in 2-3 days", R.color.green) { it != null && it in 2L..3L },
        BucketDefinition("Expiring in 4-14 days", R.color.blue) { it != null && it in 4L..14L },
        BucketDefinition("Expiring in 15-90 days", R.color.grey_600) { it != null && it in 15L..90L },
        BucketDefinition("Expiring in 3-12 months", R.color.purple) { it != null && it in 91L..365L },
        BucketDefinition("Expiring in 1+ year", R.color.gray) { it != null && it > 365L },
        BucketDefinition("No expiry date", R.color.gray) { it == null },
    )

    fun countByBucket(products: List<Product>, now: Long = System.currentTimeMillis()): List<ExpiryBucketStat> {
        val startToday = getStartOfDay(now)
        return bucketDefinitions.mapNotNull { def ->
            val count = products.count { product ->
                def.matches(dayDiff(product.expirationDate, startToday))
            }
            if (count > 0) ExpiryBucketStat(def.label, count, def.colorRes) else null
        }
    }

    private fun dayDiff(expiryMillis: Long?, startToday: Long): Long? {
        expiryMillis ?: return null
        val startExpiry = getStartOfDay(expiryMillis)
        val diffMs = startExpiry - startToday
        return floor(diffMs.toDouble() / DAY_MS).toLong()
    }

    fun getStartOfDay(ts: Long): Long {
        return java.util.Calendar.getInstance().apply {
            timeInMillis = ts
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
