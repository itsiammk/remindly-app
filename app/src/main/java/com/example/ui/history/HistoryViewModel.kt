package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ReminderRepository
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
  ALL,
  ALERTS,
  COMPLETED,
  MISSED,
}

data class HistoryUiState(
  val entries: List<ReminderHistoryEntry> = emptyList(),
  val filter: HistoryFilter = HistoryFilter.ALL,
  val totalCount: Int = 0,
)

class HistoryViewModel(
  private val reminderRepository: ReminderRepository,
) : ViewModel() {

  private val selectedFilter = MutableStateFlow(HistoryFilter.ALL)

  val uiState: StateFlow<HistoryUiState> =
    combine(reminderRepository.historyEntries, selectedFilter) { all, filter ->
      val filtered =
        when (filter) {
          HistoryFilter.ALL -> all
          HistoryFilter.ALERTS ->
            all.filter {
              it.eventType == HistoryEventType.FIRED_NOTIFICATION ||
                it.eventType == HistoryEventType.FIRED_ALARM ||
                it.eventType == HistoryEventType.SNOOZED
            }
          HistoryFilter.COMPLETED ->
            all.filter {
              it.eventType == HistoryEventType.COMPLETED ||
                it.eventType == HistoryEventType.SKIPPED ||
                it.eventType == HistoryEventType.MOVED
            }
          HistoryFilter.MISSED -> all.filter { it.eventType == HistoryEventType.MISSED }
        }
      HistoryUiState(
        entries = filtered,
        filter = filter,
        totalCount = all.size,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = HistoryUiState(),
    )

  fun setFilter(filter: HistoryFilter) {
    selectedFilter.value = filter
  }

  fun clearHistory() {
    viewModelScope.launch { reminderRepository.clearHistory() }
  }

  companion object {
    fun factory(reminderRepository: ReminderRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          HistoryViewModel(reminderRepository = reminderRepository) as T
      }
  }
}
