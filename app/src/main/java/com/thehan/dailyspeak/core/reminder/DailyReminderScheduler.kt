package com.thehan.dailyspeak.core.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.thehan.dailyspeak.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminderTime: String) {
        val triggerAtMillis = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = System.currentTimeMillis(),
            reminderTime = reminderTime,
        ) ?: return

        ensureNotificationChannel()
        val pendingIntent = reminderPendingIntent()
        alarmManager.cancel(pendingIntent)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent,
        )
    }

    fun cancel() {
        alarmManager.cancel(reminderPendingIntent())
    }

    fun ensureNotificationChannel() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun reminderPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REMINDER_REQUEST_CODE,
        Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "daily_practice_reminder"
        const val ACTION_DAILY_REMINDER = "com.thehan.dailyspeak.action.DAILY_REMINDER"
        private const val REMINDER_REQUEST_CODE = 4101
    }
}
