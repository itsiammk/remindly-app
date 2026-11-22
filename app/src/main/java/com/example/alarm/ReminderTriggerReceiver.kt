package com.example.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.RemindlyApplication
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderHistoryEntry
import com.example.domain.time.IstTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderTriggerReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_WEEKLY_BACKUP) {
      handleWeeklyBackup(context.applicationContext)
      return
    }
    if (intent.action != ACTION_TRIGGER) return

    val pendingResult = goAsync()
    val appContext = context.applicationContext

    val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
    val fallbackTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
    val fallbackNote = intent.getStringExtra(EXTRA_NOTE).orEmpty()
    val categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: ReminderCategory.OTHER.name
    val channelName = intent.getStringExtra(EXTRA_CHANNEL) ?: DeliveryChannel.NOTIFICATION.name
    val occurrenceMillis = intent.getLongExtra(EXTRA_OCCURRENCE_MILLIS, System.currentTimeMillis())
    val offsetLabel = intent.getStringExtra(EXTRA_OFFSET_LABEL).orEmpty()
    val isFallback = intent.getBooleanExtra(EXTRA_IS_FALLBACK, false)
    val isFinalForOccurrence = intent.getBooleanExtra(EXTRA_IS_FINAL_FOR_OCCURRENCE, false)
    val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
    val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val app = appContext as RemindlyApplication
        val prefs = app.container.preferencesRepository.currentPreferences()
        NotificationChannels.ensureChannels(appContext, prefs.alarmBypassDnd)

        val reminder =
          if (!isTest && reminderId > 0L) {
            app.container.reminderRepository.getReminderById(reminderId)
          } else {
            null
          }

        val allowGraceAfterDone =
          !prefs.cancelRemainingOffsetsOnDone &&
            reminder != null &&
            reminder.isActive &&
            !reminder.isDeleted &&
            reminder.lastCompletedOccurrenceEpochMillis == occurrenceMillis

        if (!isTest && !isSnooze) {
          if (reminder == null || !reminder.isActive || reminder.isDeleted) {
            return@launch
          }
          if (reminder.isCompleted && !allowGraceAfterDone) {
            return@launch
          }
        }

        // Check Fallback 8:00 AM condition: skip if any non-fallback offset already fired for this occurrence
        val shouldSkipFallback =
          isFallback &&
            !isTest &&
            reminder != null &&
            reminder.lastFiredOffsetForOccurrenceMillis == occurrenceMillis

        if (!shouldSkipFallback) {
          if (!isFallback && !isTest && !isSnooze && reminder != null) {
            app.container.reminderRepository.markOffsetFiredForOccurrence(
              reminderId = reminder.id,
              occurrenceMillis = occurrenceMillis,
            )
          }

          val title = reminder?.title ?: fallbackTitle
          val note = reminder?.note ?: fallbackNote
          val category =
            reminder?.category
              ?: runCatching { ReminderCategory.valueOf(categoryName) }
                .getOrDefault(ReminderCategory.OTHER)
          val channel =
            runCatching { DeliveryChannel.valueOf(channelName) }
              .getOrDefault(DeliveryChannel.NOTIFICATION)

          if (!isTest) {
            app.container.reminderRepository.recordHistory(
              ReminderHistoryEntry(
                reminderId = reminderId,
                reminderTitle = title,
                category = category,
                eventType =
                  if (channel == DeliveryChannel.ALARM) HistoryEventType.FIRED_ALARM
                  else HistoryEventType.FIRED_NOTIFICATION,
                deliveryChannel = channel,
                offsetLabel =
                  when {
                    isSnooze -> "Snoozed alert"
                    offsetLabel.isNotBlank() -> offsetLabel
                    else -> ""
                  },
                occurrenceEpochMillis = occurrenceMillis,
                eventEpochMillis = System.currentTimeMillis(),
              )
            )
          }

          if (channel == DeliveryChannel.ALARM) {
            val serviceIntent =
              Intent(appContext, AlarmRingingService::class.java).apply {
                action = AlarmRingingService.ACTION_START
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_CATEGORY, category.name)
                putExtra(EXTRA_OCCURRENCE_MILLIS, occurrenceMillis)
                putExtra(EXTRA_OFFSET_LABEL, offsetLabel)
                putExtra(EXTRA_IS_TEST, isTest)
              }
            ContextCompat.startForegroundService(appContext, serviceIntent)
          } else {
            postReminderNotification(
              context = appContext,
              reminderId = reminderId,
              title = title,
              note = note,
              category = category,
              occurrenceMillis = occurrenceMillis,
              offsetLabel = offsetLabel,
              snoozeMinutes = prefs.snoozeDurationMinutes,
              themeAccentArgb = com.example.ui.theme.accentColorArgbFor(prefs.themeMode),
              isTest = isTest,
            )
          }
          app.container.onWidgetRefreshRequested()
        }

        // Arm next cycle once the final scheduled offset for this occurrence has passed
        if (isFinalForOccurrence && !isTest && !isSnooze && reminder != null) {
          app.container.scheduler.scheduleNextCycleAfterFire(
            reminder = reminder,
            firedOccurrenceMillis = occurrenceMillis,
            defaultHour = prefs.defaultHour,
            defaultMinute = prefs.defaultMinute,
          )
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun postReminderNotification(
    context: Context,
    reminderId: Long,
    title: String,
    note: String,
    category: ReminderCategory,
    occurrenceMillis: Long,
    offsetLabel: String,
    snoozeMinutes: Int,
    themeAccentArgb: Int,
    isTest: Boolean,
  ) {
    if (!PermissionAndOemHelper.hasNotificationPermission(context)) return

    val notificationId = AlarmScheduler.notificationIdFor(reminderId, isTest)
    val openAppIntent =
      PendingIntent.getActivity(
        context,
        notificationId,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val doneIntent =
      PendingIntent.getBroadcast(
        context,
        notificationId * 10 + 1,
        Intent(context, NotificationActionReceiver::class.java).apply {
          action = NotificationActionReceiver.ACTION_DONE
          putExtra(EXTRA_REMINDER_ID, reminderId)
          putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
          putExtra(EXTRA_IS_TEST, isTest)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val snoozeIntent =
      PendingIntent.getBroadcast(
        context,
        notificationId * 10 + 2,
        Intent(context, NotificationActionReceiver::class.java).apply {
          action = NotificationActionReceiver.ACTION_SNOOZE
          putExtra(EXTRA_REMINDER_ID, reminderId)
          putExtra(EXTRA_TITLE, title)
          putExtra(EXTRA_CATEGORY, category.name)
          putExtra(EXTRA_CHANNEL, DeliveryChannel.NOTIFICATION.name)
          putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
          putExtra(EXTRA_IS_TEST, isTest)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val timeLabel = IstTime.fromEpochMillis(occurrenceMillis).format12Hour()
    val categoryLabel =
      when (category) {
        ReminderCategory.BIRTHDAY -> "Birthday"
        ReminderCategory.ANNIVERSARY -> "Anniversary"
        ReminderCategory.BILL -> "Bill"
        ReminderCategory.MEDICINE -> "Medicine"
        ReminderCategory.OTHER -> "Reminder"
      }
    val prefix =
      if (offsetLabel.isNotBlank()) "$categoryLabel • $offsetLabel • $timeLabel IST"
      else "$categoryLabel • $timeLabel IST"
    val contentText = if (note.isNotBlank()) "$prefix • $note" else prefix

    val notification =
      NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_stat_remindly)
        .setColor(themeAccentArgb)
        .setContentTitle(title)
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(openAppIntent)
        .addAction(0, "Done", doneIntent)
        .addAction(0, "Snooze ${snoozeMinutes}m", snoozeIntent)
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
  }

  private fun handleWeeklyBackup(appContext: Context) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val app = appContext as RemindlyApplication
        val prefs = app.container.preferencesRepository.currentPreferences()
        NotificationChannels.ensureChannels(appContext, prefs.alarmBypassDnd)

        if (!prefs.weeklyBackupReminderEnabled) {
          app.container.scheduler.scheduleWeeklyBackupReminder(enabled = false)
          return@launch
        }

        val now = System.currentTimeMillis()
        val remindersCount = app.container.reminderRepository.getAllNonDeletedRemindersOnce().size
        val lastBackup = prefs.lastBackupEpochMillis
        val needsBackup =
          remindersCount > 0 &&
            (lastBackup == null || (now - lastBackup) >= 6L * 86_400_000L)

        if (needsBackup && PermissionAndOemHelper.hasNotificationPermission(appContext)) {
          val openBackupIntent =
            PendingIntent.getActivity(
              appContext,
              WEEKLY_BACKUP_NOTIFICATION_ID,
              Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_BACKUP, true)
              },
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

          val notification =
            NotificationCompat.Builder(appContext, NotificationChannels.CHANNEL_BACKUP)
              .setSmallIcon(R.drawable.ic_stat_remindly)
              .setColor(com.example.ui.theme.accentColorArgbFor(prefs.themeMode))
              .setContentTitle("Back up your Remindly reminders")
              .setContentText("Save an offline .remind copy of your $remindersCount reminders.")
              .setAutoCancel(true)
              .setContentIntent(openBackupIntent)
              .build()

          runCatching {
            NotificationManagerCompat.from(appContext).notify(WEEKLY_BACKUP_NOTIFICATION_ID, notification)
          }
        }

        app.container.scheduler.scheduleWeeklyBackupReminder(
          enabled = true,
          nowMillis = now + 120_000L,
        )
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_TRIGGER = "com.example.alarm.ACTION_TRIGGER_REMINDER"
    const val ACTION_WEEKLY_BACKUP = "com.example.alarm.ACTION_WEEKLY_BACKUP"
    const val EXTRA_OPEN_BACKUP = "extra_open_backup"
    private const val WEEKLY_BACKUP_NOTIFICATION_ID = 77_050
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_NOTE = "extra_note"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_CHANNEL = "extra_channel"
    const val EXTRA_OCCURRENCE_MILLIS = "extra_occurrence_millis"
    const val EXTRA_OFFSET_LABEL = "extra_offset_label"
    const val EXTRA_IS_FALLBACK = "extra_is_fallback"
    const val EXTRA_IS_FINAL_FOR_OCCURRENCE = "extra_is_final_for_occurrence"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val EXTRA_IS_TEST = "extra_is_test"
  }
}
