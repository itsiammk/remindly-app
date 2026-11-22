package com.example.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderOffset
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.UserPreferences
import com.example.domain.model.Weekday
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.OffsetCalculator
import com.example.domain.time.PlannedOffsetFire
import com.example.domain.time.RecurrenceCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderEditorUiState(
  val isEditMode: Boolean = false,
  val isLoading: Boolean = true,
  val reminderId: Long = 0L,
  val title: String = "",
  val titleError: String? = null,
  val note: String = "",
  val category: ReminderCategory = ReminderCategory.OTHER,
  val date: IstDate = IstDate.today(),
  val yearKnown: Boolean = true,
  val hasExplicitTime: Boolean = false,
  val time: IstTime = IstTime(9, 0),
  val defaultTime: IstTime = IstTime(0, 0),
  val repeatRule: RepeatRuleType = RepeatRuleType.NONE,
  val repeatInterval: Int = 2,
  val selectedWeekdays: Set<Weekday> = setOf(IstDate.today().weekday()),
  val offsets: List<ReminderOffset> = emptyList(),
  val isActive: Boolean = true,
  val activeOverrideOriginalEpochMillis: Long? = null,
  val userCustomizedRepeat: Boolean = false,
  val userCustomizedOffsets: Boolean = false,
  val isSaving: Boolean = false,
  val prefs: UserPreferences = UserPreferences(),
) {
  fun previewOccurrenceMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val hourToUse = if (hasExplicitTime) time.hour else defaultTime.hour
    val minuteToUse = if (hasExplicitTime) time.minute else defaultTime.minute
    val draft =
      Reminder(
        id = reminderId,
        title = title,
        category = category,
        anchorYear = date.year,
        anchorMonth = date.month,
        anchorDay = date.day,
        yearKnown = yearKnown,
        hasExplicitTime = hasExplicitTime,
        hour = hourToUse,
        minute = minuteToUse,
        repeatRule = repeatRule,
        repeatInterval = if (repeatRule == RepeatRuleType.EVERY_N_DAYS) repeatInterval else 1,
        repeatDaysOfWeekMask =
          if (repeatRule == RepeatRuleType.WEEKLY) Weekday.maskOf(selectedWeekdays) else 0,
      )
    return RecurrenceCalculator.computeInitialOccurrenceEpochMillis(
      reminder = draft,
      defaultHour = defaultTime.hour,
      defaultMinute = defaultTime.minute,
      nowMillis = nowMillis,
    )
  }

  fun previewPlannedFires(nowMillis: Long = System.currentTimeMillis()): List<PlannedOffsetFire> {
    val occMillis = previewOccurrenceMillis(nowMillis)
    return OffsetCalculator.planFutureOffsets(offsets, occMillis, nowMillis)
  }
}

sealed interface EditorEvent {
  data object Saved : EditorEvent

  data class Deleted(val reminderId: Long, val title: String) : EditorEvent
}

class ReminderEditorViewModel(
  private val reminderId: Long?,
  private val reminderRepository: ReminderRepository,
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ReminderEditorUiState())
  val uiState: StateFlow<ReminderEditorUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<EditorEvent>()
  val events = _events.asSharedFlow()

  private var loadedOriginal: Reminder? = null

  init {
    viewModelScope.launch {
      val prefs = preferencesRepository.currentPreferences()
      val defaultIstTime = IstTime(prefs.defaultHour, prefs.defaultMinute)

      if (reminderId != null && reminderId > 0L) {
        val existing = reminderRepository.getReminderById(reminderId)
        if (existing != null) {
          loadedOriginal = existing
          val anchorDate = IstDate(existing.anchorYear, existing.anchorMonth, existing.anchorDay)
          val weekdays =
            Weekday.fromMask(existing.repeatDaysOfWeekMask).ifEmpty { setOf(anchorDate.weekday()) }
          val normalizedOffsets = ensureStandardChecklistPresent(existing.offsets, prefs.defaultDeliveryChannel)
          _uiState.value =
            ReminderEditorUiState(
              isEditMode = true,
              isLoading = false,
              reminderId = existing.id,
              title = existing.title,
              note = existing.note,
              category = existing.category,
              date = anchorDate,
              yearKnown = existing.yearKnown,
              hasExplicitTime = existing.hasExplicitTime,
              time =
                if (existing.hasExplicitTime) IstTime(existing.hour, existing.minute)
                else IstTime(9, 0),
              defaultTime = defaultIstTime,
              repeatRule = existing.repeatRule,
              repeatInterval = existing.repeatInterval.coerceAtLeast(1),
              selectedWeekdays = weekdays,
              offsets = normalizedOffsets,
              isActive = existing.isActive,
              activeOverrideOriginalEpochMillis = existing.activeOverrideOriginalEpochMillis,
              userCustomizedRepeat = true,
              userCustomizedOffsets = true,
              prefs = prefs,
            )
          return@launch
        }
      }

      val today = IstDate.today()
      val initialOffsets =
        buildInitialOffsets(
          channel = prefs.defaultDeliveryChannel,
          includeAtTime = false,
          prefs = prefs,
        )
      _uiState.value =
        ReminderEditorUiState(
          isEditMode = false,
          isLoading = false,
          date = today,
          selectedWeekdays = setOf(today.weekday()),
          defaultTime = defaultIstTime,
          offsets = initialOffsets,
          prefs = prefs,
        )
    }
  }

  private fun buildInitialOffsets(
    channel: DeliveryChannel,
    includeAtTime: Boolean,
    prefs: UserPreferences,
  ): List<ReminderOffset> =
    listOf(
      ReminderOffset(
        offsetType = OffsetType.MINUTES_BEFORE,
        offsetValue = 0,
        deliveryChannel = channel,
        isEnabled = includeAtTime,
      ),
      ReminderOffset(
        offsetType = OffsetType.DAYS_BEFORE,
        offsetValue = 1,
        deliveryChannel = channel,
        isEnabled = prefs.defaultInclude1DayBefore,
      ),
      ReminderOffset(
        offsetType = OffsetType.HOURS_BEFORE,
        offsetValue = 4,
        deliveryChannel = channel,
        isEnabled = prefs.defaultInclude4HoursBefore,
      ),
      ReminderOffset(
        offsetType = OffsetType.MINUTES_BEFORE,
        offsetValue = 5,
        deliveryChannel = channel,
        isEnabled = prefs.defaultInclude5MinutesBefore,
      ),
      ReminderOffset(
        offsetType = OffsetType.FALLBACK_8AM,
        offsetValue = 0,
        deliveryChannel = channel,
        isEnabled = prefs.defaultIncludeFallback8Am,
      ),
    )

  private fun ensureStandardChecklistPresent(
    existing: List<ReminderOffset>,
    defaultChannel: DeliveryChannel,
  ): List<ReminderOffset> {
    val channel = existing.firstOrNull()?.deliveryChannel ?: defaultChannel
    val result = existing.toMutableList()
    if (result.none { it.isPresetAtTime() }) {
      result.add(
        0,
        ReminderOffset(
          offsetType = OffsetType.MINUTES_BEFORE,
          offsetValue = 0,
          deliveryChannel = channel,
          isEnabled = false,
        ),
      )
    }
    if (result.none { it.isPreset1DayBefore() }) {
      result.add(
        ReminderOffset(
          offsetType = OffsetType.DAYS_BEFORE,
          offsetValue = 1,
          deliveryChannel = channel,
          isEnabled = false,
        )
      )
    }
    if (result.none { it.isPreset4HoursBefore() }) {
      result.add(
        ReminderOffset(
          offsetType = OffsetType.HOURS_BEFORE,
          offsetValue = 4,
          deliveryChannel = channel,
          isEnabled = false,
        )
      )
    }
    if (result.none { it.isPreset5MinutesBefore() }) {
      result.add(
        ReminderOffset(
          offsetType = OffsetType.MINUTES_BEFORE,
          offsetValue = 5,
          deliveryChannel = channel,
          isEnabled = false,
        )
      )
    }
    if (result.none { it.isFallback8Am() }) {
      result.add(
        ReminderOffset(
          offsetType = OffsetType.FALLBACK_8AM,
          offsetValue = 0,
          deliveryChannel = channel,
          isEnabled = false,
        )
      )
    }
    return result
  }

  fun onTitleChanged(value: String) {
    _uiState.update { it.copy(title = value, titleError = null) }
  }

  fun onNoteChanged(value: String) {
    _uiState.update { it.copy(note = value) }
  }

  fun onCategorySelected(category: ReminderCategory) {
    _uiState.update { current ->
      val suggestedRepeat =
        if (!current.userCustomizedRepeat && !current.isEditMode) {
          when (category) {
            ReminderCategory.BIRTHDAY,
            ReminderCategory.ANNIVERSARY -> RepeatRuleType.YEARLY
            ReminderCategory.BILL -> RepeatRuleType.MONTHLY
            ReminderCategory.MEDICINE -> RepeatRuleType.DAILY
            ReminderCategory.OTHER -> RepeatRuleType.NONE
          }
        } else {
          current.repeatRule
        }

      val suggestedTimeToggle =
        if (!current.isEditMode && category == ReminderCategory.MEDICINE) {
          true
        } else if (!current.isEditMode &&
          (category == ReminderCategory.BIRTHDAY || category == ReminderCategory.ANNIVERSARY)
        ) {
          false
        } else {
          current.hasExplicitTime
        }

      val updatedOffsets =
        if (!current.userCustomizedOffsets && !current.isEditMode) {
          val suggestedChannel =
            if (category == ReminderCategory.MEDICINE) DeliveryChannel.ALARM
            else current.prefs.defaultDeliveryChannel
          current.offsets.map { off ->
            val enabled =
              if (off.isPresetAtTime()) suggestedTimeToggle && current.prefs.defaultIncludeAtTimeForTimed
              else off.isEnabled
            off.copy(deliveryChannel = suggestedChannel, isEnabled = enabled)
          }
        } else {
          current.offsets
        }

      current.copy(
        category = category,
        repeatRule = suggestedRepeat,
        hasExplicitTime = suggestedTimeToggle,
        offsets = updatedOffsets,
      )
    }
  }

  fun onDateSelected(date: IstDate) {
    _uiState.update { current ->
      val updatedWeekdays =
        if (current.repeatRule == RepeatRuleType.WEEKLY && current.selectedWeekdays.size <= 1) {
          setOf(date.weekday())
        } else {
          current.selectedWeekdays
        }
      current.copy(date = date, selectedWeekdays = updatedWeekdays)
    }
  }

  fun onYearKnownChanged(known: Boolean) {
    _uiState.update { it.copy(yearKnown = known) }
  }

  fun onExplicitTimeToggled(enabled: Boolean) {
    _uiState.update { current ->
      val updatedOffsets =
        if (!current.userCustomizedOffsets) {
          current.offsets.map { off ->
            if (off.isPresetAtTime()) {
              off.copy(isEnabled = enabled && current.prefs.defaultIncludeAtTimeForTimed)
            } else {
              off
            }
          }
        } else {
          current.offsets
        }
      current.copy(hasExplicitTime = enabled, offsets = updatedOffsets)
    }
  }

  fun onTimeSelected(time: IstTime) {
    _uiState.update { current ->
      val updatedOffsets =
        if (!current.userCustomizedOffsets) {
          current.offsets.map { off ->
            if (off.isPresetAtTime()) off.copy(isEnabled = current.prefs.defaultIncludeAtTimeForTimed)
            else off
          }
        } else {
          current.offsets
        }
      current.copy(time = time, hasExplicitTime = true, offsets = updatedOffsets)
    }
  }

  fun onOffsetEnabledChanged(index: Int, enabled: Boolean) {
    _uiState.update { current ->
      val list = current.offsets.toMutableList()
      if (index in list.indices) {
        list[index] = list[index].copy(isEnabled = enabled)
      }
      current.copy(offsets = list, userCustomizedOffsets = true)
    }
  }

  fun onOffsetChannelToggled(index: Int) {
    _uiState.update { current ->
      val list = current.offsets.toMutableList()
      if (index in list.indices) {
        val nextChannel =
          if (list[index].deliveryChannel == DeliveryChannel.NOTIFICATION) DeliveryChannel.ALARM
          else DeliveryChannel.NOTIFICATION
        list[index] = list[index].copy(deliveryChannel = nextChannel)
      }
      current.copy(offsets = list, userCustomizedOffsets = true)
    }
  }

  fun onRemoveOffset(index: Int) {
    _uiState.update { current ->
      val list = current.offsets.toMutableList()
      if (index in list.indices) {
        if (list[index].isCustomOffset()) {
          list.removeAt(index)
        } else {
          list[index] = list[index].copy(isEnabled = false)
        }
      }
      current.copy(offsets = list, userCustomizedOffsets = true)
    }
  }

  fun onAddCustomRelativeOffset(
    type: OffsetType,
    value: Int,
    channel: DeliveryChannel,
  ) {
    val cleanValue = value.coerceAtLeast(1)
    val newOffset =
      ReminderOffset(
        offsetType = type,
        offsetValue = cleanValue,
        deliveryChannel = channel,
        isEnabled = true,
      )
    _uiState.update { current ->
      current.copy(
        offsets = current.offsets + newOffset,
        userCustomizedOffsets = true,
      )
    }
  }

  fun onAddCustomExactOffset(
    date: IstDate,
    time: IstTime,
    channel: DeliveryChannel,
  ) {
    val exactMillis = date.toEpochMillis(time.hour, time.minute)
    val newOffset =
      ReminderOffset(
        offsetType = OffsetType.EXACT_DATE_TIME,
        offsetValue = 0,
        exactEpochMillis = exactMillis,
        deliveryChannel = channel,
        isEnabled = true,
      )
    _uiState.update { current ->
      current.copy(
        offsets = current.offsets + newOffset,
        userCustomizedOffsets = true,
      )
    }
  }

  fun onRepeatRuleSelected(rule: RepeatRuleType) {
    _uiState.update { current ->
      val weekdays =
        if (rule == RepeatRuleType.WEEKLY && current.selectedWeekdays.isEmpty()) {
          setOf(current.date.weekday())
        } else {
          current.selectedWeekdays
        }
      current.copy(
        repeatRule = rule,
        selectedWeekdays = weekdays,
        userCustomizedRepeat = true,
      )
    }
  }

  fun onWeekdayToggled(day: Weekday) {
    _uiState.update { current ->
      val next = current.selectedWeekdays.toMutableSet()
      if (day in next) {
        if (next.size > 1) next.remove(day)
      } else {
        next.add(day)
      }
      current.copy(selectedWeekdays = next, userCustomizedRepeat = true)
    }
  }

  fun onRepeatIntervalChanged(interval: Int) {
    _uiState.update {
      it.copy(
        repeatInterval = interval.coerceIn(1, 365),
        userCustomizedRepeat = true,
      )
    }
  }

  fun onActiveToggled(active: Boolean) {
    _uiState.update { it.copy(isActive = active) }
  }

  fun save() {
    val state = _uiState.value
    if (state.title.isBlank()) {
      _uiState.update { it.copy(titleError = "Please enter a title") }
      return
    }
    if (state.isSaving) return

    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      val base = loadedOriginal
      val hourToSave = if (state.hasExplicitTime) state.time.hour else state.defaultTime.hour
      val minuteToSave = if (state.hasExplicitTime) state.time.minute else state.defaultTime.minute
      val weekdayMask =
        if (state.repeatRule == RepeatRuleType.WEEKLY) {
          Weekday.maskOf(state.selectedWeekdays)
        } else {
          0
        }

      // Persist all checked offsets + custom offsets (keep unchecked standard presets disabled so edits retain user choice)
      val offsetsToSave = state.offsets.filter { it.isEnabled || !it.isCustomOffset() }

      val reminder =
        if (base != null) {
          base.copy(
            title = state.title.trim(),
            note = state.note.trim(),
            category = state.category,
            anchorYear = state.date.year,
            anchorMonth = state.date.month,
            anchorDay = state.date.day,
            yearKnown = state.yearKnown,
            hasExplicitTime = state.hasExplicitTime,
            hour = hourToSave,
            minute = minuteToSave,
            repeatRule = state.repeatRule,
            repeatInterval =
              if (state.repeatRule == RepeatRuleType.EVERY_N_DAYS) state.repeatInterval else 1,
            repeatDaysOfWeekMask = weekdayMask,
            isActive = state.isActive,
            offsets = offsetsToSave,
          )
        } else {
          Reminder(
            title = state.title.trim(),
            note = state.note.trim(),
            category = state.category,
            anchorYear = state.date.year,
            anchorMonth = state.date.month,
            anchorDay = state.date.day,
            yearKnown = state.yearKnown,
            hasExplicitTime = state.hasExplicitTime,
            hour = hourToSave,
            minute = minuteToSave,
            repeatRule = state.repeatRule,
            repeatInterval =
              if (state.repeatRule == RepeatRuleType.EVERY_N_DAYS) state.repeatInterval else 1,
            repeatDaysOfWeekMask = weekdayMask,
            isActive = state.isActive,
            offsets = offsetsToSave,
          )
        }

      reminderRepository.saveReminder(reminder)
      _events.emit(EditorEvent.Saved)
    }
  }

  fun delete() {
    val id = _uiState.value.reminderId
    val title = _uiState.value.title
    if (id <= 0L) return
    viewModelScope.launch {
      reminderRepository.softDeleteReminder(id)
      _events.emit(EditorEvent.Deleted(id, title))
    }
  }

  fun skipThisOccurrence() {
    val state = _uiState.value
    if (!state.isEditMode || state.reminderId <= 0L || state.repeatRule == RepeatRuleType.NONE) return
    viewModelScope.launch {
      reminderRepository.skipCurrentOccurrence(state.reminderId)
      _events.emit(EditorEvent.Saved)
    }
  }

  fun moveThisOccurrence(date: IstDate, time: IstTime) {
    val state = _uiState.value
    if (!state.isEditMode || state.reminderId <= 0L || state.repeatRule == RepeatRuleType.NONE) return
    viewModelScope.launch {
      reminderRepository.moveCurrentOccurrence(
        reminderId = state.reminderId,
        newOccurrenceMillis = date.toEpochMillis(time.hour, time.minute),
      )
      _events.emit(EditorEvent.Saved)
    }
  }

  companion object {
    fun factory(
      reminderId: Long?,
      reminderRepository: ReminderRepository,
      preferencesRepository: PreferencesRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          ReminderEditorViewModel(reminderId, reminderRepository, preferencesRepository) as T
      }
  }
}
