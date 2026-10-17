package com.example.engine

import com.example.data.model.AppLimitConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageEngineTest {

    @Test
    fun testUsageLimitCalculationAndLock() {
        val now = 1000000L
        val config = AppLimitConfig(
            packageName = "com.social.app",
            appName = "Social App",
            dailyLimitMinutes = 30,
            cycleStartTimeMs = now,
            cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
            accumulatedUsageMs = 0L,
            isLocked = false
        )

        // Add 10 minutes (600,000 ms)
        val (after10Min, warning1) = UsageEngine.addForegroundUsage(config, 600000L, now + 600000L)
        assertFalse("App should not be locked after 10 mins of 30 min limit", after10Min.isLocked)
        assertEquals(600000L, after10Min.accumulatedUsageMs)
        assertEquals(UsageEngine.WarningState.NONE, warning1)

        // Add 20 more minutes (total 30 mins) -> should lock and issue LIMIT_EXPIRED
        val (after30Min, warning2) = UsageEngine.addForegroundUsage(after10Min, 1200000L, now + 1800000L)
        assertTrue("App should be locked when reaching 30 mins limit", after30Min.isLocked)
        assertEquals(1800000L, after30Min.accumulatedUsageMs)
        assertEquals(UsageEngine.WarningState.LIMIT_EXPIRED, warning2)
    }

    @Test
    fun testWarningThresholdsFiveAndOneMinute() {
        val now = 1000000L
        // 10 minutes limit (600,000 ms). 5-min threshold is 5 mins used (300,000 ms remaining).
        val config = AppLimitConfig(
            packageName = "com.video.app",
            appName = "Video App",
            dailyLimitMinutes = 10,
            cycleStartTimeMs = now,
            cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
            accumulatedUsageMs = 0L,
            isLocked = false
        )

        // Reach 5 minutes remaining (5 mins used = 300,000 ms)
        val (state5Min, warn5) = UsageEngine.addForegroundUsage(config, 300000L, now + 300000L)
        assertEquals(UsageEngine.WarningState.FIVE_MINUTES, warn5)
        assertTrue(state5Min.warning5MinSent)

        // Second tick at 5.5 minutes used: should not spam warning again
        val (state5MinTick, warnNoRepeat) = UsageEngine.addForegroundUsage(state5Min, 30000L, now + 330000L)
        assertEquals(UsageEngine.WarningState.NONE, warnNoRepeat)

        // Reach 1 minute remaining (9 mins used = 540,000 ms total, so delta = 210,000 ms)
        val (state1Min, warn1) = UsageEngine.addForegroundUsage(state5MinTick, 210000L, now + 540000L)
        assertEquals(UsageEngine.WarningState.ONE_MINUTE, warn1)
        assertTrue(state1Min.warning1MinSent)
    }

    @Test
    fun testIndependentAppTimers() {
        val now = 1000000L
        val appA = AppLimitConfig(
            packageName = "com.app.a",
            appName = "App A",
            dailyLimitMinutes = 15,
            cycleStartTimeMs = now,
            cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
            accumulatedUsageMs = 0L,
            isLocked = false
        )

        val appB = AppLimitConfig(
            packageName = "com.app.b",
            appName = "App B",
            dailyLimitMinutes = 60,
            cycleStartTimeMs = now,
            cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
            accumulatedUsageMs = 0L,
            isLocked = false
        )

        // App A is used for 15 minutes (reaches limit)
        val (appAUpdated, _) = UsageEngine.addForegroundUsage(appA, 15 * 60 * 1000L, now + 900000L)
        assertTrue("App A should be locked", appAUpdated.isLocked)

        // App B was only used for 10 minutes
        val (appBUpdated, _) = UsageEngine.addForegroundUsage(appB, 10 * 60 * 1000L, now + 900000L)
        assertFalse("App B should NOT be locked", appBUpdated.isLocked)
        assertEquals(10 * 60 * 1000L, appBUpdated.accumulatedUsageMs)
    }

    @Test
    fun test24HourCycleExpirationAndUnlock() {
        val cycleStart = 10000000L
        val cycleEnd = cycleStart + UsageEngine.CYCLE_DURATION_MS

        val lockedApp = AppLimitConfig(
            packageName = "com.games.app",
            appName = "Games App",
            dailyLimitMinutes = 20,
            cycleStartTimeMs = cycleStart,
            cycleEndTimeMs = cycleEnd,
            accumulatedUsageMs = 20 * 60 * 1000L,
            isLocked = true,
            warning5MinSent = true,
            warning1MinSent = true
        )

        // Case 1: Within cycle (20 hours elapsed)
        val before24h = cycleStart + (20 * 60 * 60 * 1000L)
        val eval1 = UsageEngine.evaluateCycle(lockedApp, before24h)
        assertTrue("App should remain active and locked before 24h elapses", eval1 is UsageEngine.CycleCheckResult.Active)
        val active = (eval1 as UsageEngine.CycleCheckResult.Active).updatedConfig
        assertTrue(active.isLocked)
        assertEquals(20 * 60 * 1000L, active.accumulatedUsageMs)

        // Case 2: 24-hour cycle expires (24 hours and 1 millisecond elapsed)
        val after24h = cycleEnd + 1L
        val eval2 = UsageEngine.evaluateCycle(lockedApp, after24h)
        assertTrue("Cycle should roll over when 24h expires", eval2 is UsageEngine.CycleCheckResult.RolledOver)

        val rolled = eval2 as UsageEngine.CycleCheckResult.RolledOver
        // Verify previous cycle history was recorded accurately
        assertEquals("Games App", rolled.completedRecord.appName)
        assertEquals(20, rolled.completedRecord.totalUsedMinutes)
        assertEquals(20, rolled.completedRecord.limitMinutes)
        assertTrue("Previous record should reflect it was blocked", rolled.completedRecord.wasBlocked)

        // Verify the new cycle is fresh and UNLOCKED
        val newConfig = rolled.newConfig
        assertFalse("App MUST become accessible again (unlocked) when new cycle starts", newConfig.isLocked)
        assertEquals(0L, newConfig.accumulatedUsageMs)
        assertEquals(after24h, newConfig.cycleStartTimeMs)
        assertEquals(after24h + UsageEngine.CYCLE_DURATION_MS, newConfig.cycleEndTimeMs)
        assertFalse(newConfig.warning5MinSent)
        assertFalse(newConfig.warning1MinSent)
    }

    @Test
    fun testDefensiveClockRollback() {
        val now = 50000000L
        val config = AppLimitConfig(
            packageName = "com.sample.app",
            appName = "Sample",
            dailyLimitMinutes = 30,
            cycleStartTimeMs = now,
            cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
            accumulatedUsageMs = 10000L
        )

        // User sets clock back to 40000000L
        val rollBackTime = 40000000L
        val eval = UsageEngine.evaluateCycle(config, rollBackTime)
        assertTrue(eval is UsageEngine.CycleCheckResult.Active)
        val updated = (eval as UsageEngine.CycleCheckResult.Active).updatedConfig
        assertEquals(rollBackTime, updated.cycleStartTimeMs)
        assertEquals(rollBackTime + UsageEngine.CYCLE_DURATION_MS, updated.cycleEndTimeMs)
        // Accumulated usage remains protected
        assertEquals(10000L, updated.accumulatedUsageMs)
    }

    @Test
    fun testExcludedPackages() {
        assertTrue("Settings should be excluded", UsageEngine.isPackageExcluded("com.android.settings"))
        assertTrue("SystemUI should be excluded", UsageEngine.isPackageExcluded("com.android.systemui"))
        assertTrue("Emergency dialer should be excluded", UsageEngine.isPackageExcluded("com.android.phone"))
        assertTrue("Self should be excluded", UsageEngine.isPackageExcluded("com.example", "com.example"))
        assertTrue("Self by app id should be excluded", UsageEngine.isPackageExcluded("com.nirob.apptimerlock", "com.nirob.apptimerlock"))
        assertFalse("Third party app should NOT be excluded", UsageEngine.isPackageExcluded("com.instagram.android"))
    }
}
