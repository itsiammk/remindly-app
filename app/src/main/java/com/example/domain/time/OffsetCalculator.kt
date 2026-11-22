package com.example.domain.time

import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.ReminderOffset

data class PlannedOffsetFire(
  val offset: ReminderOffset,
  val fireAtMillis: Long,
  val isFallback: Boolean,
  val isConditionalFallback: Boolean, // true when an earlier non-fallback offset will fire before 8 AM
  val label: String,
)

object OffsetCalculator {

  private const val MILLIS_PER_MINUTE = 60_000L
  private const val MILLIS_PER_HOUR = 3_600_000L
  private const val MILLIS_PER_DAY = 86_400_000L

  fun labelFor(offset: ReminderOffset): String =
    when (offset.offsetType) {
      OffsetType.MINUTES_BEFORE ->
        when (offset.offsetValue) {
          0 -> "At reminder time"
          1 -> "1 minute before"
          else -> "${offset.offsetValue} minutes before"
        }
      OffsetType.HOURS_BEFORE ->
        if (offset.offsetValue == 1) "1 hour before" else "${offset.offsetValue} hours before"
      OffsetType.DAYS_BEFORE ->
        if (offset.offsetValue == 1) "1 day before" else "${offset.offsetValue} days before"
      OffsetType.EXACT_DATE_TIME -> {
        val ts = offset.exactEpochMillis
        if (ts != null) {
          "${IstDate.fromEpochMillis(ts).formatShort()} • ${IstTime.fromEpochMillis(ts).format12Hour()}"
        } else {
          "Custom date & time"
        }
      }
      OffsetType.FALLBACK_8AM -> "8:00 AM fallback on the day"
    }

  fun computeFireTimeMillis(
    offset: ReminderOffset,
    occurrenceMillis: Long,
  ): Long? {
    if (!offset.isEnabled) return null
    return when (offset.offsetType) {
      OffsetType.MINUTES_BEFORE ->
        occurrenceMillis - offset.offsetValue.coerceAtLeast(0) * MILLIS_PER_MINUTE
      OffsetType.HOURS_BEFORE ->
        occurrenceMillis - offset.offsetValue.coerceAtLeast(0) * MILLIS_PER_HOUR
      OffsetType.DAYS_BEFORE ->
        occurrenceMillis - offset.offsetValue.coerceAtLeast(0) * MILLIS_PER_DAY
      OffsetType.EXACT_DATE_TIME -> offset.exactEpochMillis
      OffsetType.FALLBACK_8AM -> IstDate.fromEpochMillis(occurrenceMillis).toEpochMillis(8, 0)
    }
  }

  /**
   * Plans all future offset triggers strictly after [nowMillis] for a given [occurrenceMillis].
   * Never returns any offset whose fire time is in the past (<= [nowMillis]).
   */
  fun planFutureOffsets(
    offsets: List<ReminderOffset>,
    occurrenceMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<PlannedOffsetFire> {
    val nonFallbackFires =
      offsets
        .filter { it.isEnabled && it.offsetType != OffsetType.FALLBACK_8AM }
        .mapNotNull { offset ->
          val fireAt = computeFireTimeMillis(offset, occurrenceMillis) ?: return@mapNotNull null
          if (fireAt <= nowMillis) return@mapNotNull null
          PlannedOffsetFire(
            offset = offset,
            fireAtMillis = fireAt,
            isFallback = false,
            isConditionalFallback = false,
            label = labelFor(offset),
          )
        }

    val fallbackOffset = offsets.firstOrNull { it.isEnabled && it.offsetType == OffsetType.FALLBACK_8AM }
    val fallbackFire =
      if (fallbackOffset != null) {
        val fireAt = computeFireTimeMillis(fallbackOffset, occurrenceMillis)
        if (fireAt != null && fireAt > nowMillis) {
          val hasEarlierScheduled = nonFallbackFires.any { it.fireAtMillis < fireAt }
          PlannedOffsetFire(
            offset = fallbackOffset,
            fireAtMillis = fireAt,
            isFallback = true,
            isConditionalFallback = hasEarlierScheduled,
            label =
              if (hasEarlierScheduled) "8:00 AM fallback (if earlier alerts fail)"
              else "8:00 AM fallback (active)",
          )
        } else {
          null
        }
      } else {
        null
      }

    return (nonFallbackFires + listOfNotNull(fallbackFire))
      .sortedWith(
        compareBy<PlannedOffsetFire> { it.fireAtMillis }.thenBy { it.isFallback }
      )
  }

  /**
   * Builds a readable IST timestamp + channel summary for live preview UI.
   */
  fun formatFirePreview(planned: PlannedOffsetFire): String {
    val date = IstDate.fromEpochMillis(planned.fireAtMillis).formatShort()
    val time = IstTime.fromEpochMillis(planned.fireAtMillis).format12Hour()
    val mode = if (planned.offset.deliveryChannel == DeliveryChannel.ALARM) "Alarm" else "Notify"
    return "$date • $time IST ($mode)"
  }
}
