package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    private const val CHANNEL_WARNINGS = "apptimer_warnings"
    private const val CHANNEL_NAME = "AppTimer Usage Warnings"
    private const val CHANNEL_DESC = "Alerts when an app is nearing or reaching daily limit"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_WARNINGS, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showUsageWarning(
        context: Context,
        appName: String,
        minutesRemaining: Int,
        notificationId: Int
    ) {
        val title = "AppTimer Lock সতর্কতা"
        val message = when (minutesRemaining) {
            5 -> "সাবধান! '$appName' এর দৈনিক সীমা শেষ হতে আর ৫ মিনিট বাকি রয়েছে।"
            1 -> "মনোযোগ দিন! '$appName' আর মাত্র ১ মিনিট ব্যবহার করা যাবে।"
            else -> "'$appName' ব্যবহারের সময় দ্রুত শেষ হয়ে আসছে ($minutesRemaining মিনিট বাকি)।"
        }

        sendNotification(context, title, message, notificationId)
    }

    fun showLimitExpiredNotification(
        context: Context,
        appName: String,
        notificationId: Int
    ) {
        val title = "অ্যাপ লক করা হয়েছে!"
        val message = "'$appName' এর আজকের বরাদ্দকৃত সময় সম্পূর্ণ শেষ হয়ে গেছে। অ্যাক্সেস সাময়িকভাবে বন্ধ করা হলো।"
        sendNotification(context, title, message, notificationId)
    }

    private fun sendNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_WARNINGS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) {
                manager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Android 13 notification permission might not be granted yet
        }
    }
}
