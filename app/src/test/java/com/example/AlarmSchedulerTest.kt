package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.Weekday
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmSchedulerTest {

  private lateinit var context: Context
  private lateinit var alarmManager: AlarmManager
  private lateinit var scheduler: AlarmScheduler

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    scheduler = AlarmScheduler(context)
  }

  @Test
  fun `request codes are unique across reminder ids and slots`() {
    val seen = mutableSetOf<Int>()
    for (id in 1L..50L) {
      for (slot in listOf(0, 1, 2, 62)) {
        val code = AlarmScheduler.requestCodeFor(id, slot)
        assertTrue("Duplicate request code $code", seen.add(code))
      }
    }
    assertNotEquals(
      AlarmScheduler.notificationIdFor(1L),
      AlarmScheduler.notificationIdFor(2L),
    )
  }

  @Test
  fun `scheduleReminder schedules trigger and cancel removes it`() {
    val now = IstDate(2026, 11, 17).toEpochMillis(8, 0)
    val triggerAt = IstDate(2026, 11, 17).toEpochMillis(9, 30)
    val reminder =
      Reminder(
        id = 12L,
        title = "Morning Medicine",
        category = ReminderCategory.MEDICINE,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 17,
        hasExplicitTime = true,
        hour = 9,
        minute = 30,
        repeatRule = RepeatRuleType.DAILY,
        nextOccurrenceEpochMillis = triggerAt,
        offsets =
          listOf(
            ReminderOffset(
              offsetType = OffsetType.MINUTES_BEFORE,
              offsetValue = 0,
              deliveryChannel = DeliveryChannel.ALARM,
            )
          ),
      )

    scheduler.scheduleReminder(reminder, defaultHour = 0, defaultMinute = 0, nowMillis = now)

    val shadowAm = Shadows.shadowOf(alarmManager)
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == triggerAt })

    scheduler.cancelReminder(12L)
    assertTrue(shadowAm.scheduledAlarms.none { it.triggerAtMs == triggerAt })
  }

  @Test
  fun `rescheduleAll detects missed reminders and still schedules next recurring cycle`() {
    val now = IstDate(2026, 11, 17).toEpochMillis(12, 0)
    val missedOccurrence = IstDate(2026, 11, 17).toEpochMillis(9, 0)

    val dailyMissed =
      Reminder(
        id = 5L,
        title = "BP Pill",
        category = ReminderCategory.MEDICINE,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 17,
        hasExplicitTime = true,
        hour = 9,
        minute = 0,
        repeatRule = RepeatRuleType.DAILY,
        nextOccurrenceEpochMillis = missedOccurrence,
        offsets =
          listOf(
            ReminderOffset(
              offsetType = OffsetType.MINUTES_BEFORE,
              offsetValue = 0,
              deliveryChannel = DeliveryChannel.NOTIFICATION,
            )
          ),
      )

    val missed =
      scheduler.rescheduleAll(
        reminders = listOf(dailyMissed),
        defaultHour = 0,
        defaultMinute = 0,
        nowMillis = now,
      )

    assertEquals(1, missed.size)
    assertEquals(5L, missed.first().id)

    val shadowAm = Shadows.shadowOf(alarmManager)
    val expectedTomorrow = IstDate(2026, 11, 18).toEpochMillis(9, 0)
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == expectedTomorrow })
  }

  @Test
  fun `snooze and one minute test schedule exact future triggers`() {
    val now = IstDate(2026, 11, 17).toEpochMillis(10, 0)
    scheduler.scheduleSnooze(
      reminderId = 42L,
      title = "Pay Gas Bill",
      category = ReminderCategory.BILL,
      deliveryChannel = DeliveryChannel.NOTIFICATION,
      snoozeMinutes = 10,
      nowMillis = now,
    )

    val shadowAm = Shadows.shadowOf(alarmManager)
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == now + 10 * 60_000L })

    val testTrigger = scheduler.scheduleTestInOneMinute(DeliveryChannel.ALARM, nowMillis = now)
    assertEquals(now + 60_000L, testTrigger)
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == testTrigger })
  }

  @Test
  fun `scheduleReminder schedules multiple offsets across slots and keeps grace offsets when requested`() {
    val now = IstDate(2026, 11, 17).toEpochMillis(10, 0)
    val eventTime = IstDate(2026, 11, 20).toEpochMillis(18, 0)
    val fire1Day = IstDate(2026, 11, 19).toEpochMillis(18, 0)
    val fire4Hours = IstDate(2026, 11, 20).toEpochMillis(14, 0)
    val fire5Minutes = IstDate(2026, 11, 20).toEpochMillis(17, 55)

    val reminder =
      Reminder(
        id = 77L,
        title = "Tax Filing",
        category = ReminderCategory.BILL,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 20,
        hasExplicitTime = true,
        hour = 18,
        minute = 0,
        repeatRule = RepeatRuleType.NONE,
        nextOccurrenceEpochMillis = eventTime,
        offsets =
          listOf(
            ReminderOffset(
              offsetType = OffsetType.DAYS_BEFORE,
              offsetValue = 1,
              deliveryChannel = DeliveryChannel.NOTIFICATION,
            ),
            ReminderOffset(
              offsetType = OffsetType.HOURS_BEFORE,
              offsetValue = 4,
              deliveryChannel = DeliveryChannel.NOTIFICATION,
            ),
            ReminderOffset(
              offsetType = OffsetType.MINUTES_BEFORE,
              offsetValue = 5,
              deliveryChannel = DeliveryChannel.ALARM,
            ),
          ),
      )

    scheduler.scheduleReminder(reminder, defaultHour = 0, defaultMinute = 0, nowMillis = now)
    val shadowAm = Shadows.shadowOf(alarmManager)
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == fire1Day })
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == fire4Hours })
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == fire5Minutes })

    // Simulate marking Done early on Nov 19 20:00 while cancelRemainingOffsetsOnDone == false
    val doneAt = IstDate(2026, 11, 19).toEpochMillis(20, 0)
    scheduler.cancelReminder(77L)
    scheduler.scheduleRemainingOffsetsForCompletedOccurrence(
      reminder = reminder,
      completedOccurrenceMillis = eventTime,
      nowMillis = doneAt,
    )
    assertTrue(shadowAm.scheduledAlarms.none { it.triggerAtMs == fire1Day })
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == fire4Hours })
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == fire5Minutes })
  }

  @Test
  fun `scheduleWeeklyBackupReminder targets next Sunday 10 AM IST and cancels when disabled`() {
    // Tuesday 17 Nov 2026 9:00 AM -> Next Sunday is 22 Nov 2026 10:00 AM IST
    val now = IstDate(2026, 11, 17).toEpochMillis(9, 0)
    val expectedTriggerMs = IstDate(2026, 11, 22).toEpochMillis(10, 0)
    assertEquals(Weekday.SUN, IstDate.fromEpochMillis(expectedTriggerMs).weekday())
    assertEquals(IstTime(10, 0), IstTime.fromEpochMillis(expectedTriggerMs))

    scheduler.scheduleWeeklyBackupReminder(enabled = true, nowMillis = now)

    val shadowAm = Shadows.shadowOf(alarmManager)
    assertTrue(shadowAm.scheduledAlarms.any { it.triggerAtMs == expectedTriggerMs })

    scheduler.scheduleWeeklyBackupReminder(enabled = false, nowMillis = now)
    assertTrue(shadowAm.scheduledAlarms.none { it.triggerAtMs == expectedTriggerMs })
  }
}
