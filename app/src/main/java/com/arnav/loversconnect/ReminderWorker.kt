// java/com/arnav/loversconnect/ReminderWorker.kt
package com.arnav.loversconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.os.Build

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val title = inputData.getString("REMINDER_TITLE") ?: "You have a reminder!"

        showNotification("Lovers Connect Reminder ❤️", title)

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lovers_connect_reminders"

        // THIS IS THE FIX: Only create the channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Add priority for older versions
            .build()

        notificationManager.notify(1, notification)
    }
}