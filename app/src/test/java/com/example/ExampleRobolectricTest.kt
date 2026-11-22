package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.local.RemindlyDatabase
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ImportDuplicatePolicy
import com.example.data.repository.ReminderRepository
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.HistoryEventType
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderHistoryEntry
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import com.example.domain.time.IstDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.robolectric.Shadows
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: RemindlyDatabase
  private lateinit var repository: ReminderRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, RemindlyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ReminderRepository(db.reminderDao(), PreferencesRepository(context))
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches Remindly`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Remindly", appName)
  }

  @Test
  fun `repository crud pause markDone and softDeleteUndo flow`() = runTest {
    val now = IstDate(2026, 11, 17).toEpochMillis(10, 0)
    val reminderId =
      repository.saveReminder(
        Reminder(
          title = "Electricity Bill",
          note = "BESCOM",
          category = ReminderCategory.BILL,
          anchorYear = 2026,
          anchorMonth = 11,
          anchorDay = 20,
          hasExplicitTime = true,
          hour = 18,
          minute = 0,
          repeatRule = RepeatRuleType.MONTHLY,
        ),
        nowMillis = now,
      )

    val saved = repository.getReminderById(reminderId)
    assertNotNull(saved)
    assertEquals("Electricity Bill", saved!!.title)
    assertEquals(5, saved.offsets.size)

    // Pause and verify active flag
    repository.setReminderActive(reminderId, isActive = false, nowMillis = now + 1000)
    assertFalse(repository.getReminderById(reminderId)!!.isActive)

    // Resume and mark done -> advances to Dec 20
    repository.setReminderActive(reminderId, isActive = true, nowMillis = now + 2000)
    repository.markReminderDone(reminderId, nowMillis = now + 3000)
    val advanced = repository.getReminderById(reminderId)!!
    assertEquals(IstDate(2026, 12, 20), IstDate.fromEpochMillis(advanced.nextOccurrenceEpochMillis))

    // Soft delete removes from active flow, restore brings it back
    repository.softDeleteReminder(reminderId, nowMillis = now + 4000)
    assertTrue(repository.activeReminders.first().isEmpty())

    repository.restoreDeletedReminder(reminderId, nowMillis = now + 5000)
    assertEquals(1, repository.activeReminders.first().size)
  }

  @Test
  fun `skipCurrentOccurrence and moveCurrentOccurrence preserve monthly series anchor`() = runTest {
    val now = IstDate(2026, 11, 10).toEpochMillis(9, 0)
    val reminderId =
      repository.saveReminder(
        Reminder(
          title = "Rent",
          category = ReminderCategory.BILL,
          anchorYear = 2026,
          anchorMonth = 11,
          anchorDay = 15,
          hasExplicitTime = true,
          hour = 10,
          minute = 0,
          repeatRule = RepeatRuleType.MONTHLY,
        ),
        nowMillis = now,
      )

    // Skip Nov 15 -> jumps straight to Dec 15
    val afterSkip = repository.skipCurrentOccurrence(reminderId, nowMillis = now + 1000)!!
    assertEquals(IstDate(2026, 12, 15), IstDate.fromEpochMillis(afterSkip.nextOccurrenceEpochMillis))

    // Move Dec 15 once to Dec 18 5:00 PM -> series anchor stays Dec 15
    val movedMillis = IstDate(2026, 12, 18).toEpochMillis(17, 0)
    val afterMove = repository.moveCurrentOccurrence(reminderId, movedMillis, nowMillis = now + 2000)!!
    assertEquals(movedMillis, afterMove.nextOccurrenceEpochMillis)
    assertEquals(
      IstDate(2026, 12, 15).toEpochMillis(10, 0),
      afterMove.activeOverrideOriginalEpochMillis,
    )

    // Marking the moved occurrence Done advances from original Dec 15 anchor -> Jan 15
    repository.markReminderDone(reminderId, nowMillis = IstDate(2026, 12, 18).toEpochMillis(18, 0))
    val nextCycle = repository.getReminderById(reminderId)!!
    assertEquals(IstDate(2027, 1, 15), IstDate.fromEpochMillis(nextCycle.nextOccurrenceEpochMillis))
    assertEquals(null, nextCycle.activeOverrideOriginalEpochMillis)
  }

  @Test
  fun `importReminders handles duplicates missed marking and immediate scheduling`() = runTest {
    val scheduler = AlarmScheduler(context)
    val repoWithScheduler =
      ReminderRepository(db.reminderDao(), PreferencesRepository(context), scheduler)
    val now = IstDate(2026, 11, 17).toEpochMillis(14, 0)

    repoWithScheduler.saveReminder(
      Reminder(
        uuid = "existing-bill-1",
        title = "Water Bill",
        note = "Old note",
        category = ReminderCategory.BILL,
        anchorYear = 2026,
        anchorMonth = 11,
        anchorDay = 25,
        hasExplicitTime = true,
        hour = 10,
        minute = 0,
        repeatRule = RepeatRuleType.MONTHLY,
      ),
      nowMillis = now,
    )

    val incoming =
      listOf(
        Reminder(
          uuid = "existing-bill-1",
          title = "Water Bill",
          note = "Updated note",
          category = ReminderCategory.BILL,
          anchorYear = 2026,
          anchorMonth = 11,
          anchorDay = 25,
          hasExplicitTime = true,
          hour = 10,
          minute = 0,
          repeatRule = RepeatRuleType.MONTHLY,
          nextOccurrenceEpochMillis = IstDate(2026, 11, 25).toEpochMillis(10, 0),
          offsets =
            listOf(
              ReminderOffset(
                offsetType = OffsetType.MINUTES_BEFORE,
                offsetValue = 0,
                deliveryChannel = DeliveryChannel.NOTIFICATION,
              )
            ),
        ),
        Reminder(
          uuid = "missed-one-time-2",
          title = "Call Plumber",
          category = ReminderCategory.OTHER,
          anchorYear = 2026,
          anchorMonth = 11,
          anchorDay = 16,
          hasExplicitTime = true,
          hour = 18,
          minute = 0,
          repeatRule = RepeatRuleType.NONE,
          nextOccurrenceEpochMillis = IstDate(2026, 11, 16).toEpochMillis(18, 0),
        ),
      )

    // 1. SKIP policy keeps existing and imports new (which is in the past -> marked missed)
    val skipSummary =
      repoWithScheduler.importReminders(incoming, ImportDuplicatePolicy.SKIP, nowMillis = now)
    assertEquals(1, skipSummary.importedCount)
    assertEquals(1, skipSummary.skippedCount)
    assertEquals(1, skipSummary.missedCount)
    assertEquals(2, repoWithScheduler.getAllNonDeletedRemindersOnce().size)

    // 2. REPLACE policy updates matching duplicate note and schedules immediately
    val replaceSummary =
      repoWithScheduler.importReminders(
        listOf(incoming[0]),
        ImportDuplicatePolicy.REPLACE,
        nowMillis = now,
      )
    assertEquals(1, replaceSummary.replacedCount)
    assertEquals(1, replaceSummary.scheduledCount)
    val updatedExisting =
      repoWithScheduler.getAllNonDeletedRemindersOnce().first { it.uuid == "existing-bill-1" }
    assertEquals("Updated note", updatedExisting.note)

    // 3. KEEP_BOTH creates a third record with a new UUID
    val keepBothSummary =
      repoWithScheduler.importReminders(
        listOf(incoming[0]),
        ImportDuplicatePolicy.KEEP_BOTH,
        nowMillis = now,
      )
    assertEquals(1, keepBothSummary.importedCount)
    assertEquals(3, repoWithScheduler.getAllNonDeletedRemindersOnce().size)

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    assertTrue(Shadows.shadowOf(am).scheduledAlarms.isNotEmpty())
  }

  @Test
  fun `recycle bin recovery permanent delete empty bin and history logging work cleanly`() = runTest {
    val now = IstDate(2026, 11, 17).toEpochMillis(11, 0)
    val id1 =
      repository.saveReminder(
        Reminder(
          title = "Mom's Birthday",
          category = ReminderCategory.BIRTHDAY,
          anchorYear = 1968,
          anchorMonth = 12,
          anchorDay = 5,
          repeatRule = RepeatRuleType.YEARLY,
        ),
        nowMillis = now,
      )
    val id2 =
      repository.saveReminder(
        Reminder(
          title = "Vitamin D",
          category = ReminderCategory.MEDICINE,
          anchorYear = 2026,
          anchorMonth = 11,
          anchorDay = 18,
          hasExplicitTime = true,
          hour = 9,
          minute = 0,
          repeatRule = RepeatRuleType.DAILY,
        ),
        nowMillis = now,
      )

    // Complete id2 -> creates COMPLETED history entry
    repository.markReminderDone(id2, nowMillis = now + 1000)
    val missedEntry =
      ReminderHistoryEntry(
        reminderId = id1,
        reminderTitle = "Mom's Birthday",
        category = ReminderCategory.BIRTHDAY,
        eventType = HistoryEventType.MISSED,
        deliveryChannel = DeliveryChannel.NOTIFICATION,
        occurrenceEpochMillis = IstDate(2026, 12, 5).toEpochMillis(0, 0),
        eventEpochMillis = now + 2000,
      )
    repository.recordHistoryIfNotLogged(missedEntry)
    repository.recordHistoryIfNotLogged(missedEntry) // deduplicated

    val history = repository.historyEntries.first()
    assertEquals(2, history.size)
    assertEquals(1, history.count { it.eventType == HistoryEventType.COMPLETED })
    assertEquals(1, history.count { it.eventType == HistoryEventType.MISSED })

    // Soft-delete both -> both show up in deletedReminders
    repository.softDeleteReminder(id1, nowMillis = now + 3000)
    repository.softDeleteReminder(id2, nowMillis = now + 4000)
    assertEquals(2, repository.deletedReminders.first().size)
    assertTrue(repository.activeReminders.first().isEmpty())

    // Restore id1 -> moves back to active
    repository.restoreDeletedReminder(id1, nowMillis = now + 5000)
    assertEquals(1, repository.activeReminders.first().size)
    assertEquals(1, repository.deletedReminders.first().size)

    // Empty recycle bin removes id2 permanently
    repository.emptyRecycleBin()
    assertTrue(repository.deletedReminders.first().isEmpty())
    assertEquals(1, repository.activeReminders.first().size)
  }

  @Test
  fun `MainActivity handles 32-bit ActivityResult request codes without FragmentActivity 16-bit crash`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    ActivityCompat.requestPermissions(
      activity,
      arrayOf(Manifest.permission.POST_NOTIFICATIONS),
      0x0001_0001,
    )
  }
}
