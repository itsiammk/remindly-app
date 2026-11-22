package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.RemindlyApplication
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderHistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val appContext = context.applicationContext
    val reminderId = intent.getLongExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, -1L)
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
    val title = intent.getStringExtra(ReminderTriggerReceiver.EXTRA_TITLE) ?: "Reminder"
    val categoryName =
      intent.getStringExtra(ReminderTriggerReceiver.EXTRA_CATEGORY) ?: ReminderCategory.OTHER.name
    val channelName =
      intent.getStringExtra(ReminderTriggerReceiver.EXTRA_CHANNEL) ?: DeliveryChannel.NOTIFICATION.name
    val isTest = intent.getBooleanExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, false)

    if (notificationId > 0) {
      NotificationManagerCompat.from(appContext).cancel(notificationId)
    }

    // Stop alarm service if active
    appContext.stopService(Intent(appContext, AlarmRingingService::class.java))

    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val app = appContext as RemindlyApplication
        val prefs = app.container.preferencesRepository.currentPreferences()

        when (action) {
          ACTION_DONE -> {
            if (!isTest && reminderId > 0L) {
              app.container.reminderRepository.markReminderDone(reminderId)
            }
          }
          ACTION_SNOOZE -> {
            val category =
              runCatching { ReminderCategory.valueOf(categoryName) }
                .getOrDefault(ReminderCategory.OTHER)
            val channel =
              runCatching { DeliveryChannel.valueOf(channelName) }
                .getOrDefault(DeliveryChannel.NOTIFICATION)
            app.container.scheduler.scheduleSnooze(
              reminderId = if (reminderId > 0L) reminderId else 999_999L,
              title = title,
              category = category,
              deliveryChannel = channel,
              snoozeMinutes = prefs.snoozeDurationMinutes,
            )
            if (!isTest && reminderId > 0L) {
              val now = System.currentTimeMillis()
              app.container.reminderRepository.recordHistory(
                ReminderHistoryEntry(
                  reminderId = reminderId,
                  reminderTitle = title,
                  category = category,
                  eventType = HistoryEventType.SNOOZED,
                  deliveryChannel = channel,
                  offsetLabel = "Snoozed ${prefs.snoozeDurationMinutes} min",
                  occurrenceEpochMillis = now,
                  eventEpochMillis = now,
                )
              )
            }
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_DONE = "com.example.alarm.ACTION_DONE"
    const val ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
  }
}
