package com.example

import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.ReminderOffset
import com.example.domain.time.IstDate
import com.example.domain.time.OffsetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffsetCalculatorTest {

  @Test
  fun `planFutureOffsets skips past offsets and marks fallback conditional when earlier offset exists`() {
    val occurrence = IstDate(2026, 11, 20).toEpochMillis(18, 0)
    val now = IstDate(2026, 11, 18).toEpochMillis(9, 0)

    val offsets =
      ReminderOffset.defaultOffsets(
        channel = DeliveryChannel.NOTIFICATION,
        includeAtTime = true,
      )
    val planned = OffsetCalculator.planFutureOffsets(offsets, occurrence, now)

    // Expect: 1 day before (Nov 19 6 PM), 8:00 AM fallback (Nov 20 8 AM),
    // 4h before (Nov 20 2 PM), 5m before (Nov 20 5:55 PM), At reminder time (Nov 20 6 PM)
    assertEquals(5, planned.size)
    val fallback = planned.first { it.isFallback }
    assertTrue(fallback.isConditionalFallback)
    assertEquals(IstDate(2026, 11, 20).toEpochMillis(8, 0), fallback.fireAtMillis)
  }

  @Test
  fun `planFutureOffsets activates fallback when all earlier offsets are already in the past`() {
    // Birthday at 12:00 AM IST on Nov 20, created at 1:00 AM on Nov 20 (or event at 10 AM, created at 7 AM)
    val occurrence = IstDate(2026, 11, 20).toEpochMillis(10, 0)
    val now = IstDate(2026, 11, 20).toEpochMillis(7, 0) // 1 day before & 4 hours before (6 AM) already passed!

    val offsets =
      listOf(
        ReminderOffset(offsetType = OffsetType.DAYS_BEFORE, offsetValue = 1),
        ReminderOffset(offsetType = OffsetType.HOURS_BEFORE, offsetValue = 4),
        ReminderOffset(offsetType = OffsetType.FALLBACK_8AM, deliveryChannel = DeliveryChannel.ALARM),
        ReminderOffset(offsetType = OffsetType.MINUTES_BEFORE, offsetValue = 5),
      )

    val planned = OffsetCalculator.planFutureOffsets(offsets, occurrence, now)
    assertEquals(2, planned.size) // Only 8:00 AM fallback and 5m before remain in future
    val fallback = planned.first { it.isFallback }
    assertFalse(fallback.isConditionalFallback) // Nothing earlier before 8 AM fired/scheduled
    assertEquals(DeliveryChannel.ALARM, fallback.offset.deliveryChannel)
    assertTrue(planned.all { it.fireAtMillis > now })
  }

  @Test
  fun `custom relative and exact offsets compute accurate IST fire timestamps and labels`() {
    val occurrence = IstDate(2026, 11, 20).toEpochMillis(15, 30)
    val customExactMillis = IstDate(2026, 11, 19).toEpochMillis(21, 15)
    val now = IstDate(2026, 11, 15).toEpochMillis(10, 0)

    val offsets =
      listOf(
        ReminderOffset(
          offsetType = OffsetType.DAYS_BEFORE,
          offsetValue = 3,
          deliveryChannel = DeliveryChannel.ALARM,
        ),
        ReminderOffset(
          offsetType = OffsetType.EXACT_DATE_TIME,
          exactEpochMillis = customExactMillis,
          deliveryChannel = DeliveryChannel.NOTIFICATION,
        ),
      )

    val planned = OffsetCalculator.planFutureOffsets(offsets, occurrence, now)
    assertEquals(2, planned.size)
    assertEquals(IstDate(2026, 11, 17).toEpochMillis(15, 30), planned[0].fireAtMillis)
    assertEquals("3 days before", planned[0].label)
    assertEquals(customExactMillis, planned[1].fireAtMillis)
    assertTrue(OffsetCalculator.formatFirePreview(planned[0]).contains("IST (Alarm)"))
  }
}
