package com.example.ui.recyclebin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Reminder
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import com.example.ui.components.categoryStyle
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.tabular
import kotlinx.coroutines.launch

@Composable
fun RecycleBinScreen(
  viewModel: RecycleBinViewModel,
  onNavigateBack: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val deleted by viewModel.deletedReminders.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var confirmEmptyBin by remember { mutableStateOf(false) }
  var pendingPermanentDelete by remember { mutableStateOf<Reminder?>(null) }

  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            GlassIconButton(
              icon = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = strings.back,
              size = 42.dp,
              onClick = onNavigateBack,
              modifier = Modifier.testTag("recycle_bin_back_button"),
            )
            Text(
              text = strings.recycleBinTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
            )
          }

          if (deleted.isNotEmpty()) {
            TextButton(
              onClick = { confirmEmptyBin = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("empty_bin_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = null,
                tint = palette.dangerColor,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(strings.emptyBinAction, color = palette.dangerColor)
            }
          }
        }

        if (deleted.isEmpty()) {
          Column(
            modifier =
              Modifier.fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            GlassSurface(
              level = GlassLevel.CARD,
              shape = CircleShape,
              tintOverlay = palette.primaryAccent,
              modifier = Modifier.size(84.dp),
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = null,
                  tint = palette.primaryAccent,
                  modifier = Modifier.size(36.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = strings.recycleBinEmptyStateTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.recycleBinEmptyStateBody,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary.copy(alpha = 0.78f),
              textAlign = TextAlign.Center,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(deleted, key = { it.id }) { reminder ->
              DeletedReminderCard(
                reminder = reminder,
                onRestore = {
                  viewModel.restoreReminder(reminder.id)
                  scope.launch {
                    snackbarHostState.showSnackbar(
                      message = "${strings.restore}: \"${reminder.title}\"",
                      duration = SnackbarDuration.Short,
                    )
                  }
                },
                onDeleteForever = { pendingPermanentDelete = reminder },
              )
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
      )
    }
  }

  if (confirmEmptyBin) {
    Dialog(onDismissRequest = { confirmEmptyBin = false }) {
      GlassSurface(level = GlassLevel.DIALOG, shape = RoundedCornerShape(28.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = strings.emptyBinConfirmTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Text(
            text = strings.emptyBinConfirmBody,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GlassPillButton(
              text = strings.cancel,
              isPrimary = false,
              onClick = { confirmEmptyBin = false },
              modifier = Modifier.weight(1f),
            )
            GlassPillButton(
              text = strings.emptyBinAction,
              isPrimary = true,
              onClick = {
                viewModel.emptyRecycleBin()
                confirmEmptyBin = false
              },
              modifier = Modifier.weight(1f).testTag("confirm_empty_bin_button"),
            )
          }
        }
      }
    }
  }

  val deleteTarget = pendingPermanentDelete
  if (deleteTarget != null) {
    Dialog(onDismissRequest = { pendingPermanentDelete = null }) {
      GlassSurface(level = GlassLevel.DIALOG, shape = RoundedCornerShape(28.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = strings.permanentDeleteTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Text(
            text = strings.permanentDeleteBody(deleteTarget.title),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GlassPillButton(
              text = strings.cancel,
              isPrimary = false,
              onClick = { pendingPermanentDelete = null },
              modifier = Modifier.weight(1f),
            )
            GlassPillButton(
              text = strings.deleteForever,
              isPrimary = true,
              onClick = {
                viewModel.deletePermanently(deleteTarget.id)
                pendingPermanentDelete = null
              },
              modifier = Modifier.weight(1f).testTag("confirm_delete_forever_button"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeletedReminderCard(
  reminder: Reminder,
  onRestore: () -> Unit,
  onDeleteForever: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val catStyle = categoryStyle(reminder.category)
  val deletedAtMillis = reminder.deletedAtMillis ?: reminder.updatedAtMillis
  val deletedDate = IstDate.fromEpochMillis(deletedAtMillis).formatShort()
  val deletedTime = IstTime.fromEpochMillis(deletedAtMillis).format12Hour()
  val repeatText = RecurrenceCalculator.repeatSummary(reminder)

  GlassSurface(
    level = GlassLevel.CARD,
    modifier = Modifier.fillMaxWidth().testTag("deleted_card_${reminder.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .clip(CircleShape)
              .background(catStyle.containerColor)
              .border(1.dp, catStyle.contentColor.copy(alpha = 0.20f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = catStyle.icon,
            contentDescription = catStyle.label,
            tint = catStyle.contentColor,
            modifier = Modifier.size(20.dp),
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = reminder.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${strings.deletedAtPrefix} $deletedDate • $deletedTime IST • $repeatText",
            style = MaterialTheme.typography.bodySmall.tabular(),
            color = palette.textSecondary.copy(alpha = 0.78f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(
          onClick = onDeleteForever,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_forever_${reminder.id}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteForever,
            contentDescription = strings.deleteForever,
            tint = palette.dangerColor,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        GlassPillButton(
          text = strings.restore,
          icon = Icons.Outlined.RestoreFromTrash,
          isPrimary = false,
          onClick = onRestore,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          modifier = Modifier.testTag("restore_button_${reminder.id}"),
        )
      }
    }
  }
}
