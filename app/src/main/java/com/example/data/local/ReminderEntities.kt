package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.HistoryEventType
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderHistoryEntry
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType

@Entity(
  tableName = "reminders",
  indices = [
    Index(value = ["uuid"], unique = true),
    Index(value = ["isDeleted", "isCompleted", "nextOccurrenceEpochMillis"]),
  ],
)
data class ReminderEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val uuid: String,
  val title: String,
  val note: String,
  val category: ReminderCategory,
  val anchorYear: Int,
  val anchorMonth: Int,
  val anchorDay: Int,
  val yearKnown: Boolean,
  val hasExplicitTime: Boolean,
  val hour: Int,
  val minute: Int,
  val repeatRule: RepeatRuleType,
  val repeatInterval: Int,
  val repeatDaysOfWeekMask: Int,
  val isActive: Boolean,
  val isCompleted: Boolean,
  val nextOccurrenceEpochMillis: Long,
  val lastCompletedOccurrenceEpochMillis: Long?,
  val activeOverrideOriginalEpochMillis: Long? = null,
  val lastFiredOffsetForOccurrenceMillis: Long? = null,
  val isDeleted: Boolean,
  val deletedAtMillis: Long?,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(
  tableName = "reminder_offsets",
  foreignKeys = [
    ForeignKey(
      entity = ReminderEntity::class,
      parentColumns = ["id"],
      childColumns = ["reminderId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["reminderId"])],
)
data class ReminderOffsetEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val reminderId: Long,
  val offsetType: OffsetType,
  val offsetValue: Int,
  val exactEpochMillis: Long?,
  val deliveryChannel: DeliveryChannel,
  val isEnabled: Boolean,
)

@Entity(
  tableName = "occurrence_overrides",
  foreignKeys = [
    ForeignKey(
      entity = ReminderEntity::class,
      parentColumns = ["id"],
      childColumns = ["reminderId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["reminderId", "originalEpochMillis"])],
)
data class ReminderOccurrenceOverrideEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val reminderId: Long,
  val originalEpochMillis: Long,
  val overriddenEpochMillis: Long?, // null when skipped
  val isSkipped: Boolean,
  val createdAtMillis: Long,
)

data class ReminderWithOffsets(
  @Embedded val reminder: ReminderEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "reminderId",
  )
  val offsets: List<ReminderOffsetEntity>,
)

fun ReminderWithOffsets.toDomain(): Reminder =
  Reminder(
    id = reminder.id,
    uuid = reminder.uuid,
    title = reminder.title,
    note = reminder.note,
    category = reminder.category,
    anchorYear = reminder.anchorYear,
    anchorMonth = reminder.anchorMonth,
    anchorDay = reminder.anchorDay,
    yearKnown = reminder.yearKnown,
    hasExplicitTime = reminder.hasExplicitTime,
    hour = reminder.hour,
    minute = reminder.minute,
    repeatRule = reminder.repeatRule,
    repeatInterval = reminder.repeatInterval,
    repeatDaysOfWeekMask = reminder.repeatDaysOfWeekMask,
    isActive = reminder.isActive,
    isCompleted = reminder.isCompleted,
    nextOccurrenceEpochMillis = reminder.nextOccurrenceEpochMillis,
    lastCompletedOccurrenceEpochMillis = reminder.lastCompletedOccurrenceEpochMillis,
    activeOverrideOriginalEpochMillis = reminder.activeOverrideOriginalEpochMillis,
    lastFiredOffsetForOccurrenceMillis = reminder.lastFiredOffsetForOccurrenceMillis,
    isDeleted = reminder.isDeleted,
    deletedAtMillis = reminder.deletedAtMillis,
    createdAtMillis = reminder.createdAtMillis,
    updatedAtMillis = reminder.updatedAtMillis,
    offsets = offsets.map { it.toDomain() },
  )

fun ReminderOffsetEntity.toDomain(): ReminderOffset =
  ReminderOffset(
    id = id,
    reminderId = reminderId,
    offsetType = offsetType,
    offsetValue = offsetValue,
    exactEpochMillis = exactEpochMillis,
    deliveryChannel = deliveryChannel,
    isEnabled = isEnabled,
  )

fun Reminder.toEntity(): ReminderEntity =
  ReminderEntity(
    id = id,
    uuid = uuid,
    title = title,
    note = note,
    category = category,
    anchorYear = anchorYear,
    anchorMonth = anchorMonth,
    anchorDay = anchorDay,
    yearKnown = yearKnown,
    hasExplicitTime = hasExplicitTime,
    hour = hour,
    minute = minute,
    repeatRule = repeatRule,
    repeatInterval = repeatInterval,
    repeatDaysOfWeekMask = repeatDaysOfWeekMask,
    isActive = isActive,
    isCompleted = isCompleted,
    nextOccurrenceEpochMillis = nextOccurrenceEpochMillis,
    lastCompletedOccurrenceEpochMillis = lastCompletedOccurrenceEpochMillis,
    activeOverrideOriginalEpochMillis = activeOverrideOriginalEpochMillis,
    lastFiredOffsetForOccurrenceMillis = lastFiredOffsetForOccurrenceMillis,
    isDeleted = isDeleted,
    deletedAtMillis = deletedAtMillis,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
  )

fun ReminderOffset.toEntity(parentReminderId: Long): ReminderOffsetEntity =
  ReminderOffsetEntity(
    id = id,
    reminderId = parentReminderId,
    offsetType = offsetType,
    offsetValue = offsetValue,
    exactEpochMillis = exactEpochMillis,
    deliveryChannel = deliveryChannel,
    isEnabled = isEnabled,
  )

@Entity(
  tableName = "reminder_history",
  indices = [
    Index(value = ["eventEpochMillis"]),
    Index(value = ["reminderId", "occurrenceEpochMillis", "eventType"]),
  ],
)
data class ReminderHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val reminderId: Long,
  val reminderTitle: String,
  val category: ReminderCategory,
  val eventType: HistoryEventType,
  val deliveryChannel: DeliveryChannel,
  val offsetLabel: String,
  val occurrenceEpochMillis: Long,
  val eventEpochMillis: Long,
)

fun ReminderHistoryEntity.toDomain(): ReminderHistoryEntry =
  ReminderHistoryEntry(
    id = id,
    reminderId = reminderId,
    reminderTitle = reminderTitle,
    category = category,
    eventType = eventType,
    deliveryChannel = deliveryChannel,
    offsetLabel = offsetLabel,
    occurrenceEpochMillis = occurrenceEpochMillis,
    eventEpochMillis = eventEpochMillis,
  )

fun ReminderHistoryEntry.toEntity(): ReminderHistoryEntity =
  ReminderHistoryEntity(
    id = id,
    reminderId = reminderId,
    reminderTitle = reminderTitle,
    category = category,
    eventType = eventType,
    deliveryChannel = deliveryChannel,
    offsetLabel = offsetLabel,
    occurrenceEpochMillis = occurrenceEpochMillis,
    eventEpochMillis = eventEpochMillis,
  )
