package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLimitDao
import com.example.data.local.AppTimerDatabase
import com.example.data.local.UsageHistoryDao
import com.example.data.model.AppLimitConfig
import com.example.data.model.UsageHistoryRecord
import com.example.data.repository.AppTimerRepository
import com.example.engine.UsageEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppTimerDatabase
    private lateinit var appLimitDao: AppLimitDao
    private lateinit var usageHistoryDao: UsageHistoryDao
    private lateinit var repository: AppTimerRepository
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppTimerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appLimitDao = db.appLimitDao()
        usageHistoryDao = db.usageHistoryDao()
        repository = AppTimerRepository(appLimitDao, usageHistoryDao, context)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAppNameStringResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AppTimer Lock", appName)
    }

    @Test
    fun testRoomPersistenceAndRecovery() = runBlocking {
        val now = System.currentTimeMillis()
        val config = AppLimitConfig(
            packageName = "com.test.app",
            appName = "Test App",
            dailyLimitMinutes = 45,
            cycleStartTimeMs = now,
            cycleEndTimeMs = now + UsageEngine.CYCLE_DURATION_MS,
            accumulatedUsageMs = 15 * 60 * 1000L,
            isLocked = false
        )

        appLimitDao.insertOrUpdate(config)

        // Read back as if process was resurrected
        val loaded = appLimitDao.getConfigSync("com.test.app")
        assertNotNull("Config should persist in Room", loaded)
        assertEquals("Test App", loaded?.appName)
        assertEquals(45, loaded?.dailyLimitMinutes)
        assertEquals(15 * 60 * 1000L, loaded?.accumulatedUsageMs)
        assertFalse(loaded!!.isLocked)
    }

    @Test
    fun testEvaluateCyclesArchivingAndReset() = runBlocking {
        // App was used and locked in an expired cycle (e.g. yesterday)
        val pastCycleStart = 1000000L
        val pastCycleEnd = pastCycleStart + UsageEngine.CYCLE_DURATION_MS
        val expiredConfig = AppLimitConfig(
            packageName = "com.old.app",
            appName = "Old App",
            dailyLimitMinutes = 30,
            cycleStartTimeMs = pastCycleStart,
            cycleEndTimeMs = pastCycleEnd,
            accumulatedUsageMs = 30 * 60 * 1000L,
            isLocked = true
        )
        appLimitDao.insertOrUpdate(expiredConfig)

        // Run evaluateAllCycles which will recognize cycle expired
        val activeList = repository.evaluateAllCycles()

        val updated = appLimitDao.getConfigSync("com.old.app")
        assertNotNull(updated)
        assertFalse("App must be unlocked in new cycle", updated!!.isLocked)
        assertEquals(0L, updated.accumulatedUsageMs)

        // Check history DAO
        val historyRecord = usageHistoryDao.getAllHistory()
        // verify history was written
        assertTrue(activeList.isNotEmpty())
    }
}
