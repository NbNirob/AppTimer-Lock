package com.example.engine

import com.example.data.model.AppLimitConfig
import com.example.data.model.UsageHistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UsageEngine {

    const val CYCLE_DURATION_MS: Long = 24 * 60 * 60 * 1000L // 24 Hours
    const val FIVE_MINUTES_MS: Long = 5 * 60 * 1000L
    const val ONE_MINUTE_MS: Long = 1 * 60 * 1000L

    val EXCLUDED_PACKAGES: Set<String> = setOf(
        "com.example",
        "com.nirob.apptimerlock",
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.securitycenter",
        "com.miui.home"
    )

    fun isPackageExcluded(packageName: String, ourPackageName: String? = null): Boolean {
        if (packageName.isBlank()) return true
        if (ourPackageName != null && packageName == ourPackageName) return true
        if (EXCLUDED_PACKAGES.contains(packageName)) return true
        if (packageName.startsWith("com.android.internal") || packageName.startsWith("com.google.android.inputmethod")) {
            return true
        }
        return false
    }

    sealed class CycleCheckResult {
        data class Active(val updatedConfig: AppLimitConfig) : CycleCheckResult()
        data class RolledOver(
            val newConfig: AppLimitConfig,
            val completedRecord: UsageHistoryRecord
        ) : CycleCheckResult()
    }

    enum class WarningState {
        NONE,
        FIVE_MINUTES,
        ONE_MINUTE,
        LIMIT_EXPIRED
    }

    /**
     * Checks if the 24-hour cycle has expired or clock shifted backwards defensively.
     * Never resets accumulated usage merely because process died; only resets when
     * 24 hours have actually elapsed.
     */
    fun evaluateCycle(config: AppLimitConfig, currentTimeMs: Long): CycleCheckResult {
        // Defensive check: Clock shifted backwards significantly
        if (currentTimeMs < config.cycleStartTimeMs) {
            val adjusted = config.copy(
                cycleStartTimeMs = currentTimeMs,
                cycleEndTimeMs = currentTimeMs + CYCLE_DURATION_MS,
                updatedAtMs = currentTimeMs
            )
            return CycleCheckResult.Active(adjusted)
        }

        // Has 24h cycle expired?
        if (currentTimeMs >= config.cycleEndTimeMs) {
            val record = UsageHistoryRecord(
                packageName = config.packageName,
                appName = config.appName,
                cycleStartMs = config.cycleStartTimeMs,
                cycleEndMs = config.cycleEndTimeMs,
                totalUsedMinutes = (config.accumulatedUsageMs / 60000L).toInt(),
                limitMinutes = config.dailyLimitMinutes,
                wasBlocked = config.isLocked || config.accumulatedUsageMs >= config.limitMs,
                completedAtMs = currentTimeMs
            )

            val newConfig = config.copy(
                cycleStartTimeMs = currentTimeMs,
                cycleEndTimeMs = currentTimeMs + CYCLE_DURATION_MS,
                accumulatedUsageMs = 0L,
                isLocked = false,
                warning5MinSent = false,
                warning1MinSent = false,
                lastForegroundTimestampMs = 0L,
                updatedAtMs = currentTimeMs
            )

            return CycleCheckResult.RolledOver(newConfig, record)
        }

        return CycleCheckResult.Active(config)
    }

    /**
     * Calculates usage update when foreground session finishes or ticks.
     */
    fun addForegroundUsage(
        config: AppLimitConfig,
        sessionDeltaMs: Long,
        currentTimeMs: Long
    ): Pair<AppLimitConfig, WarningState> {
        val safeDelta = sessionDeltaMs.coerceAtLeast(0L)
        val newAccumulated = config.accumulatedUsageMs + safeDelta
        val limitMs = config.limitMs
        val remainingMs = (limitMs - newAccumulated).coerceAtLeast(0L)

        val isNowLocked = newAccumulated >= limitMs
        var warningToSend = WarningState.NONE
        var warning5Sent = config.warning5MinSent
        var warning1Sent = config.warning1MinSent

        if (isNowLocked) {
            warningToSend = WarningState.LIMIT_EXPIRED
        } else if (limitMs > FIVE_MINUTES_MS && remainingMs <= FIVE_MINUTES_MS && !warning5Sent) {
            warningToSend = WarningState.FIVE_MINUTES
            warning5Sent = true
        } else if (limitMs > ONE_MINUTE_MS && remainingMs <= ONE_MINUTE_MS && !warning1Sent) {
            warningToSend = WarningState.ONE_MINUTE
            warning1Sent = true
        }

        val updated = config.copy(
            accumulatedUsageMs = newAccumulated,
            isLocked = isNowLocked,
            warning5MinSent = warning5Sent,
            warning1MinSent = warning1Sent,
            updatedAtMs = currentTimeMs
        )

        return Pair(updated, warningToSend)
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours} ঘণ্টা ${mins} মিনিট"
            hours > 0 -> "${hours} ঘণ্টা"
            else -> "${mins} মিনিট"
        }
    }

    fun formatMsToDisplay(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatTimeRemainingHuman(ms: Long): String {
        val totalMinutes = (ms / 60000L).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours} ঘ ${mins} মি"
            hours > 0 -> "${hours} ঘণ্টা"
            mins > 0 -> "${mins} মিনিট"
            else -> "<১ মিনিট"
        }
    }

    fun formatTimestamp(timestampMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
