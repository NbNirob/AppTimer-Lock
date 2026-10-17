package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppLimitConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    @Query("SELECT * FROM app_limits ORDER BY isLocked DESC, appName ASC")
    fun getAllConfigs(): Flow<List<AppLimitConfig>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAllConfigsSync(): List<AppLimitConfig>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    fun getConfig(packageName: String): Flow<AppLimitConfig?>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getConfigSync(packageName: String): AppLimitConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: AppLimitConfig)

    @Update
    suspend fun update(config: AppLimitConfig)

    @Delete
    suspend fun delete(config: AppLimitConfig)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("""
        UPDATE app_limits 
        SET accumulatedUsageMs = :accumulatedUsageMs,
            isLocked = :isLocked,
            warning5MinSent = :warning5MinSent,
            warning1MinSent = :warning1MinSent,
            lastForegroundTimestampMs = :lastForegroundTimestampMs,
            updatedAtMs = :updatedAtMs
        WHERE packageName = :packageName
    """)
    suspend fun updateUsage(
        packageName: String,
        accumulatedUsageMs: Long,
        isLocked: Boolean,
        warning5MinSent: Boolean,
        warning1MinSent: Boolean,
        lastForegroundTimestampMs: Long,
        updatedAtMs: Long
    )

    @Query("""
        UPDATE app_limits
        SET cycleStartTimeMs = :cycleStartMs,
            cycleEndTimeMs = :cycleEndMs,
            accumulatedUsageMs = 0,
            isLocked = 0,
            warning5MinSent = 0,
            warning1MinSent = 0,
            lastForegroundTimestampMs = 0,
            updatedAtMs = :updatedAtMs
        WHERE packageName = :packageName
    """)
    suspend fun resetCycle(
        packageName: String,
        cycleStartMs: Long,
        cycleEndMs: Long,
        updatedAtMs: Long
    )
}
