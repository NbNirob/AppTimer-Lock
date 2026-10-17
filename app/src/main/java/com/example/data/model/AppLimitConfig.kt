package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitConfig(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val cycleStartTimeMs: Long,
    val cycleEndTimeMs: Long,
    val accumulatedUsageMs: Long = 0L,
    val isLocked: Boolean = false,
    val warning5MinSent: Boolean = false,
    val warning1MinSent: Boolean = false,
    val lastForegroundTimestampMs: Long = 0L,
    val isEnabled: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val limitMs: Long
        get() = dailyLimitMinutes * 60 * 1000L

    val remainingMs: Long
        get() = (limitMs - accumulatedUsageMs).coerceAtLeast(0L)

    val progressFraction: Float
        get() = if (limitMs > 0) {
            (accumulatedUsageMs.toFloat() / limitMs.toFloat()).coerceIn(0f, 1f)
        } else 1f
}
