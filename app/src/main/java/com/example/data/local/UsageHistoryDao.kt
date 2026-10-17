package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UsageHistoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageHistoryDao {

    @Query("SELECT * FROM usage_history ORDER BY completedAtMs DESC")
    fun getAllHistory(): Flow<List<UsageHistoryRecord>>

    @Query("SELECT * FROM usage_history WHERE packageName = :packageName ORDER BY completedAtMs DESC")
    fun getHistoryForPackage(packageName: String): Flow<List<UsageHistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: UsageHistoryRecord)

    @Query("DELETE FROM usage_history")
    suspend fun clearAllHistory()
}
