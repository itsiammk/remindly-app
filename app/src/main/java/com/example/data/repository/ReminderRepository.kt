package com.example.data.repository

import com.example.alarm.NoOpReminderScheduler
import com.example.alarm.ReminderScheduler
import com.example.data.local.ReminderDao
import com.example.data.local.ReminderOccurrenceOverrideEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.preferences.PreferencesRepository
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.HistoryEventType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderHistoryEntry
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class ImportDuplicatePolicy(val label: String, val subtitle: String) {
  SKIP("Skip existing", "Keep your current reminders untouched"),
  REPLACE("Replace existing", "Overwrite matching reminders with backup data"),
  KEEP_BOTH("Keep both", "Import duplicates as separate reminders"),
}

data class ImportSummary(
  val importedCount: Int,
  val replacedCount: Int,
  val skippedCount: Int,
  val missedCount: Int,
  val scheduledCount: Int,
)

class ReminderRepository(
  private val reminderDao: ReminderDao,
  private val preferencesRepository: PreferencesRepository,
  private val scheduler: ReminderScheduler = NoOpReminderScheduler,
  private val onRemindersChanged: () -> Unit = {},
) {

  val activeReminders: Flow<List<Reminder>> =
    reminderDao.observeActiveReminders().map { list -> list.map { it.toDomain() } }

  val deletedReminders: Flow<List<Reminder>> =
    reminderDao.observeDeletedReminders().map { list -> list.map { it.toDomain() } }

  val historyEntries: Flow<List<ReminderHistoryEntry>> =
    reminderDao.observeHistory().map { list -> list.map { it.toDomain() } }

  fun observeReminder(id: Long): Flow<Reminder?> =
    reminderDao.observeReminderById(id).map { it?.toDomain() }

  suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)?.toDomain()

  suspend fun getAllActiveRemindersOnce(): List<Reminder> =
    reminderDao.getAllActiveRemindersOnce().map { it.toDomain() }

  suspend fun saveReminder(
    reminder: Reminder,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val prefs = preferencesRepository.currentPreferences()
    val nextOccurrenceMillis =
      RecurrenceCalculator.computeInitialOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = prefs.defaultHour,
        defaultMinute = prefs.defaultMinute,
        nowMillis = nowMillis,
      )
    val offsets =
      reminder.offsets.ifEmpty {
        ReminderOffset.defaultOffsets(
          channel = prefs.defaultDeliveryChannel,
          includeAtTime = reminder.hasExplicitTime && prefs.defaultIncludeAtTimeForTimed,
          include1DayBefore = prefs.defaultInclude1DayBefore,
          include4HoursBefore = prefs.defaultInclude4HoursBefore,
          include5MinutesBefore = prefs.defaultInclude5MinutesBefore,
          includeFallback8Am = prefs.defaultIncludeFallback8Am,
        )
      }
    val prepared =
      reminder.copy(
        title = reminder.title.trim(),
        note = reminder.note.trim(),
        isCompleted = false,
        lastCompletedOccurrenceEpochMillis =
          if (reminder.id == 0L) null else reminder.lastCompletedOccurrenceEpochMillis,
        activeOverrideOriginalEpochMillis = null,
        lastFiredOffsetForOccurrenceMillis = null,
        nextOccurrenceEpochMillis = nextOccurrenceMillis,
        createdAtMillis = if (reminder.id == 0L) nowMillis else reminder.createdAtMillis,
        updatedAtMillis = nowMillis,
        offsets = offsets,
      )
    val savedId =
      reminderDao.upsertReminderWithOffsets(
        reminder = prepared.toEntity(),
        offsets = offsets.map { it.toEntity(prepared.id) },
      )
    val saved = reminderDao.getReminderById(savedId)?.toDomain()
    if (saved != null) {
      scheduler.scheduleReminder(saved, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    }
    onRemindersChanged()
    return savedId
  }

  suspend fun markReminderDone(
    reminderId: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val current = reminderDao.getReminderById(reminderId)?.toDomain() ?: return
    val prefs = preferencesRepository.currentPreferences()
    val completedOccurrence = current.nextOccurrenceEpochMillis
    val seriesAnchorMillis = current.activeOverrideOriginalEpochMillis ?: completedOccurrence

    if (current.repeatRule == RepeatRuleType.NONE) {
      reminderDao.updateReminder(
        current
          .copy(
            isCompleted = true,
            lastCompletedOccurrenceEpochMillis = completedOccurrence,
            activeOverrideOriginalEpochMillis = null,
            updatedAtMillis = nowMillis,
          )
          .toEntity()
      )
      scheduler.cancelReminder(reminderId)
      if (!prefs.cancelRemainingOffsetsOnDone) {
        scheduler.scheduleRemainingOffsetsForCompletedOccurrence(
          reminder = current,
          completedOccurrenceMillis = completedOccurrence,
          nowMillis = nowMillis,
        )
      }
    } else {
      val threshold = maxOf(seriesAnchorMillis, nowMillis)
      val nextOccurrence =
        RecurrenceCalculator.computeNextOccurrenceEpochMillis(
          reminder = current,
          defaultHour = prefs.defaultHour,
          defaultMinute = prefs.defaultMinute,
          afterEpochMillis = threshold,
        )
      val updated =
        current.copy(
          lastCompletedOccurrenceEpochMillis = seriesAnchorMillis,
          activeOverrideOriginalEpochMillis = null,
          lastFiredOffsetForOccurrenceMillis = null,
          nextOccurrenceEpochMillis = nextOccurrence,
          updatedAtMillis = nowMillis,
        )
      reminderDao.updateReminder(updated.toEntity())
      scheduler.scheduleReminder(updated, prefs.defaultHour, prefs.defaultMinute, nowMillis)
        if (!prefs.cancelRemainingOffsetsOnDone) {
        scheduler.scheduleRemainingOffsetsForCompletedOccurrence(
          reminder = current,
          completedOccurrenceMillis = completedOccurrence,
          nowMillis = nowMillis,
        )
      }
    }
    recordHistory(
      ReminderHistoryEntry(
        reminderId = current.id,
        reminderTitle = current.title,
        category = current.category,
        eventType = HistoryEventType.COMPLETED,
        deliveryChannel = current.primaryDeliveryChannel(),
        occurrenceEpochMillis = completedOccurrence,
        eventEpochMillis = nowMillis,
      )
    )
    onRemindersChanged()
  }

  /**
   * Skips only the current occurrence of a repeating reminder without changing the series rhythm.
   */
  suspend fun skipCurrentOccurrence(
    reminderId: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ): Reminder? {
    val current = reminderDao.getReminderById(reminderId)?.toDomain() ?: return null
    if (current.repeatRule == RepeatRuleType.NONE) return null

    val prefs = preferencesRepository.currentPreferences()
    val seriesAnchorMillis =
      current.activeOverrideOriginalEpochMillis ?: current.nextOccurrenceEpochMillis

    reminderDao.insertOccurrenceOverride(
      ReminderOccurrenceOverrideEntity(
        reminderId = reminderId,
        originalEpochMillis = seriesAnchorMillis,
        overriddenEpochMillis = null,
        isSkipped = true,
        createdAtMillis = nowMillis,
      )
    )

    val nextOccurrence =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = current,
        defaultHour = prefs.defaultHour,
        defaultMinute = prefs.defaultMinute,
        afterEpochMillis = maxOf(seriesAnchorMillis, current.nextOccurrenceEpochMillis),
      )

    val updated =
      current.copy(
        lastCompletedOccurrenceEpochMillis = seriesAnchorMillis,
        activeOverrideOriginalEpochMillis = null,
        lastFiredOffsetForOccurrenceMillis = null,
        nextOccurrenceEpochMillis = nextOccurrence,
        updatedAtMillis = nowMillis,
      )
    reminderDao.updateReminder(updated.toEntity())
    scheduler.scheduleReminder(updated, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    recordHistory(
      ReminderHistoryEntry(
        reminderId = current.id,
        reminderTitle = current.title,
        category = current.category,
        eventType = HistoryEventType.SKIPPED,
        deliveryChannel = current.primaryDeliveryChannel(),
        occurrenceEpochMillis = seriesAnchorMillis,
        eventEpochMillis = nowMillis,
      )
    )
    onRemindersChanged()
    return updated
  }

  /**
   * Moves only the current occurrence of a repeating reminder to [newOccurrenceMillis]
   * without altering the series anchor or future cycles.
   */
  suspend fun moveCurrentOccurrence(
    reminderId: Long,
    newOccurrenceMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ): Reminder? {
    val current = reminderDao.getReminderById(reminderId)?.toDomain() ?: return null
    if (current.repeatRule == RepeatRuleType.NONE) return null

    val prefs = preferencesRepository.currentPreferences()
    val seriesAnchorMillis =
      current.activeOverrideOriginalEpochMillis ?: current.nextOccurrenceEpochMillis

    reminderDao.insertOccurrenceOverride(
      ReminderOccurrenceOverrideEntity(
        reminderId = reminderId,
        originalEpochMillis = seriesAnchorMillis,
        overriddenEpochMillis = newOccurrenceMillis,
        isSkipped = false,
        createdAtMillis = nowMillis,
      )
    )

    val updated =
      current.copy(
        activeOverrideOriginalEpochMillis = seriesAnchorMillis,
        lastFiredOffsetForOccurrenceMillis = null,
        nextOccurrenceEpochMillis = newOccurrenceMillis,
        updatedAtMillis = nowMillis,
      )
    reminderDao.updateReminder(updated.toEntity())
    scheduler.scheduleReminder(updated, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    val movedToDate = IstDate.fromEpochMillis(newOccurrenceMillis).formatShort()
    val movedToTime = IstTime.fromEpochMillis(newOccurrenceMillis).format12Hour()
    recordHistory(
      ReminderHistoryEntry(
        reminderId = current.id,
        reminderTitle = current.title,
        category = current.category,
        eventType = HistoryEventType.MOVED,
        deliveryChannel = current.primaryDeliveryChannel(),
        offsetLabel = "Moved to $movedToDate • $movedToTime",
        occurrenceEpochMillis = newOccurrenceMillis,
        eventEpochMillis = nowMillis,
      )
    )
    onRemindersChanged()
    return updated
  }

  suspend fun restorePreviousOccurrenceState(
    previousReminder: Reminder,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val prefs = preferencesRepository.currentPreferences()
    val restored = previousReminder.copy(updatedAtMillis = nowMillis)
    reminderDao.updateReminder(restored.toEntity())
    scheduler.scheduleReminder(restored, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    onRemindersChanged()
  }

  suspend fun markOffsetFiredForOccurrence(reminderId: Long, occurrenceMillis: Long) {
    reminderDao.markOffsetFiredForOccurrence(reminderId, occurrenceMillis)
  }

  suspend fun restoreCompletedOneTimeReminder(
    reminderId: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val current = reminderDao.getReminderById(reminderId)?.toDomain() ?: return
    val prefs = preferencesRepository.currentPreferences()
    val restored =
      current.copy(
        isCompleted = false,
        updatedAtMillis = nowMillis,
      )
    reminderDao.updateReminder(restored.toEntity())
    scheduler.scheduleReminder(restored, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    onRemindersChanged()
  }

  suspend fun setReminderActive(
    reminderId: Long,
    isActive: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    reminderDao.setReminderActive(reminderId, isActive, nowMillis)
    val updated = reminderDao.getReminderById(reminderId)?.toDomain()
    if (updated != null && isActive) {
      val prefs = preferencesRepository.currentPreferences()
      scheduler.scheduleReminder(updated, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    } else {
      scheduler.cancelReminder(reminderId)
    }
    onRemindersChanged()
  }

  suspend fun softDeleteReminder(
    reminderId: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    reminderDao.softDeleteReminder(reminderId, nowMillis)
    scheduler.cancelReminder(reminderId)
    onRemindersChanged()
  }

  suspend fun restoreDeletedReminder(
    reminderId: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    reminderDao.restoreReminder(reminderId, nowMillis)
    val restored = reminderDao.getReminderById(reminderId)?.toDomain() ?: return
    val prefs = preferencesRepository.currentPreferences()
    scheduler.scheduleReminder(restored, prefs.defaultHour, prefs.defaultMinute, nowMillis)
    onRemindersChanged()
  }

  suspend fun permanentlyDeleteReminder(reminderId: Long) {
    scheduler.cancelReminder(reminderId)
    reminderDao.hardDeleteReminder(reminderId)
    onRemindersChanged()
  }

  suspend fun emptyRecycleBin() {
    reminderDao.hardDeleteAllDeleted()
    onRemindersChanged()
  }

  suspend fun recordHistory(entry: ReminderHistoryEntry) {
    reminderDao.insertHistory(entry.toEntity())
  }

  suspend fun recordHistoryIfNotLogged(entry: ReminderHistoryEntry) {
    val existing =
      reminderDao.countHistoryEvents(
        reminderId = entry.reminderId,
        occurrenceEpochMillis = entry.occurrenceEpochMillis,
        eventType = entry.eventType,
      )
    if (existing == 0) {
      reminderDao.insertHistory(entry.toEntity())
    }
  }

  suspend fun clearHistory() {
    reminderDao.clearHistory()
  }

  suspend fun getAllNonDeletedRemindersOnce(): List<Reminder> =
    reminderDao.getAllNonDeletedRemindersOnce().map { it.toDomain() }

  fun findMatchingExistingReminder(
    incoming: Reminder,
    existing: List<Reminder>,
  ): Reminder? {
    existing.firstOrNull { it.uuid == incoming.uuid }?.let { return it }
    val normalizedIncomingTitle = incoming.title.trim().lowercase()
    return existing.firstOrNull { cand ->
      cand.title.trim().lowercase() == normalizedIncomingTitle &&
        cand.category == incoming.category &&
        cand.anchorMonth == incoming.anchorMonth &&
        cand.anchorDay == incoming.anchorDay &&
        cand.hasExplicitTime == incoming.hasExplicitTime &&
        (!incoming.hasExplicitTime ||
          (cand.hour == incoming.hour && cand.minute == incoming.minute))
    }
  }

  suspend fun importReminders(
    incomingReminders: List<Reminder>,
    duplicatePolicy: ImportDuplicatePolicy,
    nowMillis: Long = System.currentTimeMillis(),
  ): ImportSummary {
    val prefs = preferencesRepository.currentPreferences()
    val existingList = reminderDao.getAllNonDeletedRemindersOnce().map { it.toDomain() }.toMutableList()

    var importedCount = 0
    var replacedCount = 0
    var skippedCount = 0
    var missedCount = 0
    var scheduledCount = 0

    for (rawIncoming in incomingReminders) {
      val match = findMatchingExistingReminder(rawIncoming, existingList)
      if (match != null && duplicatePolicy == ImportDuplicatePolicy.SKIP) {
        skippedCount++
        continue
      }

      val targetId = if (match != null && duplicatePolicy == ImportDuplicatePolicy.REPLACE) match.id else 0L
      val targetUuid =
        when {
          match != null && duplicatePolicy == ImportDuplicatePolicy.REPLACE -> match.uuid
          match != null && duplicatePolicy == ImportDuplicatePolicy.KEEP_BOTH ->
            UUID.randomUUID().toString()
          else -> rawIncoming.uuid
        }

      val effectiveOffsets =
        rawIncoming.offsets.ifEmpty {
          ReminderOffset.defaultOffsets(
            channel = prefs.defaultDeliveryChannel,
            includeAtTime = rawIncoming.hasExplicitTime && prefs.defaultIncludeAtTimeForTimed,
            include1DayBefore = prefs.defaultInclude1DayBefore,
            include4HoursBefore = prefs.defaultInclude4HoursBefore,
            include5MinutesBefore = prefs.defaultInclude5MinutesBefore,
            includeFallback8Am = prefs.defaultIncludeFallback8Am,
          )
        }

      val computedOccurrence =
        if (rawIncoming.nextOccurrenceEpochMillis > 0L) {
          rawIncoming.nextOccurrenceEpochMillis
        } else {
          RecurrenceCalculator.computeInitialOccurrenceEpochMillis(
            reminder = rawIncoming,
            defaultHour = prefs.defaultHour,
            defaultMinute = prefs.defaultMinute,
            nowMillis = nowMillis,
          )
        }

      val prepared =
        rawIncoming.copy(
          id = targetId,
          uuid = targetUuid,
          title = rawIncoming.title.trim(),
          note = rawIncoming.note.trim(),
          nextOccurrenceEpochMillis = computedOccurrence,
          isDeleted = false,
          deletedAtMillis = null,
          createdAtMillis = if (targetId == 0L) nowMillis else match?.createdAtMillis ?: nowMillis,
          updatedAtMillis = nowMillis,
          offsets = effectiveOffsets,
        )

      val savedId =
        reminderDao.upsertReminderWithOffsets(
          reminder = prepared.toEntity(),
          offsets = effectiveOffsets.map { it.toEntity(targetId) },
        )
      val saved = reminderDao.getReminderById(savedId)?.toDomain()
      if (saved != null) {
        if (targetId == 0L) {
          existingList.add(saved)
          importedCount++
        } else {
          val idx = existingList.indexOfFirst { it.id == saved.id }
          if (idx >= 0) existingList[idx] = saved else existingList.add(saved)
          replacedCount++
        }

        if (saved.isActive && !saved.isCompleted) {
          if (RecurrenceCalculator.classifyBucket(saved, nowMillis) == ReminderBucket.MISSED) {
            missedCount++
          }
          scheduler.scheduleReminder(saved, prefs.defaultHour, prefs.defaultMinute, nowMillis)
          scheduledCount++
        } else {
          scheduler.cancelReminder(saved.id)
        }
      }
    }

    if (importedCount > 0 || replacedCount > 0) {
      onRemindersChanged()
    }

    return ImportSummary(
      importedCount = importedCount,
      replacedCount = replacedCount,
      skippedCount = skippedCount,
      missedCount = missedCount,
      scheduledCount = scheduledCount,
    )
  }

  /**
   * Recomputes nextOccurrenceEpochMillis for date-only reminders when the default time changes.
   */
  suspend fun recomputeDateOnlyOccurrences(
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val reminders = reminderDao.getAllActiveRemindersOnce().map { it.toDomain() }
    reminders
      .filter { !it.hasExplicitTime && it.activeOverrideOriginalEpochMillis == null }
      .forEach { reminder ->
        val scheduledDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
        val updatedNext = scheduledDate.toEpochMillis(defaultHour, defaultMinute)
        if (updatedNext != reminder.nextOccurrenceEpochMillis) {
          val updated =
            reminder.copy(
              nextOccurrenceEpochMillis = updatedNext,
              updatedAtMillis = nowMillis,
            )
          reminderDao.updateReminder(updated.toEntity())
          scheduler.scheduleReminder(updated, defaultHour, defaultMinute, nowMillis)
        }
      }
    onRemindersChanged()
  }
}
