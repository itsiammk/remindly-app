package com.example

import com.example.backup.RemindBackupCodec
import com.example.backup.RemindParseResult
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import com.example.domain.time.IstDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RemindBackupCodecTest {

  private fun sampleReminders(): List<Reminder> =
    listOf(
      Reminder(
        uuid = "uuid-bday-1",
        title = "Aarav's Birthday",
        note = "Buy Lego set",
        category = ReminderCategory.BIRTHDAY,
        anchorYear = 2016,
        anchorMonth = 11,
        anchorDay = 25,
        yearKnown = true,
        hasExplicitTime = false,
        repeatRule = RepeatRuleType.YEARLY,
        nextOccurrenceEpochMillis = IstDate(2026, 11, 25).toEpochMillis(0, 0),
        offsets =
          listOf(
            ReminderOffset(
              offsetType = OffsetType.DAYS_BEFORE,
              offsetValue = 1,
              deliveryChannel = DeliveryChannel.ALARM,
            ),
            ReminderOffset(
              offsetType = OffsetType.FALLBACK_8AM,
              deliveryChannel = DeliveryChannel.NOTIFICATION,
            ),
          ),
      ),
      Reminder(
        uuid = "uuid-med-2",
        title = "Night Tablet",
        category = ReminderCategory.MEDICINE,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 17,
        hasExplicitTime = true,
        hour = 21,
        minute = 30,
        repeatRule = RepeatRuleType.DAILY,
        nextOccurrenceEpochMillis = IstDate(2026, 11, 17).toEpochMillis(21, 30),
        offsets =
          listOf(
            ReminderOffset(
              offsetType = OffsetType.MINUTES_BEFORE,
              offsetValue = 0,
              deliveryChannel = DeliveryChannel.ALARM,
            )
          ),
      ),
    )

  @Test
  fun `plaintext encode and parse round trip preserves reminders and offsets`() {
    val input = sampleReminders()
    val encoded = RemindBackupCodec.encode(input, password = null, exportedAtMillis = 1_700_000_000_000L)
    val parsed = RemindBackupCodec.parse(encoded)

    assertTrue(parsed is RemindParseResult.Success)
    val success = parsed as RemindParseResult.Success
    assertFalse(success.wasEncrypted)
    assertEquals(2, success.payload.reminders.size)
    assertEquals("Aarav's Birthday", success.payload.reminders[0].title)
    assertEquals("Buy Lego set", success.payload.reminders[0].note)
    assertEquals(2, success.payload.reminders[0].offsets.size)
    assertEquals(DeliveryChannel.ALARM, success.payload.reminders[0].offsets[0].deliveryChannel)
    assertEquals(21, success.payload.reminders[1].hour)
    assertEquals(30, success.payload.reminders[1].minute)
  }

  @Test
  fun `password protected encode requires password rejects wrong password and decrypts with right one`() {
    val input = sampleReminders()
    val encoded = RemindBackupCodec.encode(input, password = "secret-password-99")
    val initial = RemindBackupCodec.parse(encoded)

    assertTrue(initial is RemindParseResult.PasswordRequired)
    val envelope = (initial as RemindParseResult.PasswordRequired).envelope

    val wrong = RemindBackupCodec.decrypt(envelope, "wrong-pass")
    assertEquals(RemindParseResult.WrongPassword, wrong)

    val unlocked = RemindBackupCodec.decrypt(envelope, "secret-password-99")
    assertTrue(unlocked is RemindParseResult.Success)
    val success = unlocked as RemindParseResult.Success
    assertTrue(success.wasEncrypted)
    assertEquals(2, success.payload.reminders.size)
    assertEquals("uuid-bday-1", success.payload.reminders[0].uuid)
  }

  @Test
  fun `invalid or unsupported backup files return InvalidFile`() {
    assertTrue(RemindBackupCodec.parse("") is RemindParseResult.InvalidFile)
    assertTrue(RemindBackupCodec.parse("not json") is RemindParseResult.InvalidFile)
    assertTrue(
      RemindBackupCodec.parse("""{"format":"other.app","version":1}""") is RemindParseResult.InvalidFile
    )
    assertTrue(
      RemindBackupCodec.parse("""{"format":"com.remindly.backup","version":99}""")
        is RemindParseResult.InvalidFile
    )
  }
}
