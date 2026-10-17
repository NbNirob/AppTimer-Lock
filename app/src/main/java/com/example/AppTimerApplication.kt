package com.example

import android.app.Application
import com.example.data.local.AppTimerDatabase
import com.example.data.repository.AppTimerRepository
import com.example.notification.NotificationHelper

class AppTimerApplication : Application() {

    lateinit var database: AppTimerDatabase
        private set

    lateinit var repository: AppTimerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppTimerDatabase.getInstance(this)
        repository = AppTimerRepository(
            appLimitDao = database.appLimitDao(),
            usageHistoryDao = database.usageHistoryDao(),
            context = this
        )

        NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: AppTimerApplication
            private set
    }
}
