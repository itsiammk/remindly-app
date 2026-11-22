package com.example.ui.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.alarm.ReminderScheduler
import com.example.backup.EncryptedRemindEnvelope
import com.example.backup.RemindBackupCodec
import com.example.backup.RemindParseResult
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.ImportDuplicatePolicy
import com.example.data.repository.ImportSummary
import com.example.data.repository.ReminderRepository
import com.example.domain.model.Reminder
import com.example.domain.model.UserPreferences
import com.example.domain.time.IstDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class BackupTab {
  EXPORT,
  IMPORT,
}

data class ImportPreviewItem(
  val reminder: Reminder,
  val isDuplicate: Boolean,
)

data class LoadedImportPreview(
  val sourceName: String,
  val exportedAtMillis: Long,
  val wasEncrypted: Boolean,
  val items: List<ImportPreviewItem>,
) {
  val duplicateCount: Int
    get() = items.count { it.isDuplicate }
}

data class BackupShareUiState(
  val activeTab: BackupTab = BackupTab.EXPORT,
  val exportableReminders: List<Reminder> = emptyList(),
  val selectedIds: Set<Long> = emptySet(),
  val hasInitializedSelection: Boolean = false,
  val usePassword: Boolean = false,
  val exportPassword: String = "",
  val isBusy: Boolean = false,
  val pendingEnvelope: EncryptedRemindEnvelope? = null,
  val pendingSourceName: String = "",
  val decryptPasswordInput: String = "",
  val decryptError: String? = null,
  val importError: String? = null,
  val importPreview: LoadedImportPreview? = null,
  val selectedImportIndices: Set<Int> = emptySet(),
  val duplicatePolicy: ImportDuplicatePolicy = ImportDuplicatePolicy.SKIP,
  val preferences: UserPreferences = UserPreferences(),
)

sealed interface BackupEvent {
  data class ShareUriReady(val uri: Uri, val count: Int) : BackupEvent

  data class ExportSaved(val count: Int) : BackupEvent

  data class ImportCompleted(val summary: ImportSummary) : BackupEvent

  data class ErrorMessage(val message: String) : BackupEvent
}

class BackupShareViewModel(
  private val appContext: Context,
  private val reminderRepository: ReminderRepository,
  private val preferencesRepository: PreferencesRepository,
  private val scheduler: ReminderScheduler,
) : ViewModel() {

  private data class DraftState(
    val activeTab: BackupTab = BackupTab.EXPORT,
    val selectedIds: Set<Long> = emptySet(),
    val hasInitializedSelection: Boolean = false,
    val usePassword: Boolean = false,
    val exportPassword: String = "",
    val isBusy: Boolean = false,
    val pendingEnvelope: EncryptedRemindEnvelope? = null,
    val pendingSourceName: String = "",
    val decryptPasswordInput: String = "",
    val decryptError: String? = null,
    val importError: String? = null,
    val importPreview: LoadedImportPreview? = null,
    val selectedImportIndices: Set<Int> = emptySet(),
    val duplicatePolicy: ImportDuplicatePolicy = ImportDuplicatePolicy.SKIP,
  )

  private val draft = MutableStateFlow(DraftState())
  private val _events = MutableSharedFlow<BackupEvent>()
  val events = _events.asSharedFlow()

  val uiState: StateFlow<BackupShareUiState> =
    combine(
      reminderRepository.activeReminders,
      preferencesRepository.userPreferences,
      draft,
    ) { reminders, prefs, d ->
      val validIds = reminders.map { it.id }.toSet()
      val effectiveSelection =
        if (!d.hasInitializedSelection) {
          validIds
        } else {
          d.selectedIds.intersect(validIds)
        }
      BackupShareUiState(
        activeTab = d.activeTab,
        exportableReminders = reminders,
        selectedIds = effectiveSelection,
        hasInitializedSelection = d.hasInitializedSelection,
        usePassword = d.usePassword,
        exportPassword = d.exportPassword,
        isBusy = d.isBusy,
        pendingEnvelope = d.pendingEnvelope,
        pendingSourceName = d.pendingSourceName,
        decryptPasswordInput = d.decryptPasswordInput,
        decryptError = d.decryptError,
        importError = d.importError,
        importPreview = d.importPreview,
        selectedImportIndices = d.selectedImportIndices,
        duplicatePolicy = d.duplicatePolicy,
        preferences = prefs,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupShareUiState(),
      )

  fun selectTab(tab: BackupTab) {
    draft.update { it.copy(activeTab = tab) }
  }

  fun toggleReminderSelection(id: Long) {
    val currentIds = uiState.value.selectedIds
    val next =
      if (id in currentIds) currentIds - id
      else currentIds + id
    draft.update { it.copy(selectedIds = next, hasInitializedSelection = true) }
  }

  fun toggleSelectAll() {
    val allIds = uiState.value.exportableReminders.map { it.id }.toSet()
    val allSelected = allIds.isNotEmpty() && uiState.value.selectedIds.containsAll(allIds)
    draft.update {
      it.copy(
        selectedIds = if (allSelected) emptySet() else allIds,
        hasInitializedSelection = true,
      )
    }
  }

  fun setUsePassword(enabled: Boolean) {
    draft.update {
      it.copy(
        usePassword = enabled,
        exportPassword = if (enabled) it.exportPassword else "",
      )
    }
  }

  fun setExportPassword(password: String) {
    draft.update { it.copy(exportPassword = password) }
  }

  fun setWeeklyBackupEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setWeeklyBackupReminderEnabled(enabled)
      scheduler.scheduleWeeklyBackupReminder(enabled)
    }
  }

  fun suggestedFileName(nowMillis: Long = System.currentTimeMillis()): String {
    val d = IstDate.today(nowMillis)
    val monthNames = arrayOf(
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    return "remindly_${d.day}_${monthNames[d.month - 1]}_${d.year}.remind"
  }

  fun shareSelectedReminders() {
    val state = uiState.value
    val chosen = state.exportableReminders.filter { it.id in state.selectedIds }
    if (chosen.isEmpty()) {
      viewModelScope.launch { _events.emit(BackupEvent.ErrorMessage("Select at least one reminder.")) }
      return
    }
    if (state.usePassword && state.exportPassword.length < 4) {
      viewModelScope.launch {
        _events.emit(BackupEvent.ErrorMessage("Enter a password of at least 4 characters."))
      }
      return
    }

    viewModelScope.launch {
      draft.update { it.copy(isBusy = true) }
      try {
        val uri =
          withContext(Dispatchers.IO) {
            val encoded =
              RemindBackupCodec.encode(
                reminders = chosen,
                password = if (state.usePassword) state.exportPassword else null,
              )
            val exportDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
            exportDir.listFiles()?.forEach { old -> old.delete() }
            val outFile = File(exportDir, suggestedFileName())
            outFile.writeText(encoded, Charsets.UTF_8)
            FileProvider.getUriForFile(
              appContext,
              "${BuildConfig.APPLICATION_ID}.fileprovider",
              outFile,
            )
          }
        preferencesRepository.setLastBackupEpochMillis(System.currentTimeMillis())
        _events.emit(BackupEvent.ShareUriReady(uri, chosen.size))
      } catch (_: Exception) {
        _events.emit(BackupEvent.ErrorMessage("Could not prepare .remind file for sharing."))
      } finally {
        draft.update { it.copy(isBusy = false) }
      }
    }
  }

  fun saveExportToUri(uri: Uri) {
    val state = uiState.value
    val chosen = state.exportableReminders.filter { it.id in state.selectedIds }
    if (chosen.isEmpty()) return
    if (state.usePassword && state.exportPassword.length < 4) {
      viewModelScope.launch {
        _events.emit(BackupEvent.ErrorMessage("Enter a password of at least 4 characters."))
      }
      return
    }

    viewModelScope.launch {
      draft.update { it.copy(isBusy = true) }
      try {
        withContext(Dispatchers.IO) {
          val encoded =
            RemindBackupCodec.encode(
              reminders = chosen,
              password = if (state.usePassword) state.exportPassword else null,
            )
          appContext.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(encoded.toByteArray(Charsets.UTF_8))
          } ?: throw IllegalStateException("Cannot write to chosen file")
        }
        preferencesRepository.setLastBackupEpochMillis(System.currentTimeMillis())
        _events.emit(BackupEvent.ExportSaved(chosen.size))
      } catch (_: Exception) {
        _events.emit(BackupEvent.ErrorMessage("Could not save .remind file."))
      } finally {
        draft.update { it.copy(isBusy = false) }
      }
    }
  }

  fun loadImportUri(uri: Uri) {
    viewModelScope.launch {
      draft.update {
        it.copy(
          activeTab = BackupTab.IMPORT,
          isBusy = true,
          importError = null,
          decryptError = null,
          pendingEnvelope = null,
          importPreview = null,
        )
      }
      try {
        val (sourceName, rawText) =
          withContext(Dispatchers.IO) {
            val displayName = resolveDisplayName(appContext, uri) ?: "backup.remind"
            val text =
              appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: throw IllegalStateException("Cannot read file")
            displayName to text
          }

        when (val result = RemindBackupCodec.parse(rawText)) {
          is RemindParseResult.InvalidFile -> {
            draft.update { it.copy(importError = result.message, isBusy = false) }
          }
          is RemindParseResult.PasswordRequired -> {
            draft.update {
              it.copy(
                pendingEnvelope = result.envelope,
                pendingSourceName = sourceName,
                decryptPasswordInput = "",
                decryptError = null,
                isBusy = false,
              )
            }
          }
          is RemindParseResult.Success -> {
            val preview = buildPreview(sourceName, result)
            draft.update {
              it.copy(
                importPreview = preview,
                selectedImportIndices = preview.items.indices.toSet(),
                isBusy = false,
              )
            }
          }
          RemindParseResult.WrongPassword -> {
            draft.update { it.copy(importError = "Incorrect password.", isBusy = false) }
          }
        }
      } catch (_: Exception) {
        draft.update {
          it.copy(
            importError = "Unable to open this file. Make sure it is a readable .remind backup.",
            isBusy = false,
          )
        }
      }
    }
  }

  fun onDecryptPasswordChanged(value: String) {
    draft.update { it.copy(decryptPasswordInput = value, decryptError = null) }
  }

  fun unlockEncryptedImport() {
    val state = draft.value
    val envelope = state.pendingEnvelope ?: return
    val password = state.decryptPasswordInput
    viewModelScope.launch {
      draft.update { it.copy(isBusy = true, decryptError = null) }
      val result =
        withContext(Dispatchers.Default) {
          RemindBackupCodec.decrypt(envelope, password)
        }
      when (result) {
        is RemindParseResult.Success -> {
          val preview = buildPreview(state.pendingSourceName, result)
          draft.update {
            it.copy(
              pendingEnvelope = null,
              decryptPasswordInput = "",
              decryptError = null,
              importPreview = preview,
              selectedImportIndices = preview.items.indices.toSet(),
              isBusy = false,
            )
          }
        }
        RemindParseResult.WrongPassword -> {
          draft.update {
            it.copy(decryptError = "Wrong password. Please try again.", isBusy = false)
          }
        }
        is RemindParseResult.InvalidFile -> {
          draft.update {
            it.copy(
              pendingEnvelope = null,
              importError = result.message,
              isBusy = false,
            )
          }
        }
        is RemindParseResult.PasswordRequired -> {
          draft.update { it.copy(isBusy = false) }
        }
      }
    }
  }

  fun dismissEncryptedPrompt() {
    draft.update {
      it.copy(pendingEnvelope = null, decryptPasswordInput = "", decryptError = null)
    }
  }

  fun setDuplicatePolicy(policy: ImportDuplicatePolicy) {
    draft.update { it.copy(duplicatePolicy = policy) }
  }

  fun toggleImportItemSelection(index: Int) {
    draft.update { current ->
      val next =
        if (index in current.selectedImportIndices) current.selectedImportIndices - index
        else current.selectedImportIndices + index
      current.copy(selectedImportIndices = next)
    }
  }

  fun confirmImport() {
    val preview = draft.value.importPreview ?: return
    val selectedIndices = draft.value.selectedImportIndices
    val incoming =
      preview.items.mapIndexedNotNull { index, item ->
        if (index in selectedIndices) item.reminder else null
      }
    if (incoming.isEmpty()) return
    val policy = draft.value.duplicatePolicy
    viewModelScope.launch {
      draft.update { it.copy(isBusy = true) }
      try {
        val summary =
          reminderRepository.importReminders(
            incomingReminders = incoming,
            duplicatePolicy = policy,
          )
        draft.update {
          it.copy(
            importPreview = null,
            selectedImportIndices = emptySet(),
            importError = null,
            isBusy = false,
          )
        }
        _events.emit(BackupEvent.ImportCompleted(summary))
      } catch (_: Exception) {
        draft.update { it.copy(isBusy = false) }
        _events.emit(BackupEvent.ErrorMessage("Import failed. Please try again."))
      }
    }
  }

  fun clearImportPreview() {
    draft.update {
      it.copy(importPreview = null, selectedImportIndices = emptySet(), importError = null)
    }
  }

  private suspend fun buildPreview(
    sourceName: String,
    result: RemindParseResult.Success,
  ): LoadedImportPreview {
    val existing = reminderRepository.getAllNonDeletedRemindersOnce()
    val items =
      result.payload.reminders.map { incoming ->
        val dup = reminderRepository.findMatchingExistingReminder(incoming, existing) != null
        ImportPreviewItem(reminder = incoming, isDuplicate = dup)
      }
    return LoadedImportPreview(
      sourceName = sourceName,
      exportedAtMillis = result.payload.exportedAtMillis,
      wasEncrypted = result.wasEncrypted,
      items = items,
    )
  }

  private fun resolveDisplayName(context: Context, uri: Uri): String? {
    runCatching {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIdx >= 0 && cursor.moveToFirst()) {
          return cursor.getString(nameIdx)
        }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }

  companion object {
    fun factory(
      context: Context,
      reminderRepository: ReminderRepository,
      preferencesRepository: PreferencesRepository,
      scheduler: ReminderScheduler,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          BackupShareViewModel(
            appContext = context.applicationContext,
            reminderRepository = reminderRepository,
            preferencesRepository = preferencesRepository,
            scheduler = scheduler,
          ) as T
      }
  }
}
