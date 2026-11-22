package com.example.ui.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ReminderRepository
import com.example.domain.model.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecycleBinViewModel(
  private val reminderRepository: ReminderRepository,
) : ViewModel() {

  val deletedReminders: StateFlow<List<Reminder>> =
    reminderRepository.deletedReminders.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList(),
    )

  fun restoreReminder(reminderId: Long) {
    viewModelScope.launch { reminderRepository.restoreDeletedReminder(reminderId) }
  }

  fun deletePermanently(reminderId: Long) {
    viewModelScope.launch { reminderRepository.permanentlyDeleteReminder(reminderId) }
  }

  fun emptyRecycleBin() {
    viewModelScope.launch { reminderRepository.emptyRecycleBin() }
  }

  companion object {
    fun factory(reminderRepository: ReminderRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          RecycleBinViewModel(reminderRepository = reminderRepository) as T
      }
  }
}
