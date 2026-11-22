package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.RemindlyApplication
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderHistoryEntry
import com.example.ui.theme.accentColorArgbFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootAndTimeReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (action !in SUPPORTED_ACTIONS) return

    val pendingResult = goAsync()
    val appContext = context.applicationContext

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val app = appContext as RemindlyApplication
        val prefs = app.container.preferencesRepository.currentPreferences()
        NotificationChannels.ensureChannels(appContext, prefs.alarmBypassDnd)

        val activeReminders = app.container.reminderRepository.getAllActiveRemindersOnce()
        val missed =
          app.container.scheduler.rescheduleAll(
            reminders = activeReminders,
            defaultHour = prefs.defaultHour,
            defaultMinute = prefs.defaultMinute,
          )
        app.container.scheduler.scheduleWeeklyBackupReminder(prefs.weeklyBackupReminderEnabled)

        val now = System.currentTimeMillis()
        missed.forEach { reminder ->
          app.container.reminderRepository.recordHistoryIfNotLogged(
            ReminderHistoryEntry(
              reminderId = reminder.id,
              reminderTitle = reminder.title,
              category = reminder.category,
              eventType = HistoryEventType.MISSED,
              deliveryChannel = reminder.primaryDeliveryChannel(),
              occurrenceEpochMillis = reminder.nextOccurrenceEpochMillis,
              eventEpochMillis = now,
            )
          )
        }

        if (missed.isNotEmpty() &&
          (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) &&
          PermissionAndOemHelper.hasNotificationPermission(appContext)
        ) {
          postMissedSummaryNotification(
            context = appContext,
            count = missed.size,
            firstTitle = missed.first().title,
            accentColorArgb = accentColorArgbFor(prefs.themeMode),
          )
        }
        app.container.onWidgetRefreshRequested()
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun postMissedSummaryNotification(
    context: Context,
    count: Int,
    firstTitle: String,
    accentColorArgb: Int,
  ) {
    val openIntent =
      PendingIntent.getActivity(
        context,
        MISSED_NOTIFICATION_ID,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val title =
      if (count == 1) "1 missed reminder"
      else "$count missed reminders"
    val body =
      if (count == 1) "\"$firstTitle\" was due while your device was off."
      else "\"$firstTitle\" and ${count - 1} more were due while your device was off."

    val notification =
      NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MISSED)
        .setSmallIcon(R.drawable.ic_stat_remindly)
        .setColor(accentColorArgb)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setContentIntent(openIntent)
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(MISSED_NOTIFICATION_ID, notification)
    }
  }

  companion object {
    private const val MISSED_NOTIFICATION_ID = 77_001

    private val SUPPORTED_ACTIONS =
      setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_LOCKED_BOOT_COMPLETED,
        "android.intent.action.QUICKBOOT_POWERON",
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_DATE_CHANGED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
      )
  }
}
