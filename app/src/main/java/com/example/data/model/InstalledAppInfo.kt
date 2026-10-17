package com.example.data.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isMonitored: Boolean = false,
    val currentLimitMinutes: Int = 30,
    val isSystemOrEmergency: Boolean = false
)
