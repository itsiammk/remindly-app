package com.example.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RingingAlarmState(
  val reminderId: Long,
  val title: String,
  val note: String,
  val category: ReminderCategory,
  val occurrenceMillis: Long,
  val offsetLabel: String,
  val snoozeMinutes: Int,
  val isTest: Boolean,
)

class AlarmRingingService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private lateinit var soundPlayer: AlarmSoundPlayer

  override fun onCreate() {
    super.onCreate()
    soundPlayer = AlarmSoundPlayer(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_DONE -> {
        val reminderId = intent.getLongExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, -1L)
        val isTest = intent.getBooleanExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, false)
        soundPlayer.stop()
        _ringingState.value = null
        scope.launch(Dispatchers.IO) {
          if (!isTest && reminderId > 0L) {
            (application as RemindlyApplication).container.reminderRepository.markReminderDone(
              reminderId
            )
          }
          stopSelf()
        }
        return START_NOT_STICKY
      }

      ACTION_SNOOZE -> {
        val current = _ringingState.value
        val reminderId =
          intent.getLongExtra(
            ReminderTriggerReceiver.EXTRA_REMINDER_ID,
            current?.reminderId ?: -1L,
          )
        val title =
          intent.getStringExtra(ReminderTriggerReceiver.EXTRA_TITLE)
            ?: current?.title
            ?: "Reminder"
        val categoryName =
          intent.getStringExtra(ReminderTriggerReceiver.EXTRA_CATEGORY)
            ?: current?.category?.name
            ?: ReminderCategory.OTHER.name
        val category =
          runCatching { ReminderCategory.valueOf(categoryName) }
            .getOrDefault(ReminderCategory.OTHER)

        soundPlayer.stop()
        _ringingState.value = null
        scope.launch(Dispatchers.IO) {
          val app = application as RemindlyApplication
          val prefs = app.container.preferencesRepository.currentPreferences()
          val isTest =
            intent.getBooleanExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, current?.isTest ?: false)
          app.container.scheduler.scheduleSnooze(
            reminderId = if (reminderId > 0L) reminderId else 999_999L,
            title = title,
            category = category,
            deliveryChannel = DeliveryChannel.ALARM,
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
                deliveryChannel = DeliveryChannel.ALARM,
                offsetLabel = "Snoozed ${prefs.snoozeDurationMinutes} min",
                occurrenceEpochMillis = current?.occurrenceMillis ?: now,
                eventEpochMillis = now,
              )
            )
          }
          stopSelf()
        }
        return START_NOT_STICKY
      }

      ACTION_START, null -> {
        val reminderId = intent?.getLongExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(ReminderTriggerReceiver.EXTRA_TITLE) ?: "Reminder"
        val note = intent?.getStringExtra(ReminderTriggerReceiver.EXTRA_NOTE).orEmpty()
        val categoryName =
          intent?.getStringExtra(ReminderTriggerReceiver.EXTRA_CATEGORY)
            ?: ReminderCategory.OTHER.name
        val category =
          runCatching { ReminderCategory.valueOf(categoryName) }
            .getOrDefault(ReminderCategory.OTHER)
        val occurrenceMillis =
          intent?.getLongExtra(
            ReminderTriggerReceiver.EXTRA_OCCURRENCE_MILLIS,
            System.currentTimeMillis(),
          ) ?: System.currentTimeMillis()
        val offsetLabel = intent?.getStringExtra(ReminderTriggerReceiver.EXTRA_OFFSET_LABEL).orEmpty()
        val isTest = intent?.getBooleanExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, false) ?: false

        // Must enter foreground immediately before async preferences load
        val placeholderNotification =
          buildAlarmNotification(
            reminderId = reminderId,
            title = title,
            note = note,
            category = category,
            occurrenceMillis = occurrenceMillis,
            offsetLabel = offsetLabel,
            snoozeMinutes = 10,
            isTest = isTest,
          )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(
            NOTIFICATION_ID,
            placeholderNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
          )
        } else {
          startForeground(NOTIFICATION_ID, placeholderNotification)
        }

        scope.launch {
          val app = application as RemindlyApplication
          val prefs = app.container.preferencesRepository.currentPreferences()
          NotificationChannels.ensureChannels(this@AlarmRingingService, prefs.alarmBypassDnd)

          _ringingState.value =
            RingingAlarmState(
              reminderId = reminderId,
              title = title,
              note = note,
              category = category,
              occurrenceMillis = occurrenceMillis,
              offsetLabel = offsetLabel,
              snoozeMinutes = prefs.snoozeDurationMinutes,
              isTest = isTest,
            )

          // Update ongoing notification with configured snooze duration and theme accent
          val updatedNotification =
            buildAlarmNotification(
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
          val nm = getSystemService(android.app.NotificationManager::class.java)
          nm?.notify(NOTIFICATION_ID, updatedNotification)

          soundPlayer.start(prefs.alarmSound, prefs.vibrationPattern)

          // Launch full-screen activity directly as well for unlocked devices
          val activityIntent =
            Intent(this@AlarmRingingService, AlarmActivity::class.java).apply {
              addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                  Intent.FLAG_ACTIVITY_SINGLE_TOP or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP
              )
            }
          runCatching { startActivity(activityIntent) }
        }
      }
    }

    return START_STICKY
  }

  private fun buildAlarmNotification(
    reminderId: Long,
    title: String,
    note: String,
    category: ReminderCategory,
    occurrenceMillis: Long,
    offsetLabel: String,
    snoozeMinutes: Int,
    themeAccentArgb: Int? = null,
    isTest: Boolean,
  ): android.app.Notification {
    val fullScreenIntent =
      PendingIntent.getActivity(
        this,
        NOTIFICATION_ID,
        Intent(this, AlarmActivity::class.java).apply {
          flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
              Intent.FLAG_ACTIVITY_SINGLE_TOP or
              Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val doneIntent =
      PendingIntent.getService(
        this,
        NOTIFICATION_ID + 1,
        Intent(this, AlarmRingingService::class.java).apply {
          action = ACTION_DONE
          putExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, reminderId)
          putExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, isTest)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val snoozeIntent =
      PendingIntent.getService(
        this,
        NOTIFICATION_ID + 2,
        Intent(this, AlarmRingingService::class.java).apply {
          action = ACTION_SNOOZE
          putExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, reminderId)
          putExtra(ReminderTriggerReceiver.EXTRA_TITLE, title)
          putExtra(ReminderTriggerReceiver.EXTRA_CATEGORY, category.name)
          putExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, isTest)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val timeText = IstTime.fromEpochMillis(occurrenceMillis).format12Hour()
    val prefix = if (offsetLabel.isNotBlank()) "$offsetLabel • $timeText IST" else "$timeText IST • Alarm"
    val body = if (note.isNotBlank()) "$prefix • $note" else prefix

    val builder =
      NotificationCompat.Builder(this, NotificationChannels.CHANNEL_ALARMS)
        .setSmallIcon(R.drawable.ic_stat_remindly)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setFullScreenIntent(fullScreenIntent, true)
        .setContentIntent(fullScreenIntent)
        .addAction(0, "Done", doneIntent)
        .addAction(0, "Snooze ${snoozeMinutes}m", snoozeIntent)
    if (themeAccentArgb != null) {
      builder.setColor(themeAccentArgb)
    }
    return builder.build()
  }

  override fun onDestroy() {
    soundPlayer.stop()
    _ringingState.value = null
    scope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val ACTION_START = "com.example.alarm.ACTION_ALARM_START"
    const val ACTION_DONE = "com.example.alarm.ACTION_ALARM_DONE"
    const val ACTION_SNOOZE = "com.example.alarm.ACTION_ALARM_SNOOZE"
    private const val NOTIFICATION_ID = 95_001

    private val _ringingState = MutableStateFlow<RingingAlarmState?>(null)
    val ringingState: StateFlow<RingingAlarmState?> = _ringingState.asStateFlow()
  }
}
