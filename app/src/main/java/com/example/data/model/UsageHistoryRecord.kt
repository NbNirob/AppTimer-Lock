package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_history")
data class UsageHistoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val cycleStartMs: Long,
    val cycleEndMs: Long,
    val totalUsedMinutes: Int,
    val limitMinutes: Int,
    val wasBlocked: Boolean,
    val completedAtMs: Long = System.currentTimeMillis()
)
