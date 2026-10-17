package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.AppTimerApplication
import com.example.data.model.AppLimitConfig
import com.example.engine.UsageEngine
import com.example.notification.NotificationHelper
import com.example.ui.BlockedAppActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    private var currentForegroundPackage: String? = null
    private var sessionStartTimeMs: Long = 0L
    private var isScreenOnAndUnlocked: Boolean = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOnAndUnlocked = false
                    flushCurrentSession()
                    stopTicker()
                }
                Intent.ACTION_USER_PRESENT -> {
                    isScreenOnAndUnlocked = true
                    sessionStartTimeMs = System.currentTimeMillis()
                    startTickerIfMonitored()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceActive.value = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)

        // Evaluate all cycles on connect
        serviceScope.launch(Dispatchers.IO) {
            try {
                AppTimerApplication.instance.repository.evaluateAllCycles()
            } catch (e: Exception) {
                // Defensive
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val newPackage = event.packageName?.toString() ?: return

        if (newPackage == packageName || newPackage == currentForegroundPackage) {
            return
        }

        // Flush previous session
        flushCurrentSession()

        currentForegroundPackage = newPackage
        sessionStartTimeMs = System.currentTimeMillis()

        serviceScope.launch(Dispatchers.Main) {
            checkAndHandlePackageSwitch(newPackage)
        }
    }

    private suspend fun checkAndHandlePackageSwitch(pkg: String) {
        val repo = AppTimerApplication.instance.repository
        val config = repo.getConfigSync(pkg)

        if (config == null || !config.isEnabled) {
            stopTicker()
            return
        }

        // Check if cycle expired and rolls over
        val now = System.currentTimeMillis()
        val evalResult = UsageEngine.evaluateCycle(config, now)
        val activeConfig = when (evalResult) {
            is UsageEngine.CycleCheckResult.RolledOver -> {
                repo.evaluateAllCycles()
                evalResult.newConfig
            }
            is UsageEngine.CycleCheckResult.Active -> evalResult.updatedConfig
        }

        if (activeConfig.isLocked || activeConfig.accumulatedUsageMs >= activeConfig.limitMs) {
            launchBlockedScreen(activeConfig)
            stopTicker()
        } else if (isScreenOnAndUnlocked) {
            startTicker(activeConfig.packageName)
        }
    }

    private fun startTicker(pkg: String) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch(Dispatchers.IO) {
            var lastTick = System.currentTimeMillis()
            while (isActive && currentForegroundPackage == pkg && isScreenOnAndUnlocked) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val delta = (now - lastTick).coerceAtLeast(0L)
                lastTick = now

                val repo = AppTimerApplication.instance.repository
                val (updated, warning) = repo.recordForegroundDelta(pkg, delta, now)

                if (updated != null) {
                    when (warning) {
                        UsageEngine.WarningState.FIVE_MINUTES -> {
                            NotificationHelper.showUsageWarning(
                                this@AppLockAccessibilityService,
                                updated.appName,
                                5,
                                pkg.hashCode()
                            )
                        }
                        UsageEngine.WarningState.ONE_MINUTE -> {
                            NotificationHelper.showUsageWarning(
                                this@AppLockAccessibilityService,
                                updated.appName,
                                1,
                                pkg.hashCode()
                            )
                        }
                        UsageEngine.WarningState.LIMIT_EXPIRED -> {
                            NotificationHelper.showLimitExpiredNotification(
                                this@AppLockAccessibilityService,
                                updated.appName,
                                pkg.hashCode()
                            )
                            launchBlockedScreen(updated)
                            break
                        }
                        UsageEngine.WarningState.NONE -> {}
                    }

                    if (updated.isLocked) {
                        launchBlockedScreen(updated)
                        break
                    }
                }
            }
        }
    }

    private fun startTickerIfMonitored() {
        val pkg = currentForegroundPackage ?: return
        serviceScope.launch {
            val config = AppTimerApplication.instance.repository.getConfigSync(pkg)
            if (config != null && config.isEnabled && !config.isLocked) {
                startTicker(pkg)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun flushCurrentSession() {
        val pkg = currentForegroundPackage ?: return
        if (sessionStartTimeMs > 0L && isScreenOnAndUnlocked) {
            val now = System.currentTimeMillis()
            val delta = (now - sessionStartTimeMs).coerceAtLeast(0L)
            sessionStartTimeMs = 0L

            serviceScope.launch(Dispatchers.IO) {
                try {
                    AppTimerApplication.instance.repository.recordForegroundDelta(pkg, delta, now)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun launchBlockedScreen(config: AppLimitConfig) {
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, config.packageName)
            putExtra(BlockedAppActivity.EXTRA_APP_NAME, config.appName)
            putExtra(BlockedAppActivity.EXTRA_LOCK_UNTIL_MS, config.cycleEndTimeMs)
            putExtra(BlockedAppActivity.EXTRA_LIMIT_MINUTES, config.dailyLimitMinutes)
            putExtra(BlockedAppActivity.EXTRA_USAGE_MINUTES, (config.accumulatedUsageMs / 60000L).toInt())
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        flushCurrentSession()
        stopTicker()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        flushCurrentSession()
        stopTicker()
        serviceScope.cancel()
    }

    companion object {
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()
    }
}
