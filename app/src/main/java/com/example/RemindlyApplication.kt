package com.example

import android.app.Application
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationChannels
import com.example.alarm.ReminderScheduler
import com.example.data.local.RemindlyDatabase
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderHistoryEntry
import com.example.widget.RemindlyWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val application: Application) {
  val database: RemindlyDatabase by lazy { RemindlyDatabase.getInstance(application) }
  val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(application) }
  val scheduler: ReminderScheduler by lazy { AlarmScheduler(application) }
  val onWidgetRefreshRequested: () -> Unit = { RemindlyWidgetUpdater.requestUpdate(application) }
  val reminderRepository: ReminderRepository by lazy {
    ReminderRepository(
      reminderDao = database.reminderDao(),
      preferencesRepository = preferencesRepository,
      scheduler = scheduler,
      onRemindersChanged = onWidgetRefreshRequested,
    )
  }
}

class RemindlyApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)

    appScope.launch {
      val prefs = container.preferencesRepository.currentPreferences()
      NotificationChannels.ensureChannels(this@RemindlyApplication, prefs.alarmBypassDnd)
      val active = container.reminderRepository.getAllActiveRemindersOnce()
      val missed = container.scheduler.rescheduleAll(active, prefs.defaultHour, prefs.defaultMinute)
      val now = System.currentTimeMillis()
      missed.forEach { reminder ->
        container.reminderRepository.recordHistoryIfNotLogged(
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
      container.scheduler.scheduleWeeklyBackupReminder(prefs.weeklyBackupReminderEnabled)
      container.onWidgetRefreshRequested()
    }
  }
}
