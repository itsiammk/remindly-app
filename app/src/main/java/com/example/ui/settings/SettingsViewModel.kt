package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.NotificationChannels
import com.example.alarm.ReminderScheduler
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.domain.model.AccentPalette
import com.example.domain.model.AlarmSoundOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.UserPreferences
import com.example.domain.model.VibrationPatternOption
import com.example.domain.time.IstTime
import com.example.security.AppLockManager
import com.example.widget.RemindlyWidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val appContext: Context,
  private val preferencesRepository: PreferencesRepository,
  private val reminderRepository: ReminderRepository,
  private val scheduler: ReminderScheduler,
) : ViewModel() {

  val preferences: StateFlow<UserPreferences> =
    preferencesRepository.userPreferences.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferences(),
    )

  val deletedRemindersCount: StateFlow<Int> =
    reminderRepository.deletedReminders
      .map { it.size }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
      )

  val historyCount: StateFlow<Int> =
    reminderRepository.historyEntries
      .map { it.size }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
      )

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setAccentPalette(palette: AccentPalette) {
    viewModelScope.launch { preferencesRepository.setAccentPalette(palette) }
  }

  fun setLowPowerVisuals(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setLowPowerVisuals(enabled) }
  }

  fun setDefaultTime(time: IstTime) {
    viewModelScope.launch {
      preferencesRepository.setDefaultTime(time.hour, time.minute)
      reminderRepository.recomputeDateOnlyOccurrences(time.hour, time.minute)
    }
  }

  fun setAlarmSound(sound: AlarmSoundOption) {
    viewModelScope.launch { preferencesRepository.setAlarmSound(sound) }
  }

  fun setVibrationPattern(pattern: VibrationPatternOption) {
    viewModelScope.launch { preferencesRepository.setVibrationPattern(pattern) }
  }

  fun setSnoozeDurationMinutes(minutes: Int) {
    viewModelScope.launch { preferencesRepository.setSnoozeDurationMinutes(minutes) }
  }

  fun setAlarmBypassDnd(bypass: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setAlarmBypassDnd(bypass)
      NotificationChannels.ensureChannels(appContext, bypass)
    }
  }

  fun setDefaultDeliveryChannel(channel: DeliveryChannel) {
    viewModelScope.launch { preferencesRepository.setDefaultDeliveryChannel(channel) }
  }

  fun setDefaultOffset1DayBefore(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDefaultOffset1DayBefore(enabled) }
  }

  fun setDefaultOffset4HoursBefore(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDefaultOffset4HoursBefore(enabled) }
  }

  fun setDefaultOffset5MinutesBefore(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDefaultOffset5MinutesBefore(enabled) }
  }

  fun setDefaultOffsetFallback8Am(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDefaultOffsetFallback8Am(enabled) }
  }

  fun setDefaultAtTimeForTimed(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDefaultAtTimeForTimed(enabled) }
  }

  fun setCancelRemainingOffsetsOnDone(cancel: Boolean) {
    viewModelScope.launch { preferencesRepository.setCancelRemainingOffsetsOnDone(cancel) }
  }

  fun setAppLanguage(language: AppLanguage) {
    viewModelScope.launch {
      preferencesRepository.setAppLanguage(language)
      RemindlyWidgetUpdater.requestUpdate(appContext)
    }
  }

  fun configurePinLock(
    pin: String,
    useBiometric: Boolean,
    timeoutSeconds: Int,
  ) {
    viewModelScope.launch {
      val hash = AppLockManager.hashPin(pin)
      preferencesRepository.configureAppLock(
        enabled = true,
        pinHash = hash,
        useBiometric = useBiometric,
        timeoutSeconds = timeoutSeconds,
      )
    }
  }

  fun setAppLockUseBiometric(useBiometric: Boolean) {
    viewModelScope.launch { preferencesRepository.setAppLockUseBiometric(useBiometric) }
  }

  fun setAppLockTimeoutSeconds(seconds: Int) {
    viewModelScope.launch { preferencesRepository.setAppLockTimeoutSeconds(seconds) }
  }

  fun disableAppLock() {
    viewModelScope.launch { preferencesRepository.disableAppLock() }
  }

  fun scheduleTestInOneMinute(channel: DeliveryChannel): Long =
    scheduler.scheduleTestInOneMinute(channel)

  companion object {
    fun factory(
      context: Context,
      preferencesRepository: PreferencesRepository,
      reminderRepository: ReminderRepository,
      scheduler: ReminderScheduler,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          SettingsViewModel(
            appContext = context.applicationContext,
            preferencesRepository = preferencesRepository,
            reminderRepository = reminderRepository,
            scheduler = scheduler,
          ) as T
      }
  }
}
