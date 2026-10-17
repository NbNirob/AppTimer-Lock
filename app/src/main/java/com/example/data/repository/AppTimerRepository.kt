package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.local.AppLimitDao
import com.example.data.local.UsageHistoryDao
import com.example.data.model.AppLimitConfig
import com.example.data.model.InstalledAppInfo
import com.example.data.model.UsageHistoryRecord
import com.example.engine.UsageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppTimerRepository(
    private val appLimitDao: AppLimitDao,
    private val usageHistoryDao: UsageHistoryDao,
    private val context: Context
) {
    val allConfigs: Flow<List<AppLimitConfig>> = appLimitDao.getAllConfigs()
    val allHistory: Flow<List<UsageHistoryRecord>> = usageHistoryDao.getAllHistory()

    fun getConfig(packageName: String): Flow<AppLimitConfig?> = appLimitDao.getConfig(packageName)

    suspend fun getConfigSync(packageName: String): AppLimitConfig? = withContext(Dispatchers.IO) {
        appLimitDao.getConfigSync(packageName)
    }

    suspend fun addOrUpdateLimit(packageName: String, appName: String, limitMinutes: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = appLimitDao.getConfigSync(packageName)
        if (existing == null) {
            val newConfig = AppLimitConfig(
                packageName = packageName,
                appName = appName,
                dailyLimitMinutes = limitMinutes,
                cycleStartTimeMs = now,
                cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
                accumulatedUsageMs = 0L,
                isLocked = false,
                warning5MinSent = false,
                warning1MinSent = false,
                lastForegroundTimestampMs = 0L,
                isEnabled = true,
                updatedAtMs = now
            )
            appLimitDao.insertOrUpdate(newConfig)
        } else {
            // Update limit and recalculate lock status
            val limitMs = limitMinutes * 60 * 1000L
            val isLocked = existing.accumulatedUsageMs >= limitMs
            val updated = existing.copy(
                appName = appName,
                dailyLimitMinutes = limitMinutes,
                isLocked = isLocked,
                updatedAtMs = now
            )
            appLimitDao.update(updated)
        }
    }

    suspend fun removeLimit(packageName: String) = withContext(Dispatchers.IO) {
        appLimitDao.deleteByPackageName(packageName)
    }

    suspend fun toggleEnabled(packageName: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val config = appLimitDao.getConfigSync(packageName) ?: return@withContext
        appLimitDao.update(config.copy(isEnabled = enabled, updatedAtMs = System.currentTimeMillis()))
    }

    /**
     * Checks and rolls over 24-hour cycles for all monitored apps.
     */
    suspend fun evaluateAllCycles(): List<AppLimitConfig> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val configs = appLimitDao.getAllConfigsSync()
        val activeConfigs = mutableListOf<AppLimitConfig>()

        for (cfg in configs) {
            when (val result = UsageEngine.evaluateCycle(cfg, now)) {
                is UsageEngine.CycleCheckResult.RolledOver -> {
                    // Archive previous cycle
                    usageHistoryDao.insertRecord(result.completedRecord)
                    // Persist new cycle
                    appLimitDao.update(result.newConfig)
                    activeConfigs.add(result.newConfig)
                }
                is UsageEngine.CycleCheckResult.Active -> {
                    if (result.updatedConfig != cfg) {
                        appLimitDao.update(result.updatedConfig)
                    }
                    activeConfigs.add(result.updatedConfig)
                }
            }
        }
        activeConfigs
    }

    /**
     * Records foreground usage delta for an app.
     */
    suspend fun recordForegroundDelta(
        packageName: String,
        deltaMs: Long,
        currentTimeMs: Long
    ): Pair<AppLimitConfig?, UsageEngine.WarningState> = withContext(Dispatchers.IO) {
        var config = appLimitDao.getConfigSync(packageName) ?: return@withContext Pair(null, UsageEngine.WarningState.NONE)

        // Evaluate cycle rollover first
        when (val eval = UsageEngine.evaluateCycle(config, currentTimeMs)) {
            is UsageEngine.CycleCheckResult.RolledOver -> {
                usageHistoryDao.insertRecord(eval.completedRecord)
                config = eval.newConfig
                appLimitDao.update(config)
            }
            is UsageEngine.CycleCheckResult.Active -> {
                config = eval.updatedConfig
            }
        }

        if (!config.isEnabled) {
            return@withContext Pair(config, UsageEngine.WarningState.NONE)
        }

        val (updatedConfig, warning) = UsageEngine.addForegroundUsage(config, deltaMs, currentTimeMs)
        appLimitDao.update(updatedConfig)
        Pair(updatedConfig, warning)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        usageHistoryDao.clearAllHistory()
    }

    suspend fun getInstalledLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        val monitoredMap = appLimitDao.getAllConfigsSync().associateBy { it.packageName }
        val ourPackage = context.packageName

        val result = mutableListOf<InstalledAppInfo>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            val isExcluded = UsageEngine.isPackageExcluded(pkg, ourPackage)
            val icon = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            val config = monitoredMap[pkg]
            result.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    isMonitored = config != null,
                    currentLimitMinutes = config?.dailyLimitMinutes ?: 30,
                    isSystemOrEmergency = isExcluded
                )
            )
        }

        result.distinctBy { it.packageName }.sortedWith(
            compareByDescending<InstalledAppInfo> { it.isMonitored }
                .thenBy { it.isSystemOrEmergency }
                .thenBy { it.appName.lowercase() }
        )
    }
}
