package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderSortOption
import com.example.domain.model.UserPreferences
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderSection(
  val bucket: ReminderBucket,
  val title: String,
  val items: List<Reminder>,
)

sealed interface HomeUiState {
  data object Loading : HomeUiState

  data class Ready(
    val sections: List<ReminderSection>,
    val totalCount: Int,
    val searchQuery: String,
    val selectedCategory: ReminderCategory?,
    val sortOption: ReminderSortOption,
    val nowMillis: Long,
    val preferences: UserPreferences,
  ) : HomeUiState
}

sealed interface HomeEvent {
  data class DeletedWithUndo(val reminderId: Long, val title: String) : HomeEvent

  data class CompletedOneTimeWithUndo(val reminderId: Long, val title: String) : HomeEvent

  data class CompletedRecurring(val title: String, val nextDateFormatted: String) : HomeEvent

  data class SkippedOccurrenceWithUndo(val previousState: Reminder, val nextDateFormatted: String) :
    HomeEvent

  data class MovedOccurrenceWithUndo(
    val previousState: Reminder,
    val movedDateTimeFormatted: String,
  ) : HomeEvent
}

class HomeViewModel(
  private val reminderRepository: ReminderRepository,
  preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val selectedCategory = MutableStateFlow<ReminderCategory?>(null)
  private val sortOption = MutableStateFlow(ReminderSortOption.SOONEST)
  private val refreshTrigger = MutableStateFlow(0L)

  private val _events = MutableSharedFlow<HomeEvent>()
  val events = _events.asSharedFlow()

  private val tickerFlow = flow {
    while (true) {
      emit(System.currentTimeMillis())
      delay(30_000L)
    }
  }

  private val filterState = combine(searchQuery, selectedCategory, sortOption) { q, cat, sort ->
    Triple(q, cat, sort)
  }

  val uiState: StateFlow<HomeUiState> =
    combine(
      reminderRepository.activeReminders,
      preferencesRepository.userPreferences,
      filterState,
      tickerFlow,
      refreshTrigger,
    ) { reminders, prefs, (query, category, sort), tickNow, _ ->
      val nowMillis = maxOf(tickNow, System.currentTimeMillis())

      val filtered =
        reminders.filter { reminder ->
          val matchesCategory = category == null || reminder.category == category
          val trimmedQuery = query.trim()
          val matchesQuery =
            trimmedQuery.isEmpty() ||
              reminder.title.contains(trimmedQuery, ignoreCase = true) ||
              reminder.note.contains(trimmedQuery, ignoreCase = true)
          matchesCategory && matchesQuery
        }

      val sorted =
        when (sort) {
          ReminderSortOption.SOONEST ->
            filtered.sortedWith(
              compareBy<Reminder> { it.nextOccurrenceEpochMillis }.thenBy {
                it.title.lowercase()
              }
            )
          ReminderSortOption.TITLE_ASC ->
            filtered.sortedWith(
              compareBy<Reminder> { it.title.lowercase() }.thenBy { it.nextOccurrenceEpochMillis }
            )
          ReminderSortOption.CATEGORY ->
            filtered.sortedWith(
              compareBy<Reminder> { it.category.ordinal }
                .thenBy { it.nextOccurrenceEpochMillis }
                .thenBy { it.title.lowercase() }
            )
        }

      val grouped = sorted.groupBy { RecurrenceCalculator.classifyBucket(it, nowMillis) }
      val bucketOrder =
        listOf(
          ReminderBucket.MISSED to "Missed",
          ReminderBucket.TODAY to "Today",
          ReminderBucket.TOMORROW to "Tomorrow",
          ReminderBucket.THIS_WEEK to "This Week",
          ReminderBucket.LATER to "Later",
          ReminderBucket.PAUSED to "Paused",
        )

      val sections =
        bucketOrder.mapNotNull { (bucket, label) ->
          val items = grouped[bucket].orEmpty()
          if (items.isEmpty()) null else ReminderSection(bucket, label, items)
        }

      HomeUiState.Ready(
        sections = sections,
        totalCount = reminders.size,
        searchQuery = query,
        selectedCategory = category,
        sortOption = sort,
        nowMillis = nowMillis,
        preferences = prefs,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
      )

  fun onSearchQueryChanged(query: String) {
    searchQuery.value = query
  }

  fun onCategorySelected(category: ReminderCategory?) {
    selectedCategory.value = if (selectedCategory.value == category) null else category
  }

  fun onSortOptionSelected(option: ReminderSortOption) {
    sortOption.value = option
  }

  fun clearFilters() {
    searchQuery.value = ""
    selectedCategory.value = null
  }

  fun refreshNow() {
    refreshTrigger.value = System.currentTimeMillis()
  }

  fun markDone(reminder: Reminder) {
    viewModelScope.launch {
      reminderRepository.markReminderDone(reminder.id)
      val updated = reminderRepository.getReminderById(reminder.id)
      if (updated == null || updated.isCompleted) {
        _events.emit(HomeEvent.CompletedOneTimeWithUndo(reminder.id, reminder.title))
      } else {
        val nextDate =
          com.example.domain.time.IstDate.fromEpochMillis(updated.nextOccurrenceEpochMillis)
            .formatShort()
        _events.emit(HomeEvent.CompletedRecurring(reminder.title, nextDate))
      }
    }
  }

  fun undoCompleteOneTime(reminderId: Long) {
    viewModelScope.launch { reminderRepository.restoreCompletedOneTimeReminder(reminderId) }
  }

  fun togglePause(reminder: Reminder) {
    viewModelScope.launch {
      reminderRepository.setReminderActive(reminder.id, !reminder.isActive)
    }
  }

  fun deleteReminder(reminder: Reminder) {
    viewModelScope.launch {
      reminderRepository.softDeleteReminder(reminder.id)
      _events.emit(HomeEvent.DeletedWithUndo(reminder.id, reminder.title))
    }
  }

  fun undoDelete(reminderId: Long) {
    viewModelScope.launch { reminderRepository.restoreDeletedReminder(reminderId) }
  }

  fun skipOccurrence(reminder: Reminder) {
    viewModelScope.launch {
      val updated = reminderRepository.skipCurrentOccurrence(reminder.id)
      if (updated != null) {
        val nextDate = IstDate.fromEpochMillis(updated.nextOccurrenceEpochMillis).formatShort()
        _events.emit(HomeEvent.SkippedOccurrenceWithUndo(reminder, nextDate))
      }
    }
  }

  fun moveOccurrence(reminder: Reminder, date: IstDate, time: IstTime) {
    viewModelScope.launch {
      val newMillis = date.toEpochMillis(time.hour, time.minute)
      val updated = reminderRepository.moveCurrentOccurrence(reminder.id, newMillis)
      if (updated != null) {
        _events.emit(
          HomeEvent.MovedOccurrenceWithUndo(
            previousState = reminder,
            movedDateTimeFormatted = "${date.formatShort()} • ${time.format12Hour()}",
          )
        )
      }
    }
  }

  fun undoOccurrenceOverride(previousState: Reminder) {
    viewModelScope.launch { reminderRepository.restorePreviousOccurrenceState(previousState) }
  }

  companion object {
    fun factory(
      reminderRepository: ReminderRepository,
      preferencesRepository: PreferencesRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          HomeViewModel(reminderRepository, preferencesRepository) as T
      }
  }
}
