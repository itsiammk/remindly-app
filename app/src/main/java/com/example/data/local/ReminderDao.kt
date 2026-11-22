package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.domain.model.HistoryEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

  @Transaction
  @Query(
    """
    SELECT * FROM reminders
    WHERE isDeleted = 0 AND isCompleted = 0
    ORDER BY nextOccurrenceEpochMillis ASC, title ASC
    """
  )
  fun observeActiveReminders(): Flow<List<ReminderWithOffsets>>

  @Transaction
  @Query(
    """
    SELECT * FROM reminders
    WHERE isDeleted = 0 AND isCompleted = 0
    """
  )
  suspend fun getAllActiveRemindersOnce(): List<ReminderWithOffsets>

  @Transaction
  @Query(
    """
    SELECT * FROM reminders
    WHERE isDeleted = 0
    ORDER BY nextOccurrenceEpochMillis ASC, title ASC
    """
  )
  suspend fun getAllNonDeletedRemindersOnce(): List<ReminderWithOffsets>

  @Transaction
  @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
  suspend fun getReminderById(id: Long): ReminderWithOffsets?

  @Transaction
  @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
  fun observeReminderById(id: Long): Flow<ReminderWithOffsets?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReminder(reminder: ReminderEntity): Long

  @Update
  suspend fun updateReminder(reminder: ReminderEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOffsets(offsets: List<ReminderOffsetEntity>)

  @Query("DELETE FROM reminder_offsets WHERE reminderId = :reminderId")
  suspend fun deleteOffsetsForReminder(reminderId: Long)

  @Transaction
  suspend fun upsertReminderWithOffsets(
    reminder: ReminderEntity,
    offsets: List<ReminderOffsetEntity>,
  ): Long {
    val savedId =
      if (reminder.id == 0L) {
        insertReminder(reminder)
      } else {
        updateReminder(reminder)
        reminder.id
      }
    deleteOffsetsForReminder(savedId)
    if (offsets.isNotEmpty()) {
      insertOffsets(offsets.map { it.copy(id = 0, reminderId = savedId) })
    }
    return savedId
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOccurrenceOverride(override: ReminderOccurrenceOverrideEntity): Long

  @Query(
    """
    UPDATE reminders
    SET lastFiredOffsetForOccurrenceMillis = :occurrenceMillis
    WHERE id = :reminderId
    """
  )
  suspend fun markOffsetFiredForOccurrence(reminderId: Long, occurrenceMillis: Long)

  @Query(
    """
    UPDATE reminders
    SET isDeleted = 1, deletedAtMillis = :deletedAtMillis, updatedAtMillis = :deletedAtMillis
    WHERE id = :id
    """
  )
  suspend fun softDeleteReminder(id: Long, deletedAtMillis: Long)

  @Query(
    """
    UPDATE reminders
    SET isDeleted = 0, deletedAtMillis = NULL, updatedAtMillis = :updatedAtMillis
    WHERE id = :id
    """
  )
  suspend fun restoreReminder(id: Long, updatedAtMillis: Long)

  @Query(
    """
    UPDATE reminders
    SET isActive = :isActive, updatedAtMillis = :updatedAtMillis
    WHERE id = :id
    """
  )
  suspend fun setReminderActive(id: Long, isActive: Boolean, updatedAtMillis: Long)

  @Query("DELETE FROM reminders WHERE id = :id")
  suspend fun hardDeleteReminder(id: Long)

  @Transaction
  @Query(
    """
    SELECT * FROM reminders
    WHERE isDeleted = 1
    ORDER BY deletedAtMillis DESC, updatedAtMillis DESC
    """
  )
  fun observeDeletedReminders(): Flow<List<ReminderWithOffsets>>

  @Query("DELETE FROM reminders WHERE isDeleted = 1")
  suspend fun hardDeleteAllDeleted()

  @Query(
    """
    SELECT * FROM reminder_history
    ORDER BY eventEpochMillis DESC, id DESC
    LIMIT :limit
    """
  )
  fun observeHistory(limit: Int = 250): Flow<List<ReminderHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(entry: ReminderHistoryEntity): Long

  @Query(
    """
    SELECT COUNT(*) FROM reminder_history
    WHERE reminderId = :reminderId
      AND occurrenceEpochMillis = :occurrenceEpochMillis
      AND eventType = :eventType
    """
  )
  suspend fun countHistoryEvents(
    reminderId: Long,
    occurrenceEpochMillis: Long,
    eventType: HistoryEventType,
  ): Int

  @Query("DELETE FROM reminder_history")
  suspend fun clearHistory()
}
