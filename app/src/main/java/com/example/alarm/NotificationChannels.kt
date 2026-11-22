package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationChannels {
  const val CHANNEL_REMINDERS = "remindly_reminders"
  const val CHANNEL_ALARMS = "remindly_alarms"
  const val CHANNEL_MISSED = "remindly_missed"
  const val CHANNEL_BACKUP = "remindly_backup"

  fun ensureChannels(context: Context, alarmBypassDnd: Boolean = true) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    // 1. Standard Reminders channel (sound + vibration, high importance popup)
    val remindersChannel =
      NotificationChannel(
        CHANNEL_REMINDERS,
        "Reminders",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Quiet and standard scheduled reminders"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 250, 150, 250)
        setSound(
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        )
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }

    // 2. Alarms channel (silent on channel because AlarmRingingService plays on USAGE_ALARM)
    val alarmsChannel =
      NotificationChannel(
        CHANNEL_ALARMS,
        "Ringing Alarms",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Full-screen ringing alarms that play on the alarm volume"
        setSound(null, null)
        enableVibration(false)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        if (manager.isNotificationPolicyAccessGranted) {
          setBypassDnd(alarmBypassDnd)
        }
      }

    // 3. Missed reminders summary channel
    val missedChannel =
      NotificationChannel(
        CHANNEL_MISSED,
        "Missed Reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Summary of reminders that were due while your phone was off"
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }

    // 4. Weekly backup reminder channel
    val backupChannel =
      NotificationChannel(
        CHANNEL_BACKUP,
        "Weekly Backup Reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Gentle weekly reminder to export an offline backup"
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }

    manager.createNotificationChannel(remindersChannel)
    manager.createNotificationChannel(alarmsChannel)
    manager.createNotificationChannel(missedChannel)
    manager.createNotificationChannel(backupChannel)
  }
}
