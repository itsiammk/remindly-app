package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.HistoryEventType
import com.example.domain.model.OffsetType
import com.example.domain.model.ReminderCategory
import com.example.domain.model.RepeatRuleType

class ReminderConverters {
  @TypeConverter
  fun fromCategory(value: ReminderCategory): String = value.name

  @TypeConverter
  fun toCategory(value: String): ReminderCategory =
    runCatching { ReminderCategory.valueOf(value) }.getOrDefault(ReminderCategory.OTHER)

  @TypeConverter
  fun fromRepeatRule(value: RepeatRuleType): String = value.name

  @TypeConverter
  fun toRepeatRule(value: String): RepeatRuleType =
    runCatching { RepeatRuleType.valueOf(value) }.getOrDefault(RepeatRuleType.NONE)

  @TypeConverter
  fun fromOffsetType(value: OffsetType): String = value.name

  @TypeConverter
  fun toOffsetType(value: String): OffsetType =
    runCatching { OffsetType.valueOf(value) }.getOrDefault(OffsetType.MINUTES_BEFORE)

  @TypeConverter
  fun fromDeliveryChannel(value: DeliveryChannel): String = value.name

  @TypeConverter
  fun toDeliveryChannel(value: String): DeliveryChannel =
    runCatching { DeliveryChannel.valueOf(value) }.getOrDefault(DeliveryChannel.NOTIFICATION)

  @TypeConverter
  fun fromHistoryEventType(value: HistoryEventType): String = value.name

  @TypeConverter
  fun toHistoryEventType(value: String): HistoryEventType =
    runCatching { HistoryEventType.valueOf(value) }.getOrDefault(HistoryEventType.FIRED_NOTIFICATION)
}
