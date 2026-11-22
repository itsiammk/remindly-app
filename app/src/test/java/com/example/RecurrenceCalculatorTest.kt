package com.example

import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.Weekday
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceCalculatorTest {

  @Test
  fun `ist date and 12h time format matches specification`() {
    val date = IstDate(2026, 11, 17)
    assertEquals("17 Nov 2026", date.formatShort())

    assertEquals("12:00 AM", IstTime(0, 0).format12Hour())
    assertEquals("9:05 AM", IstTime(9, 5).format12Hour())
    assertEquals("12:00 PM", IstTime(12, 0).format12Hour())
    assertEquals("8:30 PM", IstTime(20, 30).format12Hour())
  }

  @Test
  fun `monthly reminder on 31st clamps to month end and returns to 31st`() {
    val jan31 = IstDate(2026, 1, 31)
    val reminder =
      Reminder(
        title = "Rent",
        category = ReminderCategory.BILL,
        anchorYear = 2026,
        anchorMonth = 1,
        anchorDay = 31,
        hasExplicitTime = true,
        hour = 9,
        minute = 0,
        repeatRule = RepeatRuleType.MONTHLY,
      )

    val febMillis =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = jan31.toEpochMillis(9, 0),
      )
    assertEquals(IstDate(2026, 2, 28), IstDate.fromEpochMillis(febMillis))

    val marMillis =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = febMillis,
      )
    assertEquals(IstDate(2026, 3, 31), IstDate.fromEpochMillis(marMillis))

    val aprMillis =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = marMillis,
      )
    assertEquals(IstDate(2026, 4, 30), IstDate.fromEpochMillis(aprMillis))
  }

  @Test
  fun `yearly reminder on 29 Feb clamps to 28 Feb in non leap years and restores on leap year`() {
    val reminder =
      Reminder(
        title = "Leap Birthday",
        category = ReminderCategory.BIRTHDAY,
        anchorYear = 2024,
        anchorMonth = 2,
        anchorDay = 29,
        repeatRule = RepeatRuleType.YEARLY,
      )

    val y2025 =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = IstDate(2024, 2, 29).toEpochMillis(0, 0),
      )
    assertEquals(IstDate(2025, 2, 28), IstDate.fromEpochMillis(y2025))

    val y2028 =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = IstDate(2027, 2, 28).toEpochMillis(0, 0),
      )
    assertEquals(IstDate(2028, 2, 29), IstDate.fromEpochMillis(y2028))
  }

  @Test
  fun `historical birthday schedules upcoming occurrence and computes age`() {
    val now = IstDate(2026, 11, 17).toEpochMillis(10, 0)
    val reminder =
      Reminder(
        title = "Riya's Birthday",
        category = ReminderCategory.BIRTHDAY,
        anchorYear = 1998,
        anchorMonth = 11,
        anchorDay = 20,
        yearKnown = true,
        hasExplicitTime = false,
        repeatRule = RepeatRuleType.YEARLY,
      )

    val nextMillis =
      RecurrenceCalculator.computeInitialOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        nowMillis = now,
      )
    assertEquals(IstDate(2026, 11, 20), IstDate.fromEpochMillis(nextMillis))

    val scheduled = reminder.copy(nextOccurrenceEpochMillis = nextMillis)
    assertEquals("Turns 28", RecurrenceCalculator.milestoneLabel(scheduled))
    assertEquals(ReminderBucket.THIS_WEEK, RecurrenceCalculator.classifyBucket(scheduled, now))
    assertEquals("In 3 days", RecurrenceCalculator.daysLeftLabel(scheduled, now))

    val unknownYear = scheduled.copy(yearKnown = false)
    assertNull(RecurrenceCalculator.milestoneLabel(unknownYear))
  }

  @Test
  fun `weekly chosen weekdays picks next matching day`() {
    // 17 Nov 2026 is Tuesday
    val tue = IstDate(2026, 11, 17)
    val reminder =
      Reminder(
        title = "Gym",
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 17,
        hasExplicitTime = true,
        hour = 7,
        minute = 0,
        repeatRule = RepeatRuleType.WEEKLY,
        repeatDaysOfWeekMask = Weekday.maskOf(listOf(Weekday.MON, Weekday.THU)),
      )

    val nextMillis =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = tue.toEpochMillis(7, 0),
      )
    assertEquals(IstDate(2026, 11, 19), IstDate.fromEpochMillis(nextMillis))
    assertEquals(Weekday.THU, IstDate.fromEpochMillis(nextMillis).weekday())
  }

  @Test
  fun `every N days advances by exact interval`() {
    val start = IstDate(2026, 11, 1)
    val reminder =
      Reminder(
        title = "Water plants",
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 1,
        hasExplicitTime = true,
        hour = 8,
        minute = 0,
        repeatRule = RepeatRuleType.EVERY_N_DAYS,
        repeatInterval = 3,
      )

    val nextMillis =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = 0,
        defaultMinute = 0,
        afterEpochMillis = start.toEpochMillis(8, 0),
      )
    assertEquals(IstDate(2026, 11, 4), IstDate.fromEpochMillis(nextMillis))
  }

  @Test
  fun `date only today stays in TODAY while past explicit time moves to MISSED`() {
    val now = IstDate(2026, 11, 17).toEpochMillis(14, 0) // 2:00 PM IST

    val birthdayToday =
      Reminder(
        title = "Birthday",
        category = ReminderCategory.BIRTHDAY,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 17,
        hasExplicitTime = false,
        nextOccurrenceEpochMillis = IstDate(2026, 11, 17).toEpochMillis(0, 0),
      )
    assertEquals(ReminderBucket.TODAY, RecurrenceCalculator.classifyBucket(birthdayToday, now))

    val morningMedicine =
      Reminder(
        title = "Vitamin D",
        category = ReminderCategory.MEDICINE,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 17,
        hasExplicitTime = true,
        hour = 9,
        minute = 0,
        nextOccurrenceEpochMillis = IstDate(2026, 11, 17).toEpochMillis(9, 0),
      )
    assertEquals(ReminderBucket.MISSED, RecurrenceCalculator.classifyBucket(morningMedicine, now))
  }

  @Test
  fun `countdownLabel formats upcoming and overdue durations cleanly`() {
    val base = IstDate(2026, 11, 17).toEpochMillis(10, 0)
    assertEquals("now", RecurrenceCalculator.countdownLabel(base + 30_000L, base))
    assertEquals("in 45m", RecurrenceCalculator.countdownLabel(base + 45 * 60_000L, base))
    assertEquals(
      "in 2h 15m",
      RecurrenceCalculator.countdownLabel(base + (2 * 60 + 15) * 60_000L, base),
    )
    assertEquals(
      "in 1d 3h",
      RecurrenceCalculator.countdownLabel(base + (27 * 60) * 60_000L, base),
    )
    assertEquals(
      "in 4d",
      RecurrenceCalculator.countdownLabel(base + (4 * 24 * 60) * 60_000L, base),
    )
    assertEquals(
      "2h 10m overdue",
      RecurrenceCalculator.countdownLabel(base - (2 * 60 + 10) * 60_000L, base),
    )
  }

  @Test
  fun `remindersByDayInMonth maps recurring and one-time reminders cleanly across IST month`() {
    val febStart = IstDate(2026, 2, 1)
    val monthlyOn31 =
      Reminder(
        id = 1L,
        title = "Rent",
        category = ReminderCategory.BILL,
        anchorYear = 2026,
        anchorMonth = 1,
        anchorDay = 31,
        hasExplicitTime = true,
        hour = 9,
        minute = 0,
        repeatRule = RepeatRuleType.MONTHLY,
        nextOccurrenceEpochMillis = IstDate(2026, 2, 28).toEpochMillis(9, 0),
      )
    val weeklyMon =
      Reminder(
        id = 2L,
        title = "Trash",
        anchorYear = 2026,
        anchorMonth = 2,
        anchorDay = 2, // Monday in Feb 2026
        hasExplicitTime = true,
        hour = 8,
        minute = 0,
        repeatRule = RepeatRuleType.WEEKLY,
        repeatDaysOfWeekMask = Weekday.maskOf(listOf(Weekday.MON)),
        nextOccurrenceEpochMillis = febStart.plusDays(1).toEpochMillis(8, 0),
      )
    val deletedReminder =
      Reminder(
        id = 3L,
        title = "Deleted",
        anchorYear = 2026,
        anchorMonth = 2,
        anchorDay = 14,
        isDeleted = true,
        nextOccurrenceEpochMillis = IstDate(2026, 2, 14).toEpochMillis(10, 0),
      )

    val byDay =
      RecurrenceCalculator.remindersByDayInMonth(
        reminders = listOf(monthlyOn31, weeklyMon, deletedReminder),
        year = 2026,
        month = 2,
        defaultHour = 0,
        defaultMinute = 0,
      )

    assertEquals(listOf(1L), byDay[28]?.map { it.id })
    assertEquals(listOf(2L), byDay[2]?.map { it.id })
    assertEquals(listOf(2L), byDay[9]?.map { it.id })
    assertEquals(listOf(2L), byDay[16]?.map { it.id })
    assertEquals(listOf(2L), byDay[23]?.map { it.id })
    assertNull(byDay[14])
  }
}
