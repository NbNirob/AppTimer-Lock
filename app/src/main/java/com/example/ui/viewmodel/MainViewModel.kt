package com.example.ui.viewmodel

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLimitConfig
import com.example.data.model.InstalledAppInfo
import com.example.data.model.UsageHistoryRecord
import com.example.data.repository.AppTimerRepository
import com.example.service.AppLockAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppTimerRepository,
    private val context: Context
) : ViewModel() {

    val configs: StateFlow<List<AppLimitConfig>> = repository.allConfigs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<UsageHistoryRecord>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _hasAccessibility = MutableStateFlow(false)
    val hasAccessibility: StateFlow<Boolean> = _hasAccessibility.asStateFlow()

    private val _hasNotification = MutableStateFlow(false)
    val hasNotification: StateFlow<Boolean> = _hasNotification.asStateFlow()

    init {
        refreshPermissions()
        refreshInstalledApps()
        evaluateCycles()
    }

    fun refreshPermissions() {
        _hasUsageAccess.value = checkUsageAccess(context)
        _hasAccessibility.value = checkAccessibility(context)
        _hasNotification.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            try {
                val apps = repository.getInstalledLaunchableApps()
                _installedApps.value = apps
            } catch (e: Exception) {
                // Defensive
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    fun evaluateCycles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.evaluateAllCycles()
            } catch (e: Exception) {
                // Defensive
            }
        }
    }

    fun setAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addOrUpdateLimit(packageName, appName, limitMinutes)
            refreshInstalledApps()
        }
    }

    fun removeAppLimit(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeLimit(packageName)
            refreshInstalledApps()
        }
    }

    fun toggleAppLimit(packageName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleEnabled(packageName, enabled)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    private fun checkUsageAccess(ctx: Context): Boolean {
        return try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun checkAccessibility(ctx: Context): Boolean {
        if (AppLockAccessibilityService.isServiceActive.value) return true
        return try {
            val enabledServices = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(ctx.packageName)
        } catch (e: Exception) {
            false
        }
    }

    class Factory(
        private val repository: AppTimerRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, context) as T
        }
    }
}
