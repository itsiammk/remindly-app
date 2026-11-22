package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.RepeatRuleType
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AnimatedCheckmark
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.tabular

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderCard(
  reminder: Reminder,
  bucket: ReminderBucket,
  nowMillis: Long,
  onClick: () -> Unit,
  onMarkDone: () -> Unit,
  onTogglePause: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  showActionsMenu: Boolean = true,
  isSelectionMode: Boolean = false,
  isSelected: Boolean = false,
  onLongPress: () -> Unit = {},
  onSkipOccurrence: () -> Unit = {},
  onMoveOccurrence: () -> Unit = {},
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val haptic = LocalHapticFeedback.current
  val style = categoryStyle(reminder.category)

  val occurrenceDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
  val occurrenceTime = IstTime.fromEpochMillis(reminder.nextOccurrenceEpochMillis)
  val daysLeft = RecurrenceCalculator.daysLeftLabel(reminder, nowMillis)
  val milestone = RecurrenceCalculator.milestoneLabel(reminder)
  val repeatText = RecurrenceCalculator.repeatSummary(reminder)

  val isMissed = bucket == ReminderBucket.MISSED
  val isPaused = !reminder.isActive

  var menuExpanded by remember { mutableStateOf(false) }

  val subtitleText =
    buildString {
      if (reminder.repeatRule != RepeatRuleType.NONE) {
        append(repeatText)
        append(" • ")
      } else if (bucket != ReminderBucket.TODAY) {
        append(occurrenceDate.formatDayMonth())
        append(" • ")
      }
      append(occurrenceTime.format12Hour())
      if (milestone != null) {
        append(" • ")
        append(milestone)
      }
    }

  val pillText =
    when {
      isPaused -> strings.pausedBadge
      isMissed -> strings.missedBadge
      else -> daysLeft
    }
  val pillBg =
    when {
      isMissed -> palette.dangerColor.copy(alpha = 0.22f)
      bucket == ReminderBucket.TODAY -> palette.primaryAccent.copy(alpha = 0.18f)
      else -> palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.10f else 0.55f)
    }
  val pillFg =
    when {
      isMissed -> palette.dangerColor
      bucket == ReminderBucket.TODAY -> palette.primaryAccent
      else -> palette.textSecondary
    }

  val cardContent: @Composable () -> Unit = {
    GlassSurface(
      level = GlassLevel.CARD,
      shape = RoundedCornerShape(24.dp),
      tintOverlay =
        when {
          isSelected -> palette.primaryAccent
          isMissed -> palette.dangerColor
          else -> null
        },
      accentBorder =
        when {
          isSelected -> palette.primaryAccent
          isMissed -> palette.dangerColor.copy(alpha = 0.55f)
          else -> null
        },
      modifier =
        Modifier.fillMaxWidth()
          .alpha(if (isPaused) 0.58f else 1f)
          .clip(RoundedCornerShape(24.dp))
          .combinedClickable(
            onClick = onClick,
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onLongPress()
            },
          )
          .testTag("reminder_card_${reminder.id}"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, bottom = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Category glass icon bubble (or animated checkmark in selection mode)
        if (isSelectionMode) {
          Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
          ) {
            AnimatedCheckmark(checked = isSelected, size = 26.dp)
          }
        } else {
          Box(
            modifier =
              Modifier.size(44.dp)
                .clip(CircleShape)
                .background(style.containerColor)
                .border(1.dp, style.contentColor.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = style.icon,
              contentDescription = style.label,
              tint = style.contentColor,
              modifier = Modifier.size(21.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Middle: Title (17 sp SemiBold) + 1-line subtitle
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = reminder.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector =
                if (reminder.primaryDeliveryChannel() == DeliveryChannel.ALARM)
                  Icons.Outlined.Alarm
                else Icons.Outlined.NotificationsNone,
              contentDescription = null,
              tint = palette.textSecondary.copy(alpha = 0.72f),
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = subtitleText,
              style = MaterialTheme.typography.bodySmall.tabular(),
              color = palette.textSecondary.copy(alpha = 0.78f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Days left glass pill + compact actions
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Box(
            modifier =
              Modifier.clip(CircleShape)
                .background(pillBg)
                .border(1.dp, pillFg.copy(alpha = 0.20f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              text = pillText,
              style = MaterialTheme.typography.labelSmall.tabular().copy(fontWeight = FontWeight.SemiBold),
              color = pillFg,
            )
          }

          if (showActionsMenu && !isSelectionMode) {
            IconButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMarkDone()
              },
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .testTag("done_button_${reminder.id}")
                  .semantics { contentDescription = "Mark ${reminder.title} done" },
            ) {
              Box(
                modifier =
                  Modifier.size(28.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent.copy(alpha = 0.14f))
                    .border(1.dp, palette.primaryAccent.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = palette.primaryAccent,
                  modifier = Modifier.size(15.dp),
                )
              }
            }

            Box {
              IconButton(
                onClick = { menuExpanded = true },
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("more_button_${reminder.id}")
                    .semantics { contentDescription = "More actions for ${reminder.title}" },
              ) {
                Icon(
                  imageVector = Icons.Rounded.MoreVert,
                  contentDescription = null,
                  tint = palette.textSecondary.copy(alpha = 0.75f),
                  modifier = Modifier.size(18.dp),
                )
              }

              DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
              ) {
                DropdownMenuItem(
                  text = { Text(strings.edit) },
                  leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onClick()
                  },
                )
                if (reminder.repeatRule != RepeatRuleType.NONE) {
                  DropdownMenuItem(
                    text = { Text(strings.skipOccurrence) },
                    leadingIcon = { Icon(Icons.Outlined.SkipNext, contentDescription = null) },
                    onClick = {
                      menuExpanded = false
                      onSkipOccurrence()
                    },
                  )
                  DropdownMenuItem(
                    text = { Text(strings.moveOccurrence) },
                    leadingIcon = { Icon(Icons.Outlined.EditCalendar, contentDescription = null) },
                    onClick = {
                      menuExpanded = false
                      onMoveOccurrence()
                    },
                  )
                }
                DropdownMenuItem(
                  text = { Text(if (reminder.isActive) strings.pause else strings.resume) },
                  leadingIcon = {
                    Icon(
                      imageVector =
                        if (reminder.isActive) Icons.Outlined.PauseCircle
                        else Icons.Outlined.PlayCircle,
                      contentDescription = null,
                    )
                  },
                  onClick = {
                    menuExpanded = false
                    onTogglePause()
                  },
                )
                DropdownMenuItem(
                  text = {
                    Text(
                      strings.delete,
                      color = palette.dangerColor,
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = null,
                      tint = palette.dangerColor,
                    )
                  },
                  onClick = {
                    menuExpanded = false
                    onDelete()
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  if (!showActionsMenu || isSelectionMode) {
    Box(modifier = modifier) { cardContent() }
    return
  }

  val dismissState =
    rememberSwipeToDismissBoxState(
      confirmValueChange = { value ->
        when (value) {
          SwipeToDismissBoxValue.StartToEnd -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onMarkDone()
            false
          }
          SwipeToDismissBoxValue.EndToStart -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDelete()
            false
          }
          SwipeToDismissBoxValue.Settled -> false
        }
      },
    )

  LaunchedEffect(reminder.id, reminder.nextOccurrenceEpochMillis, reminder.isActive) {
    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
  }

  SwipeToDismissBox(
    state = dismissState,
    modifier = modifier,
    backgroundContent = {
      val direction = dismissState.dismissDirection
      val bgTint by
        animateColorAsState(
          targetValue =
            when (direction) {
              SwipeToDismissBoxValue.StartToEnd -> palette.successColor.copy(alpha = 0.24f)
              SwipeToDismissBoxValue.EndToStart -> palette.dangerColor.copy(alpha = 0.24f)
              SwipeToDismissBoxValue.Settled -> Color.Transparent
            },
          label = "swipe_bg_tint",
        )
      val alignment =
        if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
        else Alignment.CenterEnd

      Box(
        modifier =
          Modifier.fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(bgTint)
            .padding(horizontal = 22.dp),
        contentAlignment = alignment,
      ) {
        if (direction == SwipeToDismissBoxValue.StartToEnd) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = strings.done,
              tint = palette.successColor,
            )
            Text(
              text = strings.done,
              style = MaterialTheme.typography.labelLarge,
              color = palette.successColor,
            )
          }
        } else if (direction == SwipeToDismissBoxValue.EndToStart) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = strings.delete,
              style = MaterialTheme.typography.labelLarge,
              color = palette.dangerColor,
            )
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = strings.delete,
              tint = palette.dangerColor,
            )
          }
        }
      }
    },
    content = { cardContent() },
  )
}
