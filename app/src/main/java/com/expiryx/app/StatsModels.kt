package com.expiryx.app

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

enum class TimeRange(@StringRes val labelRes: Int, val days: Int?) {
    DAYS_7(R.string.stats_range_7d, 7),
    DAYS_30(R.string.stats_range_30d, 30),
    DAYS_90(R.string.stats_range_90d, 90),
    YEAR(R.string.stats_range_1y, 365),
    ALL(R.string.stats_range_all, null);

    fun startMillis(now: Long = System.currentTimeMillis()): Long? {
        days ?: return null
        return now - days * DAY_MS
    }

    companion object {
        private const val DAY_MS = 86_400_000L
    }
}

data class ExpiryBucketStat(
    val label: String,
    val count: Int,
    @param:ColorRes val colorRes: Int,
)

data class BrandStat(
    val brand: String,
    val count: Int,
)

data class WeeklyActivity(
    val weekLabel: String,
    val weekStartMillis: Long,
    val added: Int = 0,
    val used: Int = 0,
    val expired: Int = 0,
    val deleted: Int = 0,
)

data class StatsUiState(
    val timeRange: TimeRange = TimeRange.DAYS_30,
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true,
    val hasHistoryInRange: Boolean = false,
    val activeItems: Int = 0,
    val activeQuantity: Int = 0,
    val consumedCount: Int = 0,
    val expiredCount: Int = 0,
    val deletedCount: Int = 0,
    val wasteRate: Float = 0f,
    val itemsAddedInRange: Int = 0,
    val avgDaysToUse: Float? = null,
    val consumedBeforeExpiryPercent: Float = 0f,
    val lifecycleUsed: Int = 0,
    val lifecycleExpired: Int = 0,
    val lifecycleDeleted: Int = 0,
    val weeklyActivity: List<WeeklyActivity> = emptyList(),
    val expiryBuckets: List<ExpiryBucketStat> = emptyList(),
    val topBrandsConsumed: List<BrandStat> = emptyList(),
    val topBrandsWasted: List<BrandStat> = emptyList(),
    val favoritesCount: Int = 0,
    val barcodeScanRate: Float = 0f,
    val noExpiryCount: Int = 0,
    val totalWeightG: Int = 0,
    val totalVolumeMl: Int = 0,
)
