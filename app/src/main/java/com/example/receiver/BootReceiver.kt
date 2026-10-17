package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AppTimerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Defensively restore state, evaluate cycles across reboot
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AppTimerApplication.instance.repository.evaluateAllCycles()
                } catch (e: Exception) {
                    // Safe error handling
                }
            }
        }
    }
}
