package com.example.domain.time

import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.Weekday

object RecurrenceCalculator {

  /**
   * Resolves the effective IST time for a reminder (explicit time or user's configured default).
   */
  fun effectiveTime(reminder: Reminder, defaultHour: Int, defaultMinute: Int): IstTime =
    if (reminder.hasExplicitTime) {
      IstTime(reminder.hour, reminder.minute)
    } else {
      IstTime(defaultHour, defaultMinute)
    }

  /**
   * Computes the initial occurrence epoch millis when creating or editing a recurring series.
   * For recurring reminders whose anchor date is strictly before today in IST (such as a birthday
   * in 1995 or an anniversary in 2018), advances to the first occurrence on or after today (IST)
   * while preserving the anchor's day/month/interval rhythm.
   */
  fun computeInitialOccurrenceEpochMillis(
    reminder: Reminder,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val anchorDate = IstDate(reminder.anchorYear, reminder.anchorMonth, reminder.anchorDay)
    val todayIst = IstDate.today(nowMillis)
    if (reminder.repeatRule != RepeatRuleType.NONE && anchorDate < todayIst) {
      val startOfTodayMinusOne = todayIst.toEpochMillis(0, 0) - 1L
      return computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = defaultHour,
        defaultMinute = defaultMinute,
        afterEpochMillis = startOfTodayMinusOne,
      )
    }
    return computeNextOccurrenceEpochMillis(
      reminder = reminder,
      defaultHour = defaultHour,
      defaultMinute = defaultMinute,
      afterEpochMillis = null,
    )
  }

  /**
   * Computes the next occurrence epoch millis in IST.
   *
   * If [afterEpochMillis] is null, computes the active upcoming/current occurrence from the
   * anchor date (or advancing past [reminder.lastCompletedOccurrenceEpochMillis] if set).
   * If [afterEpochMillis] is provided (e.g., when marking an occurrence Done), returns the
   * earliest occurrence strictly after [afterEpochMillis].
   */
  fun computeNextOccurrenceEpochMillis(
    reminder: Reminder,
    defaultHour: Int,
    defaultMinute: Int,
    afterEpochMillis: Long? = reminder.lastCompletedOccurrenceEpochMillis,
  ): Long {
    val anchorDate = IstDate(reminder.anchorYear, reminder.anchorMonth, reminder.anchorDay)
    val time = effectiveTime(reminder, defaultHour, defaultMinute)
    val anchorDateTime = IstDateTime(anchorDate, time)
    val anchorMillis = anchorDateTime.toEpochMillis()

    if (reminder.repeatRule == RepeatRuleType.NONE) {
      return anchorMillis
    }

    val thresholdMillis = afterEpochMillis ?: return anchorMillis
    if (anchorMillis > thresholdMillis) {
      return anchorMillis
    }

    return when (reminder.repeatRule) {
      RepeatRuleType.NONE -> anchorMillis

      RepeatRuleType.DAILY -> {
        val diffDays = anchorDate.daysUntil(IstDate.fromEpochMillis(thresholdMillis)).coerceAtLeast(0)
        var candidate = IstDateTime(anchorDate.plusDays(diffDays), time)
        if (candidate.toEpochMillis() <= thresholdMillis) {
          candidate = IstDateTime(anchorDate.plusDays(diffDays + 1), time)
        }
        candidate.toEpochMillis()
      }

      RepeatRuleType.EVERY_N_DAYS -> {
        val step = reminder.repeatInterval.coerceAtLeast(1)
        val diffDays = anchorDate.daysUntil(IstDate.fromEpochMillis(thresholdMillis)).coerceAtLeast(0)
        var steps = diffDays / step
        var candidate = IstDateTime(anchorDate.plusDays(steps * step), time)
        while (candidate.toEpochMillis() <= thresholdMillis) {
          steps++
          candidate = IstDateTime(anchorDate.plusDays(steps * step), time)
        }
        candidate.toEpochMillis()
      }

      RepeatRuleType.WEEKLY -> {
        val allowedDays =
          Weekday.fromMask(reminder.repeatDaysOfWeekMask).ifEmpty { setOf(anchorDate.weekday()) }
        val thresholdDate = IstDate.fromEpochMillis(thresholdMillis)
        var cursor = if (thresholdDate > anchorDate) thresholdDate else anchorDate
        // Search forward up to 8 days to guarantee matching the next selected weekday
        for (i in 0..8) {
          val candidateDate = cursor.plusDays(i)
          if (candidateDate >= anchorDate && candidateDate.weekday() in allowedDays) {
            val candidateMillis = IstDateTime(candidateDate, time).toEpochMillis()
            if (candidateMillis > thresholdMillis) {
              return candidateMillis
            }
          }
        }
        anchorDate.plusDays(7).toEpochMillis(time.hour, time.minute)
      }

      RepeatRuleType.MONTHLY -> {
        val thresholdDate = IstDate.fromEpochMillis(thresholdMillis)
        var monthsDiff =
          ((thresholdDate.year - anchorDate.year) * 12 + (thresholdDate.month - anchorDate.month))
            .coerceAtLeast(0)
        var candidate = IstDateTime(anchorDate.plusMonthsClamped(monthsDiff), time)
        while (candidate.toEpochMillis() <= thresholdMillis) {
          monthsDiff++
          candidate = IstDateTime(anchorDate.plusMonthsClamped(monthsDiff), time)
        }
        candidate.toEpochMillis()
      }

      RepeatRuleType.YEARLY -> {
        val thresholdDate = IstDate.fromEpochMillis(thresholdMillis)
        var yearsDiff = (thresholdDate.year - anchorDate.year).coerceAtLeast(0)
        var candidate = IstDateTime(anchorDate.plusYearsClamped(yearsDiff), time)
        while (candidate.toEpochMillis() <= thresholdMillis) {
          yearsDiff++
          candidate = IstDateTime(anchorDate.plusYearsClamped(yearsDiff), time)
        }
        candidate.toEpochMillis()
      }
    }
  }

  /**
   * Determines which Home section bucket a reminder belongs to in IST.
   */
  fun classifyBucket(
    reminder: Reminder,
    nowMillis: Long = System.currentTimeMillis(),
  ): ReminderBucket {
    if (!reminder.isActive) return ReminderBucket.PAUSED

    val todayIst = IstDate.today(nowMillis)
    val occurrenceDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
    val dayDelta = todayIst.daysUntil(occurrenceDate)

    // Date-only reminders (e.g., birthdays at default 12:00 AM) stay in Today for the whole day
    val isMissed =
      if (reminder.hasExplicitTime) {
        reminder.nextOccurrenceEpochMillis < nowMillis
      } else {
        dayDelta < 0
      }

    return when {
      isMissed -> ReminderBucket.MISSED
      dayDelta == 0 -> ReminderBucket.TODAY
      dayDelta == 1 -> ReminderBucket.TOMORROW
      dayDelta in 2..7 -> ReminderBucket.THIS_WEEK
      else -> ReminderBucket.LATER
    }
  }

  /**
   * Human-friendly chip text showing how many days remain (or how overdue it is).
   */
  fun daysLeftLabel(
    reminder: Reminder,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val todayIst = IstDate.today(nowMillis)
    val occurrenceDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
    val dayDelta = todayIst.daysUntil(occurrenceDate)

    return when {
      dayDelta < -1 -> "${-dayDelta}d overdue"
      dayDelta == -1 -> "Yesterday"
      dayDelta == 0 -> {
        if (reminder.hasExplicitTime && reminder.nextOccurrenceEpochMillis < nowMillis) {
          "Earlier today"
        } else {
          "Today"
        }
      }
      dayDelta == 1 -> "Tomorrow"
      dayDelta in 2..30 -> "In $dayDelta days"
      else -> {
        val months = dayDelta / 30
        if (months == 1) "In ~1 mo" else "In ~$months mos"
      }
    }
  }

  /**
   * Computes age (Birthday) or years completed (Anniversary) on the upcoming occurrence
   * when [Reminder.yearKnown] is true and anchor year is strictly before occurrence year.
   */
  fun milestoneLabel(reminder: Reminder): String? {
    if (!reminder.yearKnown) return null
    if (reminder.category != ReminderCategory.BIRTHDAY &&
      reminder.category != ReminderCategory.ANNIVERSARY
    ) {
      return null
    }
    val occurrenceYear = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis).year
    val years = occurrenceYear - reminder.anchorYear
    if (years <= 0) return null

    return when (reminder.category) {
      ReminderCategory.BIRTHDAY -> "Turns $years"
      ReminderCategory.ANNIVERSARY -> "${ordinal(years)} anniversary"
      else -> null
    }
  }

  fun repeatSummary(reminder: Reminder): String =
    when (reminder.repeatRule) {
      RepeatRuleType.NONE -> "One-time"
      RepeatRuleType.DAILY -> "Daily"
      RepeatRuleType.WEEKLY -> {
        val days = Weekday.fromMask(reminder.repeatDaysOfWeekMask)
        if (days.isEmpty()) {
          "Weekly"
        } else if (days.size == 7) {
          "Every day"
        } else {
          days.joinToString(", ") { it.shortLabel }
        }
      }
      RepeatRuleType.MONTHLY -> "Monthly"
      RepeatRuleType.YEARLY -> "Yearly"
      RepeatRuleType.EVERY_N_DAYS ->
        if (reminder.repeatInterval <= 1) "Daily" else "Every ${reminder.repeatInterval} days"
    }

  /**
   * Compact tabular countdown label (e.g., "in 2h 14m", "in 45m", "in 3d", or "2h overdue").
   */
  fun countdownLabel(
    targetEpochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val diffMillis = targetEpochMillis - nowMillis
    if (diffMillis < -60_000L) {
      val overdueMins = (-diffMillis) / 60_000L
      val hours = overdueMins / 60L
      val days = hours / 24L
      return when {
        days >= 1L -> "${days}d overdue"
        hours >= 1L -> "${hours}h ${overdueMins % 60L}m overdue"
        else -> "${overdueMins}m overdue"
      }
    }
    if (diffMillis <= 60_000L) return "now"
    val totalMins = diffMillis / 60_000L
    val days = totalMins / (60L * 24L)
    val hours = (totalMins % (60L * 24L)) / 60L
    val mins = totalMins % 60L
    return when {
      days >= 2L -> "in ${days}d"
      days == 1L -> if (hours > 0L) "in 1d ${hours}h" else "in 1d"
      hours >= 1L -> if (mins > 0L) "in ${hours}h ${mins}m" else "in ${hours}h"
      else -> "in ${mins}m"
    }
  }

  /**
   * Maps each IST day-of-month in [year]-[month] to active reminders occurring on that day
   * for the Calendar screen.
   */
  fun remindersByDayInMonth(
    reminders: List<Reminder>,
    year: Int,
    month: Int,
    defaultHour: Int,
    defaultMinute: Int,
  ): Map<Int, List<Reminder>> {
    val daysInMonth = IstDate.lastDayOfMonth(year, month)
    val monthStartMillis = IstDate(year, month, 1).toEpochMillis(0, 0)
    val monthEndMillis = IstDate(year, month, daysInMonth).toEpochMillis(23, 59) + 59_999L
    val map = mutableMapOf<Int, MutableList<Reminder>>()

    for (reminder in reminders) {
      if (reminder.isDeleted || reminder.isCompleted) continue
      val nextDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
      if (nextDate.year == year && nextDate.month == month) {
        map.getOrPut(nextDate.day) { mutableListOf() }.add(reminder)
      }

      if (reminder.repeatRule == RepeatRuleType.NONE) continue

      var cursorMillis =
        computeNextOccurrenceEpochMillis(
          reminder = reminder,
          defaultHour = defaultHour,
          defaultMinute = defaultMinute,
          afterEpochMillis = monthStartMillis - 1L,
        )
      var guard = 0
      while (cursorMillis <= monthEndMillis && guard < 40) {
        guard++
        val d = IstDate.fromEpochMillis(cursorMillis)
        if (d.year == year && d.month == month) {
          val list = map.getOrPut(d.day) { mutableListOf() }
          if (list.none { it.id == reminder.id }) {
            list.add(reminder)
          }
        }
        cursorMillis =
          computeNextOccurrenceEpochMillis(
            reminder = reminder,
            defaultHour = defaultHour,
            defaultMinute = defaultMinute,
            afterEpochMillis = cursorMillis,
          )
      }
    }

    return map.mapValues { (_, list) ->
      list.sortedWith(compareBy<Reminder> { it.hour * 60 + it.minute }.thenBy { it.title.lowercase() })
    }
  }

  private fun ordinal(n: Int): String {
    val mod100 = n % 100
    val suffix =
      if (mod100 in 11..13) {
        "th"
      } else {
        when (n % 10) {
          1 -> "st"
          2 -> "nd"
          3 -> "rd"
          else -> "th"
        }
      }
    return "$n$suffix"
  }
}
