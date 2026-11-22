package com.example.domain.model

import java.util.UUID

enum class ReminderCategory {
  BIRTHDAY,
  ANNIVERSARY,
  BILL,
  MEDICINE,
  OTHER,
}

enum class RepeatRuleType {
  NONE,
  DAILY,
  WEEKLY,
  MONTHLY,
  YEARLY,
  EVERY_N_DAYS,
}

enum class OffsetType {
  MINUTES_BEFORE,
  HOURS_BEFORE,
  DAYS_BEFORE,
  EXACT_DATE_TIME,
  FALLBACK_8AM,
}

enum class DeliveryChannel {
  NOTIFICATION,
  ALARM,
}

enum class Weekday(val bit: Int, val calendarDayOfWeek: Int, val shortLabel: String) {
  MON(1 shl 0, java.util.Calendar.MONDAY, "Mon"),
  TUE(1 shl 1, java.util.Calendar.TUESDAY, "Tue"),
  WED(1 shl 2, java.util.Calendar.WEDNESDAY, "Wed"),
  THU(1 shl 3, java.util.Calendar.THURSDAY, "Thu"),
  FRI(1 shl 4, java.util.Calendar.FRIDAY, "Fri"),
  SAT(1 shl 5, java.util.Calendar.SATURDAY, "Sat"),
  SUN(1 shl 6, java.util.Calendar.SUNDAY, "Sun");

  companion object {
    fun fromCalendarDayOfWeek(dayOfWeek: Int): Weekday =
      entries.firstOrNull { it.calendarDayOfWeek == dayOfWeek } ?: MON

    fun maskOf(days: Collection<Weekday>): Int = days.fold(0) { acc, d -> acc or d.bit }

    fun fromMask(mask: Int): Set<Weekday> = entries.filter { (mask and it.bit) != 0 }.toSet()
  }
}

enum class ReminderBucket {
  MISSED,
  TODAY,
  TOMORROW,
  THIS_WEEK,
  LATER,
  PAUSED,
}

enum class ReminderSortOption(val label: String) {
  SOONEST("Soonest first"),
  TITLE_ASC("Title A–Z"),
  CATEGORY("Category"),
}

enum class AppThemeMode(val label: String, val subtitle: String, val isLightDefault: Boolean = false) {
  SYSTEM("Follow system", "Monochrome Light by day, Aurora by night", false),
  MONOCHROME_LIGHT("Monochrome Light", "Frosted white glass & crisp black type", true),
  MONOCHROME_BLACK("Monochrome Dark", "True AMOLED black with smoky glass", false),
  AURORA("Aurora", "Deep navy with violet & teal light", false),
  SUNSET("Sunset", "Warm dusk with coral & amber glow", false),
  FOREST("Forest", "Deep green with emerald & sage orbs", false),
  OCEAN("Ocean", "Midnight blue with cyan & indigo light", false),
  ROSE("Rose", "Soft blush canvas with rose & peach glass", true);

  companion object {
    fun fromPersisted(raw: String?): AppThemeMode =
      when (raw) {
        null -> SYSTEM
        "LIGHT" -> MONOCHROME_LIGHT
        "DARK" -> AURORA
        else -> runCatching { valueOf(raw) }.getOrDefault(SYSTEM)
      }
  }
}

enum class AccentPalette(val label: String) {
  SAGE("Calm Sage"),
  OCEAN("Serene Ocean"),
  TERRACOTTA("Warm Clay"),
  LAVENDER("Soft Dusk"),
}

enum class AlarmSoundOption(val label: String, val subtitle: String) {
  MORNING_CHIME("Morning Chime", "Warm layered harmonic bell"),
  CRISP_BELL("Crisp Bell", "Clear high-clarity chime"),
  CLASSIC_TONE("Classic Pulse", "Steady traditional alarm beep"),
  URGENT_RING("Urgent Ring", "Strong attention-grabbing cadence"),
  SYSTEM_ALARM("Phone Default", "Uses your device's alarm ringtone"),
}

enum class VibrationPatternOption(val label: String, val pattern: LongArray) {
  STANDARD("Standard", longArrayOf(0, 500, 500)),
  GENTLE("Gentle", longArrayOf(0, 250, 750)),
  HEARTBEAT("Heartbeat", longArrayOf(0, 200, 150, 200, 700)),
  URGENT("Urgent", longArrayOf(0, 350, 200, 350, 200)),
  OFF("Off", longArrayOf()),
}

enum class AppLanguage(val tag: String, val nativeLabel: String, val englishLabel: String) {
  ENGLISH("en", "English", "English"),
  HINDI("hi", "हिन्दी", "Hindi"),
}

enum class HistoryEventType {
  FIRED_NOTIFICATION,
  FIRED_ALARM,
  COMPLETED,
  SNOOZED,
  SKIPPED,
  MOVED,
  MISSED,
}

data class ReminderHistoryEntry(
  val id: Long = 0,
  val reminderId: Long,
  val reminderTitle: String,
  val category: ReminderCategory,
  val eventType: HistoryEventType,
  val deliveryChannel: DeliveryChannel,
  val offsetLabel: String = "",
  val occurrenceEpochMillis: Long,
  val eventEpochMillis: Long,
)


data class ReminderOffset(
  val id: Long = 0,
  val reminderId: Long = 0,
  val offsetType: OffsetType,
  val offsetValue: Int = 0,
  val exactEpochMillis: Long? = null,
  val deliveryChannel: DeliveryChannel = DeliveryChannel.NOTIFICATION,
  val isEnabled: Boolean = true,
) {
  fun isPresetAtTime(): Boolean = offsetType == OffsetType.MINUTES_BEFORE && offsetValue == 0

  fun isPreset1DayBefore(): Boolean = offsetType == OffsetType.DAYS_BEFORE && offsetValue == 1

  fun isPreset4HoursBefore(): Boolean = offsetType == OffsetType.HOURS_BEFORE && offsetValue == 4

  fun isPreset5MinutesBefore(): Boolean = offsetType == OffsetType.MINUTES_BEFORE && offsetValue == 5

  fun isFallback8Am(): Boolean = offsetType == OffsetType.FALLBACK_8AM

  fun isCustomOffset(): Boolean =
    !isPresetAtTime() &&
      !isPreset1DayBefore() &&
      !isPreset4HoursBefore() &&
      !isPreset5MinutesBefore() &&
      !isFallback8Am()

  companion object {
    fun defaultOffsets(
      channel: DeliveryChannel = DeliveryChannel.NOTIFICATION,
      includeAtTime: Boolean = false,
      include1DayBefore: Boolean = true,
      include4HoursBefore: Boolean = true,
      include5MinutesBefore: Boolean = true,
      includeFallback8Am: Boolean = true,
    ): List<ReminderOffset> =
      buildList {
        if (includeAtTime) {
          add(
            ReminderOffset(
              offsetType = OffsetType.MINUTES_BEFORE,
              offsetValue = 0,
              deliveryChannel = channel,
              isEnabled = true,
            )
          )
        }
        add(
          ReminderOffset(
            offsetType = OffsetType.DAYS_BEFORE,
            offsetValue = 1,
            deliveryChannel = channel,
            isEnabled = include1DayBefore,
          )
        )
        add(
          ReminderOffset(
            offsetType = OffsetType.HOURS_BEFORE,
            offsetValue = 4,
            deliveryChannel = channel,
            isEnabled = include4HoursBefore,
          )
        )
        add(
          ReminderOffset(
            offsetType = OffsetType.MINUTES_BEFORE,
            offsetValue = 5,
            deliveryChannel = channel,
            isEnabled = include5MinutesBefore,
          )
        )
        add(
          ReminderOffset(
            offsetType = OffsetType.FALLBACK_8AM,
            offsetValue = 0,
            deliveryChannel = channel,
            isEnabled = includeFallback8Am,
          )
        )
      }
  }
}

data class Reminder(
  val id: Long = 0,
  val uuid: String = UUID.randomUUID().toString(),
  val title: String,
  val note: String = "",
  val category: ReminderCategory = ReminderCategory.OTHER,
  val anchorYear: Int,
  val anchorMonth: Int, // 1..12
  val anchorDay: Int, // 1..31
  val yearKnown: Boolean = true,
  val hasExplicitTime: Boolean = false,
  val hour: Int = 0, // 0..23
  val minute: Int = 0, // 0..59
  val repeatRule: RepeatRuleType = RepeatRuleType.NONE,
  val repeatInterval: Int = 1,
  val repeatDaysOfWeekMask: Int = 0,
  val isActive: Boolean = true,
  val isCompleted: Boolean = false,
  val nextOccurrenceEpochMillis: Long = 0L,
  val lastCompletedOccurrenceEpochMillis: Long? = null,
  val activeOverrideOriginalEpochMillis: Long? = null,
  val lastFiredOffsetForOccurrenceMillis: Long? = null,
  val isDeleted: Boolean = false,
  val deletedAtMillis: Long? = null,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val offsets: List<ReminderOffset> = emptyList(),
) {
  fun primaryDeliveryChannel(): DeliveryChannel {
    val enabled = offsets.filter { it.isEnabled }
    if (enabled.any { it.deliveryChannel == DeliveryChannel.ALARM }) return DeliveryChannel.ALARM
    return enabled.firstOrNull()?.deliveryChannel ?: DeliveryChannel.NOTIFICATION
  }
}

data class UserPreferences(
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val accentPalette: AccentPalette = AccentPalette.SAGE,
  val lowPowerVisuals: Boolean = false,
  val defaultHour: Int = 0, // 12:00 AM IST
  val defaultMinute: Int = 0,
  val alarmSound: AlarmSoundOption = AlarmSoundOption.MORNING_CHIME,
  val vibrationPattern: VibrationPatternOption = VibrationPatternOption.STANDARD,
  val snoozeDurationMinutes: Int = 10,
  val alarmBypassDnd: Boolean = true,
  val defaultDeliveryChannel: DeliveryChannel = DeliveryChannel.NOTIFICATION,
  val hasCompletedOnboarding: Boolean = false,
  val defaultInclude1DayBefore: Boolean = true,
  val defaultInclude4HoursBefore: Boolean = true,
  val defaultInclude5MinutesBefore: Boolean = true,
  val defaultIncludeFallback8Am: Boolean = true,
  val defaultIncludeAtTimeForTimed: Boolean = true,
  val cancelRemainingOffsetsOnDone: Boolean = true,
  val weeklyBackupReminderEnabled: Boolean = true,
  val lastBackupEpochMillis: Long? = null,
  val appLanguage: AppLanguage = AppLanguage.ENGLISH,
  val appLockEnabled: Boolean = false,
  val appLockPinHash: String = "",
  val appLockUseBiometric: Boolean = true,
  val appLockTimeoutSeconds: Int = 0,
)
