package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppLimitConfig
import com.example.data.model.UsageHistoryRecord

@Database(
    entities = [AppLimitConfig::class, UsageHistoryRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppTimerDatabase : RoomDatabase() {

    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageHistoryDao(): UsageHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppTimerDatabase? = null

        fun getInstance(context: Context): AppTimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppTimerDatabase::class.java,
                    "apptimer_lock.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
