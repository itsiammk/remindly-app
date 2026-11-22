package com.example.ui.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.ImportDuplicatePolicy
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryStyle
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.AnimatedCheckmark
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSwitch
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.tabular

@Composable
fun BackupShareScreen(
  viewModel: BackupShareViewModel,
  incomingFileUri: Uri?,
  onIncomingFileConsumed: () -> Unit,
  onNavigateBack: () -> Unit,
  onImportedToHome: () -> Unit,
) {
  val context = LocalContext.current
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  val createDocumentLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
      if (uri != null) {
        viewModel.saveExportToUri(uri)
      }
    }

  val openDocumentLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri != null) {
        viewModel.loadImportUri(uri)
      }
    }

  LaunchedEffect(incomingFileUri) {
    if (incomingFileUri != null) {
      viewModel.loadImportUri(incomingFileUri)
      onIncomingFileConsumed()
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is BackupEvent.ShareUriReady -> {
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "application/octet-stream"
              putExtra(Intent.EXTRA_STREAM, event.uri)
              putExtra(
                Intent.EXTRA_SUBJECT,
                "Remindly backup (${event.count} reminder${if (event.count == 1) "" else "s"})",
              )
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          context.startActivity(Intent.createChooser(shareIntent, "Share .remind file"))
        }
        is BackupEvent.ExportSaved -> {
          snackbarHostState.showSnackbar(
            "Saved ${event.count} reminder${if (event.count == 1) "" else "s"} to .remind file"
          )
        }
        is BackupEvent.ImportCompleted -> {
          val s = event.summary
          val parts = buildList {
            if (s.importedCount > 0) add("${s.importedCount} added")
            if (s.replacedCount > 0) add("${s.replacedCount} updated")
            if (s.skippedCount > 0) add("${s.skippedCount} skipped")
            if (s.missedCount > 0) add("${s.missedCount} marked missed")
          }
          snackbarHostState.showSnackbar(
            if (parts.isEmpty()) "No changes imported"
            else "Import complete: ${parts.joinToString(" • ")}"
          )
          if (s.importedCount + s.replacedCount > 0) {
            onImportedToHome()
          }
        }
        is BackupEvent.ErrorMessage -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }

  val total = state.exportableReminders.size
  val selectedCount = state.selectedIds.size
  val allSelected = total > 0 && selectedCount == total
  val canExport =
    selectedCount > 0 && !state.isBusy && (!state.usePassword || state.exportPassword.length >= 4)

  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
      ) {
        // Frosted Glass Top Selection / Navigation Bar
        GlassSurface(
          level = GlassLevel.BAR,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                size = 40.dp,
                onClick = onNavigateBack,
                modifier = Modifier.testTag("backup_back_button"),
              )
              Column {
                Text(
                  text = "Backup & Share",
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                )
                if (state.activeTab == BackupTab.EXPORT && total > 0) {
                  Text(
                    text = "$selectedCount of $total ${strings.selectedCountLabel}",
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = palette.primaryAccent,
                  )
                }
              }
            }

            if (state.activeTab == BackupTab.EXPORT && total > 0) {
              TextButton(
                onClick = viewModel::toggleSelectAll,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("backup_select_all_button"),
              ) {
                Text(
                  text = if (allSelected) "Deselect all" else "Select all",
                  color = palette.primaryAccent,
                )
              }
            }
          }
        }

        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = if (state.activeTab == BackupTab.EXPORT && total > 0) 104.dp else 24.dp,
              ),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Segmented Glass Pill Switcher: Export vs Import
          GlassSurface(
            level = GlassLevel.CARD,
            shape = CircleShape,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              listOf(
                Triple(BackupTab.EXPORT, "Export / Share", "backup_tab_export") to Icons.Outlined.Upload,
                Triple(BackupTab.IMPORT, "Import", "backup_tab_import") to Icons.Outlined.Download,
              ).forEach { (meta, icon) ->
                val (tab, label, tag) = meta
                val selected = state.activeTab == tab
                Row(
                  modifier =
                    Modifier.weight(1f)
                      .clip(CircleShape)
                      .background(if (selected) palette.primaryAccent else Color.Transparent)
                      .clickable { viewModel.selectTab(tab) }
                      .minimumInteractiveComponentSize()
                      .padding(horizontal = 12.dp, vertical = 10.dp)
                      .testTag(tag),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) palette.onPrimaryAccent else palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) palette.onPrimaryAccent else palette.textPrimary,
                  )
                }
              }
            }
          }

          // Weekly Backup Reminder Glass Card
          GlassSurface(level = GlassLevel.CARD) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  text = "Weekly backup reminder",
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                val lastBackupText =
                  state.preferences.lastBackupEpochMillis?.let { ts ->
                    "Last backup: ${IstDate.fromEpochMillis(ts).formatShort()}"
                  } ?: "Reminds Sundays at 10:00 AM IST if not backed up recently"
                Text(
                  text = lastBackupText,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary.copy(alpha = 0.75f),
                )
              }
              GlassSwitch(
                checked = state.preferences.weeklyBackupReminderEnabled,
                onCheckedChange = viewModel::setWeeklyBackupEnabled,
                modifier = Modifier.testTag("weekly_backup_switch"),
              )
            }
          }

          if (state.isBusy) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = palette.primaryAccent)
            }
          }

          when (state.activeTab) {
            BackupTab.EXPORT -> ExportSection(
              state = state,
              onToggleItem = viewModel::toggleReminderSelection,
              onTogglePassword = viewModel::setUsePassword,
              onPasswordChanged = viewModel::setExportPassword,
            )

            BackupTab.IMPORT -> ImportSection(
              state = state,
              onPickFile = { openDocumentLauncher.launch(arrayOf("*/*")) },
              onToggleImportItem = viewModel::toggleImportItemSelection,
              onSelectDuplicatePolicy = viewModel::setDuplicatePolicy,
              onConfirmImport = viewModel::confirmImport,
              onClearPreview = viewModel::clearImportPreview,
            )
          }
        }
      }

      // Floating Level 2 Glass Export Bar (Save File & Share)
      if (state.activeTab == BackupTab.EXPORT && total > 0) {
        Box(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 20.dp, vertical = 12.dp),
          contentAlignment = Alignment.Center,
        ) {
          GlassSurface(
            level = GlassLevel.BAR,
            shape = CircleShape,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              GlassPillButton(
                text = "Save file",
                icon = Icons.Outlined.SaveAlt,
                isPrimary = false,
                enabled = canExport,
                onClick = { createDocumentLauncher.launch(viewModel.suggestedFileName()) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f).testTag("export_save_file_button"),
              )
              GlassPillButton(
                text = "Share",
                icon = Icons.Outlined.Share,
                isPrimary = true,
                enabled = canExport,
                onClick = viewModel::shareSelectedReminders,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f).testTag("export_share_button"),
              )
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 94.dp),
      )
    }
  }

  if (state.pendingEnvelope != null) {
    Dialog(onDismissRequest = viewModel::dismissEncryptedPrompt) {
      GlassSurface(level = GlassLevel.DIALOG, shape = RoundedCornerShape(28.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Password protected backup",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Text(
            text = "Enter the password used when \"${state.pendingSourceName}\" was created.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
          GlassSurface(level = GlassLevel.CARD, shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
              Text(
                text = "BACKUP PASSWORD",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary.copy(alpha = 0.72f),
              )
              BasicTextField(
                value = state.decryptPasswordInput,
                onValueChange = viewModel::onDecryptPasswordChanged,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(palette.primaryAccent),
                modifier =
                  Modifier.fillMaxWidth().padding(top = 4.dp).testTag("import_decrypt_password_input"),
              )
            }
          }
          if (state.decryptError != null) {
            Text(
              text = state.decryptError.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = palette.dangerColor,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GlassPillButton(
              text = "Cancel",
              isPrimary = false,
              onClick = viewModel::dismissEncryptedPrompt,
              modifier = Modifier.weight(1f),
            )
            GlassPillButton(
              text = "Unlock",
              isPrimary = true,
              onClick = viewModel::unlockEncryptedImport,
              modifier = Modifier.weight(1f).testTag("import_unlock_button"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ExportSection(
  state: BackupShareUiState,
  onToggleItem: (Long) -> Unit,
  onTogglePassword: (Boolean) -> Unit,
  onPasswordChanged: (String) -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val total = state.exportableReminders.size

  if (total == 0) {
    GlassSurface(level = GlassLevel.CARD, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "No active reminders to export yet. Create a reminder first, or switch to Import to restore a .remind file.",
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textSecondary,
        modifier = Modifier.padding(20.dp),
      )
    }
    return
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    GlassSurface(level = GlassLevel.CARD) {
      Column(modifier = Modifier.fillMaxWidth()) {
        state.exportableReminders.forEachIndexed { index, reminder ->
          val checked = reminder.id in state.selectedIds
          val style = categoryStyle(reminder.category)
          val date = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
          val time = IstTime.fromEpochMillis(reminder.nextOccurrenceEpochMillis)

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onToggleItem(reminder.id) }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("export_row_${reminder.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            AnimatedCheckmark(checked = checked, size = 22.dp)
            Icon(
              imageVector = categoryIcon(reminder.category),
              contentDescription = null,
              tint = style.contentColor,
              modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${date.formatShort()} • ${time.format12Hour()} • ${RecurrenceCalculator.repeatSummary(reminder)}",
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = palette.textSecondary.copy(alpha = 0.75f),
              )
            }
          }
          if (index < state.exportableReminders.lastIndex) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 16.dp)
                  .height(1.dp)
                  .background(palette.glassBorderBottom)
            )
          }
        }
      }
    }

    // Optional AES-256 Password Protection
    GlassSurface(level = GlassLevel.CARD) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = null,
              tint = palette.primaryAccent,
              modifier = Modifier.size(20.dp),
            )
            Column {
              Text(
                text = "Password protect backup",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textPrimary,
              )
              Text(
                text = "Encrypts your .remind file with AES-256",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary.copy(alpha = 0.75f),
              )
            }
          }
          GlassSwitch(
            checked = state.usePassword,
            onCheckedChange = onTogglePassword,
            modifier = Modifier.testTag("export_password_switch"),
          )
        }

        AnimatedVisibility(visible = state.usePassword) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassSurface(level = GlassLevel.BAR, shape = RoundedCornerShape(16.dp)) {
              Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                  text = "PASSWORD (MIN 4 CHARACTERS)",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textSecondary.copy(alpha = 0.72f),
                )
                BasicTextField(
                  value = state.exportPassword,
                  onValueChange = onPasswordChanged,
                  singleLine = true,
                  visualTransformation = PasswordVisualTransformation(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                  textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                  cursorBrush = SolidColor(palette.primaryAccent),
                  modifier =
                    Modifier.fillMaxWidth().padding(top = 4.dp).testTag("export_password_input"),
                )
              }
            }
            Text(
              text = "Keep this password safe — locked backups cannot be recovered without it.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary.copy(alpha = 0.72f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ImportSection(
  state: BackupShareUiState,
  onPickFile: () -> Unit,
  onToggleImportItem: (Int) -> Unit,
  onSelectDuplicatePolicy: (ImportDuplicatePolicy) -> Unit,
  onConfirmImport: () -> Unit,
  onClearPreview: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    GlassSurface(level = GlassLevel.CARD, modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Restore or add from a .remind file",
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
        Text(
          text =
            "Pick a .remind file saved on your device, or tap a .remind file shared with you in WhatsApp or Files to preview and import.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary.copy(alpha = 0.78f),
        )
        GlassPillButton(
          text = "Choose .remind file",
          icon = Icons.Outlined.FileOpen,
          isPrimary = true,
          onClick = onPickFile,
          modifier = Modifier.fillMaxWidth().testTag("import_pick_file_button"),
        )
      }
    }

    if (state.importError != null) {
      GlassSurface(
        level = GlassLevel.CARD,
        tintOverlay = palette.dangerColor,
        accentBorder = palette.dangerColor.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth().testTag("import_error_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = palette.dangerColor,
          )
          Text(
            text = state.importError,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
          )
        }
      }
    }

    val preview = state.importPreview
    if (preview != null) {
      val selectedImportCount = state.selectedImportIndices.size
      GlassSurface(
        level = GlassLevel.DIALOG,
        tintOverlay = palette.primaryAccent,
        accentBorder = palette.primaryAccent.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth().testTag("import_preview_card"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = palette.primaryAccent,
                modifier = Modifier.size(20.dp),
              )
              Column {
                Text(
                  text = "${preview.items.size} reminder${if (preview.items.size == 1) "" else "s"} ready",
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                )
                val exportedDate = IstDate.fromEpochMillis(preview.exportedAtMillis).formatShort()
                val encBadge = if (preview.wasEncrypted) " • Encrypted" else ""
                Text(
                  text = "${preview.sourceName} • $exportedDate$encBadge",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary.copy(alpha = 0.78f),
                )
              }
            }
            TextButton(onClick = onClearPreview) {
              Text("Clear", color = palette.primaryAccent)
            }
          }

          preview.items.forEachIndexed { index, item ->
            val r = item.reminder
            val date =
              if (r.nextOccurrenceEpochMillis > 0L)
                IstDate.fromEpochMillis(r.nextOccurrenceEpochMillis).formatShort()
              else IstDate(r.anchorYear, r.anchorMonth, r.anchorDay).formatShort()
            val time = IstTime(r.hour, r.minute).format12Hour()
            val checked = index in state.selectedImportIndices

            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onToggleImportItem(index) }
                  .padding(vertical = 6.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              AnimatedCheckmark(checked = checked, size = 20.dp)
              Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                  text = r.title,
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "$date • $time IST • ${RecurrenceCalculator.repeatSummary(r)}",
                  style = MaterialTheme.typography.bodySmall.tabular(),
                  color = palette.textSecondary.copy(alpha = 0.75f),
                )
              }
              if (item.isDuplicate) {
                Box(
                  modifier =
                    Modifier.clip(CircleShape)
                      .background(palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.14f else 0.55f))
                      .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                  Text(
                    text = "Exists",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                  )
                }
              }
            }
          }
        }
      }

      if (preview.duplicateCount > 0) {
        GlassSurface(
          level = GlassLevel.CARD,
          modifier = Modifier.fillMaxWidth().testTag("duplicate_policy_card"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "${preview.duplicateCount} matching reminder${if (preview.duplicateCount == 1) "" else "s"} already on phone",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textPrimary,
              modifier = Modifier.padding(bottom = 4.dp),
            )
            ImportDuplicatePolicy.entries.forEach { policy ->
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectDuplicatePolicy(policy) }
                    .minimumInteractiveComponentSize()
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("duplicate_policy_${policy.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                RadioButton(
                  selected = state.duplicatePolicy == policy,
                  onClick = null,
                  colors =
                    RadioButtonDefaults.colors(
                      selectedColor = palette.primaryAccent,
                      unselectedColor = palette.textSecondary,
                    ),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = policy.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = policy.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
              }
            }
          }
        }
      }

      GlassPillButton(
        text = "Import $selectedImportCount reminder${if (selectedImportCount == 1) "" else "s"}",
        isPrimary = true,
        enabled = !state.isBusy && selectedImportCount > 0,
        onClick = onConfirmImport,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_import_button"),
      )
    }
  }
}
