package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccentPalette
import com.example.domain.model.AlarmSoundOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.UserPreferences
import com.example.domain.model.VibrationPatternOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.remindlyDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "remindly_preferences")

class PreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    val LOW_POWER_VISUALS = booleanPreferencesKey("low_power_visuals")
    val DEFAULT_HOUR = intPreferencesKey("default_hour")
    val DEFAULT_MINUTE = intPreferencesKey("default_minute")
    val ALARM_SOUND = stringPreferencesKey("alarm_sound")
    val VIBRATION_PATTERN = stringPreferencesKey("vibration_pattern")
    val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
    val ALARM_BYPASS_DND = booleanPreferencesKey("alarm_bypass_dnd")
    val DEFAULT_DELIVERY_CHANNEL = stringPreferencesKey("default_delivery_channel")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val DEFAULT_1_DAY_BEFORE = booleanPreferencesKey("default_1_day_before")
    val DEFAULT_4_HOURS_BEFORE = booleanPreferencesKey("default_4_hours_before")
    val DEFAULT_5_MINUTES_BEFORE = booleanPreferencesKey("default_5_minutes_before")
    val DEFAULT_FALLBACK_8AM = booleanPreferencesKey("default_fallback_8am")
    val DEFAULT_AT_TIME_FOR_TIMED = booleanPreferencesKey("default_at_time_for_timed")
    val CANCEL_OFFSETS_ON_DONE = booleanPreferencesKey("cancel_offsets_on_done")
    val WEEKLY_BACKUP_ENABLED = booleanPreferencesKey("weekly_backup_enabled")
    val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
    val APP_LOCK_USE_BIOMETRIC = booleanPreferencesKey("app_lock_use_biometric")
    val APP_LOCK_TIMEOUT_SECONDS = intPreferencesKey("app_lock_timeout_seconds")
  }

  val userPreferences: Flow<UserPreferences> =
    context.remindlyDataStore.data.map { prefs ->
      val themeMode = AppThemeMode.fromPersisted(prefs[Keys.THEME_MODE])
      val accentPalette =
        prefs[Keys.ACCENT_PALETTE]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() }
          ?: AccentPalette.SAGE
      val lowPowerVisuals = prefs[Keys.LOW_POWER_VISUALS] ?: false
      val defaultHour = (prefs[Keys.DEFAULT_HOUR] ?: 0).coerceIn(0, 23)
      val defaultMinute = (prefs[Keys.DEFAULT_MINUTE] ?: 0).coerceIn(0, 59)
      val alarmSound =
        prefs[Keys.ALARM_SOUND]?.let { runCatching { AlarmSoundOption.valueOf(it) }.getOrNull() }
          ?: AlarmSoundOption.MORNING_CHIME
      val vibrationPattern =
        prefs[Keys.VIBRATION_PATTERN]?.let {
          runCatching { VibrationPatternOption.valueOf(it) }.getOrNull()
        } ?: VibrationPatternOption.STANDARD
      val snoozeMinutes = (prefs[Keys.SNOOZE_MINUTES] ?: 10).coerceIn(1, 60)
      val bypassDnd = prefs[Keys.ALARM_BYPASS_DND] ?: true
      val defaultChannel =
        prefs[Keys.DEFAULT_DELIVERY_CHANNEL]?.let {
          runCatching { DeliveryChannel.valueOf(it) }.getOrNull()
        } ?: DeliveryChannel.NOTIFICATION
      val onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false

      UserPreferences(
        themeMode = themeMode,
        accentPalette = accentPalette,
        lowPowerVisuals = lowPowerVisuals,
        defaultHour = defaultHour,
        defaultMinute = defaultMinute,
        alarmSound = alarmSound,
        vibrationPattern = vibrationPattern,
        snoozeDurationMinutes = snoozeMinutes,
        alarmBypassDnd = bypassDnd,
        defaultDeliveryChannel = defaultChannel,
        hasCompletedOnboarding = onboardingCompleted,
        defaultInclude1DayBefore = prefs[Keys.DEFAULT_1_DAY_BEFORE] ?: true,
        defaultInclude4HoursBefore = prefs[Keys.DEFAULT_4_HOURS_BEFORE] ?: true,
        defaultInclude5MinutesBefore = prefs[Keys.DEFAULT_5_MINUTES_BEFORE] ?: true,
        defaultIncludeFallback8Am = prefs[Keys.DEFAULT_FALLBACK_8AM] ?: true,
        defaultIncludeAtTimeForTimed = prefs[Keys.DEFAULT_AT_TIME_FOR_TIMED] ?: true,
        cancelRemainingOffsetsOnDone = prefs[Keys.CANCEL_OFFSETS_ON_DONE] ?: true,
        weeklyBackupReminderEnabled = prefs[Keys.WEEKLY_BACKUP_ENABLED] ?: true,
        lastBackupEpochMillis = prefs[Keys.LAST_BACKUP_MILLIS],
        appLanguage =
          prefs[Keys.APP_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.ENGLISH,
        appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
        appLockPinHash = prefs[Keys.APP_LOCK_PIN_HASH].orEmpty(),
        appLockUseBiometric = prefs[Keys.APP_LOCK_USE_BIOMETRIC] ?: true,
        appLockTimeoutSeconds = prefs[Keys.APP_LOCK_TIMEOUT_SECONDS] ?: 0,
      )
    }

  suspend fun currentPreferences(): UserPreferences = userPreferences.first()

  suspend fun setThemeMode(mode: AppThemeMode) {
    context.remindlyDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setAccentPalette(palette: AccentPalette) {
    context.remindlyDataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
  }

  suspend fun setLowPowerVisuals(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.LOW_POWER_VISUALS] = enabled }
  }

  suspend fun setDefaultTime(hour: Int, minute: Int) {
    context.remindlyDataStore.edit {
      it[Keys.DEFAULT_HOUR] = hour.coerceIn(0, 23)
      it[Keys.DEFAULT_MINUTE] = minute.coerceIn(0, 59)
    }
  }

  suspend fun setAlarmSound(sound: AlarmSoundOption) {
    context.remindlyDataStore.edit { it[Keys.ALARM_SOUND] = sound.name }
  }

  suspend fun setVibrationPattern(pattern: VibrationPatternOption) {
    context.remindlyDataStore.edit { it[Keys.VIBRATION_PATTERN] = pattern.name }
  }

  suspend fun setSnoozeDurationMinutes(minutes: Int) {
    context.remindlyDataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes.coerceIn(1, 60) }
  }

  suspend fun setAlarmBypassDnd(bypass: Boolean) {
    context.remindlyDataStore.edit { it[Keys.ALARM_BYPASS_DND] = bypass }
  }

  suspend fun setDefaultDeliveryChannel(channel: DeliveryChannel) {
    context.remindlyDataStore.edit { it[Keys.DEFAULT_DELIVERY_CHANNEL] = channel.name }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.remindlyDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setDefaultOffset1DayBefore(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.DEFAULT_1_DAY_BEFORE] = enabled }
  }

  suspend fun setDefaultOffset4HoursBefore(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.DEFAULT_4_HOURS_BEFORE] = enabled }
  }

  suspend fun setDefaultOffset5MinutesBefore(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.DEFAULT_5_MINUTES_BEFORE] = enabled }
  }

  suspend fun setDefaultOffsetFallback8Am(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.DEFAULT_FALLBACK_8AM] = enabled }
  }

  suspend fun setDefaultAtTimeForTimed(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.DEFAULT_AT_TIME_FOR_TIMED] = enabled }
  }

  suspend fun setCancelRemainingOffsetsOnDone(cancel: Boolean) {
    context.remindlyDataStore.edit { it[Keys.CANCEL_OFFSETS_ON_DONE] = cancel }
  }

  suspend fun setWeeklyBackupReminderEnabled(enabled: Boolean) {
    context.remindlyDataStore.edit { it[Keys.WEEKLY_BACKUP_ENABLED] = enabled }
  }

  suspend fun setLastBackupEpochMillis(timestampMillis: Long) {
    context.remindlyDataStore.edit { it[Keys.LAST_BACKUP_MILLIS] = timestampMillis }
  }

  suspend fun setAppLanguage(language: AppLanguage) {
    context.remindlyDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
  }

  suspend fun configureAppLock(
    enabled: Boolean,
    pinHash: String,
    useBiometric: Boolean,
    timeoutSeconds: Int,
  ) {
    context.remindlyDataStore.edit {
      it[Keys.APP_LOCK_ENABLED] = enabled
      it[Keys.APP_LOCK_PIN_HASH] = pinHash
      it[Keys.APP_LOCK_USE_BIOMETRIC] = useBiometric
      it[Keys.APP_LOCK_TIMEOUT_SECONDS] = timeoutSeconds.coerceAtLeast(0)
    }
  }

  suspend fun setAppLockUseBiometric(useBiometric: Boolean) {
    context.remindlyDataStore.edit { it[Keys.APP_LOCK_USE_BIOMETRIC] = useBiometric }
  }

  suspend fun setAppLockTimeoutSeconds(seconds: Int) {
    context.remindlyDataStore.edit { it[Keys.APP_LOCK_TIMEOUT_SECONDS] = seconds.coerceAtLeast(0) }
  }

  suspend fun disableAppLock() {
    context.remindlyDataStore.edit {
      it[Keys.APP_LOCK_ENABLED] = false
      it[Keys.APP_LOCK_PIN_HASH] = ""
    }
  }
}
