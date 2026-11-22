package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.HistoryEventType
import com.example.domain.model.ReminderHistoryEntry
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.ui.components.categoryStyle
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.tabular

@Composable
fun HistoryScreen(
  viewModel: HistoryViewModel,
  onNavigateBack: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showClearConfirm by remember { mutableStateOf(false) }

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
              modifier = Modifier.testTag("history_back_button"),
            )
            Text(
              text = strings.historyTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (uiState.totalCount > 0) {
            GlassIconButton(
              icon = Icons.Outlined.DeleteSweep,
              contentDescription = strings.clearHistory,
              tint = palette.dangerColor,
              size = 42.dp,
              onClick = { showClearConfirm = true },
              modifier = Modifier.testTag("history_clear_button"),
            )
          }
        }

        if (uiState.totalCount > 0) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            HistoryFilter.entries.forEach { filter ->
              val label =
                when (filter) {
                  HistoryFilter.ALL -> strings.filterAll
                  HistoryFilter.ALERTS -> "${strings.notifyLabel} / ${strings.alarmLabel}"
                  HistoryFilter.COMPLETED -> strings.done
                  HistoryFilter.MISSED -> strings.missedBadge
                }
              val selected = uiState.filter == filter
              GlassSurface(
                level = if (selected) GlassLevel.BAR else GlassLevel.CARD,
                shape = CircleShape,
                tintOverlay = if (selected) palette.primaryAccent else null,
                accentBorder = if (selected) palette.primaryAccent.copy(alpha = 0.55f) else null,
                onClick = { viewModel.setFilter(filter) },
                modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}"),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) palette.primaryAccent else palette.textPrimary,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
              }
            }
          }
        }

        if (uiState.entries.isEmpty()) {
          Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
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
                  imageVector = Icons.Outlined.History,
                  contentDescription = null,
                  tint = palette.primaryAccent,
                  modifier = Modifier.size(36.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = strings.historyEmptyTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.historyEmptySubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary.copy(alpha = 0.78f),
              textAlign = TextAlign.Center,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(uiState.entries, key = { it.id }) { entry ->
              HistoryRowCard(entry = entry)
            }
          }
        }
      }
    }
  }

  if (showClearConfirm) {
    Dialog(onDismissRequest = { showClearConfirm = false }) {
      GlassSurface(level = GlassLevel.DIALOG, shape = RoundedCornerShape(28.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = strings.clearHistoryConfirmTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Text(
            text = strings.clearHistoryConfirmBody,
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
              onClick = { showClearConfirm = false },
              modifier = Modifier.weight(1f),
            )
            GlassPillButton(
              text = strings.clearHistory,
              isPrimary = true,
              onClick = {
                viewModel.clearHistory()
                showClearConfirm = false
              },
              modifier = Modifier.weight(1f).testTag("confirm_clear_history_button"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryRowCard(entry: ReminderHistoryEntry) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val catStyle = categoryStyle(entry.category)
  val eventDate = IstDate.fromEpochMillis(entry.eventEpochMillis).formatShort()
  val eventTime = IstTime.fromEpochMillis(entry.eventEpochMillis).format12Hour()

  val eventIcon =
    when (entry.eventType) {
      HistoryEventType.FIRED_NOTIFICATION -> Icons.Outlined.NotificationsActive
      HistoryEventType.FIRED_ALARM -> Icons.Outlined.Alarm
      HistoryEventType.COMPLETED -> Icons.Outlined.CheckCircle
      HistoryEventType.SNOOZED -> Icons.Outlined.Snooze
      HistoryEventType.SKIPPED -> Icons.Outlined.SkipNext
      HistoryEventType.MOVED -> Icons.Outlined.EditCalendar
      HistoryEventType.MISSED -> Icons.Outlined.ErrorOutline
    }

  val isMissed = entry.eventType == HistoryEventType.MISSED
  val isCompleted = entry.eventType == HistoryEventType.COMPLETED
  val badgeFg =
    when {
      isMissed -> palette.dangerColor
      isCompleted -> palette.successColor
      else -> palette.primaryAccent
    }
  val badgeBg = badgeFg.copy(alpha = 0.16f)

  GlassSurface(
    level = GlassLevel.CARD,
    tintOverlay = if (isMissed) palette.dangerColor else null,
    accentBorder = if (isMissed) palette.dangerColor.copy(alpha = 0.45f) else null,
    modifier = Modifier.fillMaxWidth().testTag("history_item_${entry.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
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

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = entry.reminderTitle,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            buildString {
              append("$eventDate • $eventTime IST")
              if (entry.offsetLabel.isNotBlank()) {
                append(" • ${entry.offsetLabel}")
              }
            },
          style = MaterialTheme.typography.bodySmall.tabular(),
          color = palette.textSecondary.copy(alpha = 0.78f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Box(
        modifier =
          Modifier.clip(CircleShape)
            .background(badgeBg)
            .border(1.dp, badgeFg.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = eventIcon,
            contentDescription = null,
            tint = badgeFg,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = strings.historyEventLabel(entry.eventType),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = badgeFg,
          )
        }
      }
    }
  }
}
