package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.Weekday
import com.example.domain.time.IstDate
import com.example.domain.time.OffsetCalculator
import com.example.domain.time.PlannedOffsetFire
import com.example.domain.time.RecurrenceCalculator

interface ReminderScheduler {
  fun scheduleReminder(
    reminder: Reminder,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  )

  fun scheduleRemainingOffsetsForCompletedOccurrence(
    reminder: Reminder,
    completedOccurrenceMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
  )

  fun scheduleNextCycleAfterFire(
    reminder: Reminder,
    firedOccurrenceMillis: Long,
    defaultHour: Int,
    defaultMinute: Int,
  )

  fun scheduleSnooze(
    reminderId: Long,
    title: String,
    category: ReminderCategory,
    deliveryChannel: DeliveryChannel,
    snoozeMinutes: Int,
    nowMillis: Long = System.currentTimeMillis(),
  )

  fun scheduleTestInOneMinute(
    deliveryChannel: DeliveryChannel,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long

  fun cancelReminder(reminderId: Long)

  fun rescheduleAll(
    reminders: List<Reminder>,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<Reminder>

  fun scheduleWeeklyBackupReminder(
    enabled: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
  )
}

object NoOpReminderScheduler : ReminderScheduler {
  override fun scheduleReminder(
    reminder: Reminder,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long,
  ) {}

  override fun scheduleRemainingOffsetsForCompletedOccurrence(
    reminder: Reminder,
    completedOccurrenceMillis: Long,
    nowMillis: Long,
  ) {}

  override fun scheduleNextCycleAfterFire(
    reminder: Reminder,
    firedOccurrenceMillis: Long,
    defaultHour: Int,
    defaultMinute: Int,
  ) {}

  override fun scheduleSnooze(
    reminderId: Long,
    title: String,
    category: ReminderCategory,
    deliveryChannel: DeliveryChannel,
    snoozeMinutes: Int,
    nowMillis: Long,
  ) {}

  override fun scheduleTestInOneMinute(
    deliveryChannel: DeliveryChannel,
    nowMillis: Long,
  ): Long = nowMillis + 60_000L

  override fun cancelReminder(reminderId: Long) {}

  override fun rescheduleAll(
    reminders: List<Reminder>,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long,
  ): List<Reminder> = emptyList()

  override fun scheduleWeeklyBackupReminder(enabled: Boolean, nowMillis: Long) {}
}

class AlarmScheduler(private val context: Context) : ReminderScheduler {

  private val alarmManager: AlarmManager?
    get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  override fun scheduleReminder(
    reminder: Reminder,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long,
  ) {
    cancelReminder(reminder.id)
    if (!reminder.isActive || reminder.isCompleted || reminder.isDeleted) return

    val effectiveOffsets =
      reminder.offsets.ifEmpty {
        listOf(
          ReminderOffset(
            offsetType = OffsetType.MINUTES_BEFORE,
            offsetValue = 0,
            deliveryChannel = reminder.primaryDeliveryChannel(),
            isEnabled = true,
          )
        )
      }

    val occurrenceMillis = reminder.nextOccurrenceEpochMillis
    val planned =
      OffsetCalculator.planFutureOffsets(
        offsets = effectiveOffsets,
        occurrenceMillis = occurrenceMillis,
        nowMillis = nowMillis,
      )

    if (planned.isNotEmpty()) {
      schedulePlannedList(
        reminder = reminder,
        occurrenceMillis = occurrenceMillis,
        plannedList = planned,
        slotBase = SLOT_PRIMARY_BASE,
        maxSlots = MAX_PRIMARY_SLOTS,
        markFinalOnLast = reminder.repeatRule != RepeatRuleType.NONE,
      )
    } else if (reminder.repeatRule != RepeatRuleType.NONE) {
      // Current occurrence + its offsets are already in the past; ensure next future cycle is armed
      val seriesAnchor = reminder.activeOverrideOriginalEpochMillis ?: occurrenceMillis
      val futureTrigger =
        RecurrenceCalculator.computeNextOccurrenceEpochMillis(
          reminder = reminder,
          defaultHour = defaultHour,
          defaultMinute = defaultMinute,
          afterEpochMillis = maxOf(seriesAnchor, nowMillis),
        )
      val futurePlanned =
        OffsetCalculator.planFutureOffsets(
          offsets = effectiveOffsets,
          occurrenceMillis = futureTrigger,
          nowMillis = nowMillis,
        )
      if (futurePlanned.isNotEmpty()) {
        schedulePlannedList(
          reminder = reminder,
          occurrenceMillis = futureTrigger,
          plannedList = futurePlanned,
          slotBase = SLOT_PRIMARY_BASE,
          maxSlots = MAX_PRIMARY_SLOTS,
          markFinalOnLast = true,
        )
      }
    }
  }

  override fun scheduleRemainingOffsetsForCompletedOccurrence(
    reminder: Reminder,
    completedOccurrenceMillis: Long,
    nowMillis: Long,
  ) {
    if (!reminder.isActive || reminder.isDeleted) return
    val effectiveOffsets = reminder.offsets.ifEmpty { return }
    val remaining =
      OffsetCalculator.planFutureOffsets(
        offsets = effectiveOffsets,
        occurrenceMillis = completedOccurrenceMillis,
        nowMillis = nowMillis,
      )
    if (remaining.isNotEmpty()) {
      schedulePlannedList(
        reminder = reminder,
        occurrenceMillis = completedOccurrenceMillis,
        plannedList = remaining,
        slotBase = SLOT_GRACE_BASE,
        maxSlots = MAX_GRACE_SLOTS,
        markFinalOnLast = false,
      )
    }
  }

  override fun scheduleNextCycleAfterFire(
    reminder: Reminder,
    firedOccurrenceMillis: Long,
    defaultHour: Int,
    defaultMinute: Int,
  ) {
    if (!reminder.isActive || reminder.isCompleted || reminder.isDeleted) return
    if (reminder.repeatRule == RepeatRuleType.NONE) return

    val nowMillis = System.currentTimeMillis()
    val seriesAnchor = reminder.activeOverrideOriginalEpochMillis ?: firedOccurrenceMillis
    val threshold = maxOf(seriesAnchor, firedOccurrenceMillis, nowMillis)
    val nextTrigger =
      RecurrenceCalculator.computeNextOccurrenceEpochMillis(
        reminder = reminder,
        defaultHour = defaultHour,
        defaultMinute = defaultMinute,
        afterEpochMillis = threshold,
      )
    val effectiveOffsets =
      reminder.offsets.ifEmpty {
        listOf(
          ReminderOffset(
            offsetType = OffsetType.MINUTES_BEFORE,
            offsetValue = 0,
            deliveryChannel = reminder.primaryDeliveryChannel(),
            isEnabled = true,
          )
        )
      }
    val nextPlanned =
      OffsetCalculator.planFutureOffsets(
        offsets = effectiveOffsets,
        occurrenceMillis = nextTrigger,
        nowMillis = nowMillis,
      )
    if (nextPlanned.isNotEmpty()) {
      schedulePlannedList(
        reminder = reminder,
        occurrenceMillis = nextTrigger,
        plannedList = nextPlanned,
        slotBase = SLOT_PRIMARY_BASE,
        maxSlots = MAX_PRIMARY_SLOTS,
        markFinalOnLast = true,
      )
    }
  }

  override fun scheduleSnooze(
    reminderId: Long,
    title: String,
    category: ReminderCategory,
    deliveryChannel: DeliveryChannel,
    snoozeMinutes: Int,
    nowMillis: Long,
  ) {
    val triggerAt = nowMillis + snoozeMinutes.coerceAtLeast(1) * 60_000L
    scheduleExact(
      requestCode = requestCodeFor(reminderId, SLOT_SNOOZE),
      triggerAtMillis = triggerAt,
      reminderId = reminderId,
      title = title,
      note = "Snoozed for $snoozeMinutes min",
      category = category,
      deliveryChannel = deliveryChannel,
      occurrenceMillis = triggerAt,
      offsetLabel = "Snoozed",
      isFallback = false,
      isFinalForOccurrence = false,
      isSnooze = true,
      isTest = false,
    )
  }

  override fun scheduleTestInOneMinute(
    deliveryChannel: DeliveryChannel,
    nowMillis: Long,
  ): Long {
    val triggerAt = nowMillis + 60_000L
    val requestCode =
      if (deliveryChannel == DeliveryChannel.ALARM) REQUEST_CODE_TEST_ALARM
      else REQUEST_CODE_TEST_NOTIFICATION

    scheduleExact(
      requestCode = requestCode,
      triggerAtMillis = triggerAt,
      reminderId = -1L,
      title =
        if (deliveryChannel == DeliveryChannel.ALARM) "Remindly Test Alarm"
        else "Remindly Test Notification",
      note = "Scheduled 1 minute ago to verify reliability",
      category = ReminderCategory.OTHER,
      deliveryChannel = deliveryChannel,
      occurrenceMillis = triggerAt,
      offsetLabel = "Reliability test",
      isFallback = false,
      isFinalForOccurrence = false,
      isSnooze = false,
      isTest = true,
    )
    return triggerAt
  }

  override fun cancelReminder(reminderId: Long) {
    if (reminderId <= 0L) return
    val am = alarmManager ?: return
    for (slot in 0..SLOT_SNOOZE) {
      val intent =
        Intent(context, ReminderTriggerReceiver::class.java).apply {
          action = ReminderTriggerReceiver.ACTION_TRIGGER
        }
      val pending =
        PendingIntent.getBroadcast(
          context,
          requestCodeFor(reminderId, slot),
          intent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
      if (pending != null) {
        am.cancel(pending)
        pending.cancel()
      }
    }
  }

  override fun rescheduleAll(
    reminders: List<Reminder>,
    defaultHour: Int,
    defaultMinute: Int,
    nowMillis: Long,
  ): List<Reminder> {
    val missed = mutableListOf<Reminder>()
    val todayIst = IstDate.today(nowMillis)

    reminders.forEach { reminder ->
      if (!reminder.isActive || reminder.isCompleted || reminder.isDeleted) {
        cancelReminder(reminder.id)
      } else {
        val occDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
        val isDuePast =
          if (reminder.hasExplicitTime) {
            reminder.nextOccurrenceEpochMillis <= nowMillis
          } else {
            occDate < todayIst
          }
        if (isDuePast) {
          missed.add(reminder)
        }
        scheduleReminder(reminder, defaultHour, defaultMinute, nowMillis)
      }
    }
    return missed
  }

  override fun scheduleWeeklyBackupReminder(enabled: Boolean, nowMillis: Long) {
    val am = alarmManager ?: return
    val intent =
      Intent(context, ReminderTriggerReceiver::class.java).apply {
        action = ReminderTriggerReceiver.ACTION_WEEKLY_BACKUP
      }
    val existing =
      PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_WEEKLY_BACKUP,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
    if (!enabled) {
      if (existing != null) {
        am.cancel(existing)
        existing.cancel()
      }
      return
    }

    var targetDate = IstDate.today(nowMillis)
    var candidateMillis = targetDate.toEpochMillis(10, 0)
    while (targetDate.weekday() != Weekday.SUN || candidateMillis <= nowMillis + 60_000L) {
      targetDate = targetDate.plusDays(1)
      candidateMillis = targetDate.toEpochMillis(10, 0)
    }

    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_WEEKLY_BACKUP,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, candidateMillis, pendingIntent)
  }

  private fun schedulePlannedList(
    reminder: Reminder,
    occurrenceMillis: Long,
    plannedList: List<PlannedOffsetFire>,
    slotBase: Int,
    maxSlots: Int,
    markFinalOnLast: Boolean,
  ) {
    val capped = plannedList.take(maxSlots)
    capped.forEachIndexed { idx, planned ->
      val isLast = markFinalOnLast && idx == capped.lastIndex
      scheduleExact(
        requestCode = requestCodeFor(reminder.id, slotBase + idx),
        triggerAtMillis = planned.fireAtMillis,
        reminderId = reminder.id,
        title = reminder.title,
        note = reminder.note,
        category = reminder.category,
        deliveryChannel = planned.offset.deliveryChannel,
        occurrenceMillis = occurrenceMillis,
        offsetLabel = OffsetCalculator.labelFor(planned.offset),
        isFallback = planned.isFallback,
        isFinalForOccurrence = isLast,
        isSnooze = false,
        isTest = false,
      )
    }
  }

  private fun scheduleExact(
    requestCode: Int,
    triggerAtMillis: Long,
    reminderId: Long,
    title: String,
    note: String,
    category: ReminderCategory,
    deliveryChannel: DeliveryChannel,
    occurrenceMillis: Long,
    offsetLabel: String,
    isFallback: Boolean,
    isFinalForOccurrence: Boolean,
    isSnooze: Boolean,
    isTest: Boolean,
  ) {
    val am = alarmManager ?: return
    val triggerIntent =
      Intent(context, ReminderTriggerReceiver::class.java).apply {
        action = ReminderTriggerReceiver.ACTION_TRIGGER
        putExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, reminderId)
        putExtra(ReminderTriggerReceiver.EXTRA_TITLE, title)
        putExtra(ReminderTriggerReceiver.EXTRA_NOTE, note)
        putExtra(ReminderTriggerReceiver.EXTRA_CATEGORY, category.name)
        putExtra(ReminderTriggerReceiver.EXTRA_CHANNEL, deliveryChannel.name)
        putExtra(ReminderTriggerReceiver.EXTRA_OCCURRENCE_MILLIS, occurrenceMillis)
        putExtra(ReminderTriggerReceiver.EXTRA_OFFSET_LABEL, offsetLabel)
        putExtra(ReminderTriggerReceiver.EXTRA_IS_FALLBACK, isFallback)
        putExtra(ReminderTriggerReceiver.EXTRA_IS_FINAL_FOR_OCCURRENCE, isFinalForOccurrence)
        putExtra(ReminderTriggerReceiver.EXTRA_IS_SNOOZE, isSnooze)
        putExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, isTest)
      }

    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        requestCode,
        triggerIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val canExact = PermissionAndOemHelper.canScheduleExactAlarms(context)

    if (deliveryChannel == DeliveryChannel.ALARM && canExact) {
      val showIntent =
        PendingIntent.getActivity(
          context,
          requestCode,
          Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          },
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), pendingIntent)
    } else if (canExact) {
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
  }

  companion object {
    private const val SLOT_PRIMARY_BASE = 0
    private const val MAX_PRIMARY_SLOTS = 28
    private const val SLOT_GRACE_BASE = 30
    private const val MAX_GRACE_SLOTS = 28
    private const val SLOT_SNOOZE = 62
    private const val REQUEST_CODE_TEST_NOTIFICATION = 1_900_001
    private const val REQUEST_CODE_TEST_ALARM = 1_900_002
    private const val REQUEST_CODE_WEEKLY_BACKUP = 1_900_050

    fun requestCodeFor(reminderId: Long, slot: Int): Int {
      val safeId = (reminderId.toInt() and 0x00FFFFFF).coerceAtLeast(1)
      val safeSlot = slot and 0x3F
      return (safeId shl 6) or safeSlot
    }

    fun notificationIdFor(reminderId: Long, isTest: Boolean = false): Int =
      if (isTest) 88_001 else (10_000 + (reminderId.toInt() and 0x000FFFFF))
  }
}
