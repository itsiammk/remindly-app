package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.OffsetType
import com.example.domain.model.ReminderCategory
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.Weekday
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.OffsetCalculator
import com.example.ui.components.IstDatePickerDialog
import com.example.ui.components.IstTimePickerDialog
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryLabel
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
import kotlinx.coroutines.delay

@Composable
fun ReminderEditorScreen(
  viewModel: ReminderEditorViewModel,
  onNavigateBack: () -> Unit,
  onDeleted: (Long, String) -> Unit,
) {
  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      ReminderEditorSheetContent(
        viewModel = viewModel,
        isBottomSheet = false,
        onDismiss = onNavigateBack,
        onDeleted = onDeleted,
      )
    }
  }
}

@Composable
fun ReminderEditorSheetOverlay(
  viewModel: ReminderEditorViewModel,
  onDismiss: () -> Unit,
  onDeleted: (Long, String) -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  BackHandler(onBack = onDismiss)

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black.copy(alpha = if (palette.isDark) 0.56f else 0.34f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss,
        ),
    contentAlignment = Alignment.BottomCenter,
  ) {
    GlassSurface(
      level = GlassLevel.BAR,
      shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 680.dp)
          .fillMaxHeight(0.93f)
          .imePadding()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          ),
    ) {
      ReminderEditorSheetContent(
        viewModel = viewModel,
        isBottomSheet = true,
        onDismiss = onDismiss,
        onDeleted = onDeleted,
      )
    }
  }
}

@Composable
fun ReminderEditorSheetContent(
  viewModel: ReminderEditorViewModel,
  isBottomSheet: Boolean,
  onDismiss: () -> Unit,
  onDeleted: (Long, String) -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var showCustomOffsetDialog by remember { mutableStateOf(false) }
  var showMoveOccDatePicker by remember { mutableStateOf(false) }
  var moveOccPendingDate by remember { mutableStateOf<IstDate?>(null) }
  var savedConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        EditorEvent.Saved -> {
          savedConfirmation = true
          delay(220)
          onDismiss()
        }
        is EditorEvent.Deleted -> onDeleted(event.reminderId, event.title)
      }
    }
  }

  Column(
    modifier = Modifier.fillMaxSize().widthIn(max = 660.dp),
  ) {
    if (isBottomSheet) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.width(42.dp)
              .height(4.dp)
              .clip(CircleShape)
              .background(palette.textSecondary.copy(alpha = 0.35f)),
        )
      }
    }

    // Header Row
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      GlassIconButton(
        icon = if (isBottomSheet) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(R.string.nav_back),
        size = 42.dp,
        onClick = onDismiss,
        modifier = Modifier.testTag("editor_back_button"),
      )

      Text(
        text =
          stringResource(
            if (state.isEditMode) R.string.form_edit_title else R.string.form_new_title
          ),
        style = MaterialTheme.typography.titleLarge,
        color = palette.textPrimary,
      )

      if (state.isEditMode) {
        GlassIconButton(
          icon = Icons.Outlined.DeleteOutline,
          contentDescription = stringResource(R.string.action_delete),
          tint = palette.dangerColor,
          size = 42.dp,
          onClick = { showDeleteConfirm = true },
          modifier = Modifier.testTag("editor_delete_button"),
        )
      } else {
        Spacer(modifier = Modifier.size(42.dp))
      }
    }

    if (state.isLoading) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = palette.primaryAccent)
      }
    } else {
      val nowMillis = remember(state) { System.currentTimeMillis() }
      val previewOccurrenceMillis =
        remember(state, nowMillis) { state.previewOccurrenceMillis(nowMillis) }
      val plannedFires = remember(state, nowMillis) { state.previewPlannedFires(nowMillis) }

      Column(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // Large borderless Title field with animated underline
        val titleInteraction = remember { MutableInteractionSource() }
        val titleFocused by titleInteraction.collectIsFocusedAsState()
        val underlineColor by
          animateColorAsState(
            targetValue =
              when {
                state.titleError != null -> palette.dangerColor
                titleFocused -> palette.primaryAccent
                else -> palette.textSecondary.copy(alpha = 0.25f)
              },
            animationSpec = tween(180),
            label = "title_underline_color",
          )
        val underlineHeight by
          animateDpAsState(
            targetValue = if (titleFocused || state.titleError != null) 2.dp else 1.dp,
            label = "title_underline_height",
          )

        Column(modifier = Modifier.fillMaxWidth()) {
          Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            if (state.title.isEmpty()) {
              Text(
                text = stringResource(R.string.field_title_placeholder),
                style =
                  MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                color = palette.textSecondary.copy(alpha = 0.45f),
              )
            }
            BasicTextField(
              value = state.title,
              onValueChange = viewModel::onTitleChanged,
              singleLine = true,
              interactionSource = titleInteraction,
              textStyle =
                MaterialTheme.typography.headlineSmall.copy(
                  fontSize = 22.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.textPrimary,
                ),
              cursorBrush = SolidColor(palette.primaryAccent),
              modifier = Modifier.fillMaxWidth().testTag("title_input"),
            )
          }
          Box(
            modifier = Modifier.fillMaxWidth().height(underlineHeight).background(underlineColor),
          )
          if (state.titleError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = state.titleError.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = palette.dangerColor,
            )
          }
        }

        // Category Horizontal Row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.field_category_label).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary.copy(alpha = 0.72f),
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ReminderCategory.entries.forEach { cat ->
              val selected = state.category == cat
              val catStyle = categoryStyle(cat)
              GlassSurface(
                level = if (selected) GlassLevel.BAR else GlassLevel.CARD,
                shape = CircleShape,
                tintOverlay = if (selected) catStyle.contentColor else null,
                accentBorder = if (selected) catStyle.contentColor.copy(alpha = 0.65f) else null,
                onClick = { viewModel.onCategorySelected(cat) },
                modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                  Icon(
                    imageVector = categoryIcon(cat),
                    contentDescription = null,
                    tint = if (selected) catStyle.contentColor else palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = categoryLabel(cat),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) palette.textPrimary else palette.textSecondary,
                  )
                }
              }
            }
          }
        }

        // Date & Time Side-by-Side Glass Tiles
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            GlassSurface(
              level = GlassLevel.CARD,
              shape = RoundedCornerShape(22.dp),
              onClick = { showDatePicker = true },
              modifier = Modifier.weight(1f).testTag("date_picker_row"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(17.dp),
                  )
                  Text(
                    text = stringResource(R.string.field_date_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
                Text(
                  text = state.date.formatWithWeekday(),
                  style = MaterialTheme.typography.titleMedium.tabular(),
                  color = palette.textPrimary,
                )
              }
            }

            GlassSurface(
              level = GlassLevel.CARD,
              shape = RoundedCornerShape(22.dp),
              tintOverlay = if (state.hasExplicitTime) palette.primaryAccent else null,
              onClick = {
                if (state.hasExplicitTime) {
                  showTimePicker = true
                } else {
                  viewModel.onExplicitTimeToggled(true)
                  showTimePicker = true
                }
              },
              modifier = Modifier.weight(1f).testTag("time_picker_row"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(17.dp),
                  )
                  Text(
                    text = stringResource(R.string.field_time_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
                Text(
                  text =
                    if (state.hasExplicitTime) "${state.time.format12Hour()} IST"
                    else "${state.defaultTime.format12Hour()} (Auto)",
                  style = MaterialTheme.typography.titleMedium.tabular(),
                  color = palette.textPrimary,
                )
              }
            }
          }

          GlassSurface(
            level = GlassLevel.CARD,
            shape = RoundedCornerShape(22.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    text = stringResource(R.string.field_specific_time_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text =
                      if (state.hasExplicitTime) "Fires at chosen IST time"
                      else "Date-only • uses ${state.defaultTime.format12Hour()} IST",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
                GlassSwitch(
                  checked = state.hasExplicitTime,
                  onCheckedChange = viewModel::onExplicitTimeToggled,
                  modifier = Modifier.testTag("specific_time_switch"),
                )
              }

              AnimatedVisibility(
                visible =
                  state.category == ReminderCategory.BIRTHDAY ||
                    state.category == ReminderCategory.ANNIVERSARY,
              ) {
                Column {
                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(1.dp)
                        .background(palette.glassBorderBottom),
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                      Text(
                        text = "Year known",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                      )
                      Text(
                        text =
                          if (state.category == ReminderCategory.BIRTHDAY)
                            "Shows age on upcoming birthday"
                          else "Shows years completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary.copy(alpha = 0.75f),
                      )
                    }
                    GlassSwitch(
                      checked = state.yearKnown,
                      onCheckedChange = viewModel::onYearKnownChanged,
                      modifier = Modifier.testTag("year_known_switch"),
                    )
                  }
                }
              }
            }
          }
        }

        // Repeat Rule Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.field_repeat_label).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary.copy(alpha = 0.72f),
          )

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            RepeatRuleType.entries.forEach { rule ->
              val selected = state.repeatRule == rule
              GlassSurface(
                level = if (selected) GlassLevel.BAR else GlassLevel.CARD,
                shape = CircleShape,
                tintOverlay = if (selected) palette.primaryAccent else null,
                accentBorder = if (selected) palette.primaryAccent.copy(alpha = 0.6f) else null,
                onClick = { viewModel.onRepeatRuleSelected(rule) },
                modifier = Modifier.testTag("repeat_chip_${rule.name.lowercase()}"),
              ) {
                Text(
                  text = repeatRuleLabel(rule),
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) palette.primaryAccent else palette.textPrimary,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
              }
            }
          }

          AnimatedVisibility(
            visible = state.repeatRule == RepeatRuleType.WEEKLY,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            GlassSurface(level = GlassLevel.CARD) {
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                  text = "Repeat on",
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.textSecondary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Weekday.entries.forEach { day ->
                    val selected = day in state.selectedWeekdays
                    Box(
                      modifier =
                        Modifier.size(40.dp)
                          .clip(CircleShape)
                          .background(
                            if (selected) palette.primaryAccent
                            else palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.12f else 0.6f)
                          )
                          .border(
                            width = 1.dp,
                            color =
                              if (selected) palette.primaryAccent
                              else palette.glassBorderTop,
                            shape = CircleShape,
                          )
                          .clickable { viewModel.onWeekdayToggled(day) }
                          .testTag("weekday_${day.name.lowercase()}"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = day.shortLabel.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) palette.onPrimaryAccent else palette.textPrimary,
                      )
                    }
                  }
                }
              }
            }
          }

          AnimatedVisibility(
            visible = state.repeatRule == RepeatRuleType.EVERY_N_DAYS,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            GlassSurface(level = GlassLevel.CARD) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Repeat interval",
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  IconButton(
                    onClick = { viewModel.onRepeatIntervalChanged(state.repeatInterval - 1) },
                    enabled = state.repeatInterval > 1,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("interval_minus"),
                  ) {
                    Icon(
                      Icons.Rounded.Remove,
                      contentDescription = "Decrease days",
                      tint = palette.textPrimary,
                    )
                  }
                  Text(
                    text =
                      if (state.repeatInterval == 1) "1 day"
                      else "${state.repeatInterval} days",
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = palette.textPrimary,
                  )
                  IconButton(
                    onClick = { viewModel.onRepeatIntervalChanged(state.repeatInterval + 1) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("interval_plus"),
                  ) {
                    Icon(
                      Icons.Rounded.Add,
                      contentDescription = "Increase days",
                      tint = palette.textPrimary,
                    )
                  }
                }
              }
            }
          }
        }

        // Offsets Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "WHEN TO REMIND YOU",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textSecondary.copy(alpha = 0.72f),
            )
            TextButton(
              onClick = { showCustomOffsetDialog = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("add_custom_offset_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = palette.primaryAccent,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.size(4.dp))
              Text("Add custom", color = palette.primaryAccent)
            }
          }

          GlassSurface(level = GlassLevel.CARD) {
            Column(modifier = Modifier.fillMaxWidth()) {
              state.offsets.forEachIndexed { index, offset ->
                val isCustom = offset.isCustomOffset()
                val label = OffsetCalculator.labelFor(offset)
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable {
                        viewModel.onOffsetEnabledChanged(index, !offset.isEnabled)
                      }
                      .minimumInteractiveComponentSize()
                      .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
                      .testTag("offset_row_$index"),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  AnimatedCheckmark(
                    checked = offset.isEnabled,
                    size = 24.dp,
                  )

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.bodyLarge,
                      color =
                        if (offset.isEnabled) palette.textPrimary
                        else palette.textSecondary.copy(alpha = 0.72f),
                    )
                    if (offset.isFallback8Am()) {
                      Text(
                        text = "Only fires if none of the above alerts fired",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary.copy(alpha = 0.72f),
                      )
                    }
                  }

                  val isAlarm = offset.deliveryChannel == DeliveryChannel.ALARM
                  Box(
                    modifier =
                      Modifier.clip(CircleShape)
                        .background(
                          if (isAlarm) palette.primaryAccent.copy(alpha = 0.16f)
                          else palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.12f else 0.5f)
                        )
                        .border(
                          1.dp,
                          if (isAlarm) palette.primaryAccent.copy(alpha = 0.35f)
                          else palette.glassBorderTop,
                          CircleShape,
                        )
                        .clickable { viewModel.onOffsetChannelToggled(index) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("offset_channel_toggle_$index"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector =
                          if (isAlarm) Icons.Outlined.Alarm
                          else Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = if (isAlarm) palette.primaryAccent else palette.textSecondary,
                        modifier = Modifier.size(14.dp),
                      )
                      Text(
                        text = if (isAlarm) "Alarm" else "Notify",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAlarm) palette.primaryAccent else palette.textSecondary,
                      )
                    }
                  }

                  if (isCustom || offset.isEnabled) {
                    IconButton(
                      onClick = { viewModel.onRemoveOffset(index) },
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("offset_remove_$index"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove offset",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
                if (index < state.offsets.lastIndex) {
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
        }

        // Live Timeline Preview Glass Card
        GlassSurface(
          level = GlassLevel.CARD,
          tintOverlay = palette.primaryAccent,
          accentBorder = palette.primaryAccent.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth().testTag("live_preview_card"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = palette.primaryAccent,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = strings.previewTimelineTitle.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = palette.primaryAccent,
              )
            }

            Text(
              text =
                "${IstDate.fromEpochMillis(previewOccurrenceMillis).formatShort()} • ${IstTime.fromEpochMillis(previewOccurrenceMillis).format12Hour()} IST",
              style = MaterialTheme.typography.titleMedium.tabular(),
              color = palette.textPrimary,
            )

            if (plannedFires.isEmpty()) {
              Text(
                text = "No future alerts remain for this date/time (past times are skipped automatically).",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary.copy(alpha = 0.78f),
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plannedFires.forEachIndexed { idx, fire ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.width(18.dp),
                    ) {
                      Box(
                        modifier =
                          Modifier.size(8.dp)
                            .clip(CircleShape)
                            .background(palette.primaryAccent),
                      )
                      if (idx < plannedFires.lastIndex) {
                        Box(
                          modifier =
                            Modifier.width(1.5.dp)
                              .height(14.dp)
                              .background(palette.primaryAccent.copy(alpha = 0.35f)),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = fire.label,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textPrimary,
                      modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                      text = OffsetCalculator.formatFirePreview(fire),
                      style = MaterialTheme.typography.labelSmall.tabular().copy(fontWeight = FontWeight.SemiBold),
                      color = palette.primaryAccent,
                    )
                  }
                }
              }
            }
          }
        }

        // Borderless Glass Note Container
        GlassSurface(level = GlassLevel.CARD) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = stringResource(R.string.field_note_label).uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = palette.textSecondary.copy(alpha = 0.72f),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
              if (state.note.isEmpty()) {
                Text(
                  text = stringResource(R.string.field_note_placeholder),
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textSecondary.copy(alpha = 0.48f),
                )
              }
              BasicTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChanged,
                minLines = 2,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(palette.primaryAccent),
                modifier = Modifier.fillMaxWidth().testTag("note_input"),
              )
            }
          }
        }

        // Active / Paused Switch (edit mode)
        if (state.isEditMode) {
          GlassSurface(level = GlassLevel.CARD) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  text = stringResource(R.string.field_active_toggle),
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                Text(
                  text =
                    if (state.isActive) "Active and scheduled"
                    else "Paused until you resume it",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary.copy(alpha = 0.75f),
                )
              }
              GlassSwitch(
                checked = state.isActive,
                onCheckedChange = viewModel::onActiveToggled,
                modifier = Modifier.testTag("active_switch"),
              )
            }
          }
        }

        // Single Occurrence Actions
        if (state.isEditMode && state.repeatRule != RepeatRuleType.NONE) {
          GlassSurface(
            level = GlassLevel.CARD,
            modifier = Modifier.fillMaxWidth().testTag("single_occurrence_card"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "THIS OCCURRENCE ONLY",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textSecondary.copy(alpha = 0.78f),
              )
              Text(
                text =
                  if (state.activeOverrideOriginalEpochMillis != null)
                    "Moved once without changing your future repeat schedule."
                  else "Skip or reschedule only the current occurrence without changing the series.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary.copy(alpha = 0.75f),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                GlassPillButton(
                  text = "Skip once",
                  icon = Icons.Outlined.SkipNext,
                  isPrimary = false,
                  onClick = viewModel::skipThisOccurrence,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                  modifier = Modifier.weight(1f).testTag("editor_skip_once_button"),
                )
                GlassPillButton(
                  text = "Move once",
                  icon = Icons.Outlined.EditCalendar,
                  isPrimary = false,
                  onClick = { showMoveOccDatePicker = true },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                  modifier = Modifier.weight(1f).testTag("editor_move_once_button"),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }

    // Pinned Bottom Glowing Save Pill Footer
    GlassSurface(
      level = GlassLevel.BAR,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
      ) {
        val saveLabel =
          stringResource(if (state.isEditMode) R.string.action_update else R.string.action_save)
        GlassPillButton(
          text = if (savedConfirmation) strings.done else saveLabel,
          icon = if (savedConfirmation) Icons.Rounded.Check else null,
          isPrimary = true,
          enabled = !state.isSaving && !state.isLoading && !savedConfirmation,
          onClick = viewModel::save,
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_reminder_button"),
        )
      }
    }
  }

  if (showDatePicker) {
    IstDatePickerDialog(
      initialDate = state.date,
      onDateSelected = viewModel::onDateSelected,
      onDismiss = { showDatePicker = false },
    )
  }

  if (showTimePicker) {
    IstTimePickerDialog(
      initialTime = state.time,
      onTimeSelected = viewModel::onTimeSelected,
      onDismiss = { showTimePicker = false },
    )
  }

  if (showMoveOccDatePicker) {
    IstDatePickerDialog(
      initialDate = state.date,
      onDateSelected = { pickedDate ->
        showMoveOccDatePicker = false
        moveOccPendingDate = pickedDate
      },
      onDismiss = { showMoveOccDatePicker = false },
    )
  }

  val pendingMoveDate = moveOccPendingDate
  if (pendingMoveDate != null) {
    IstTimePickerDialog(
      initialTime = if (state.hasExplicitTime) state.time else state.defaultTime,
      title = "Move occurrence time (IST)",
      onTimeSelected = { pickedTime ->
        viewModel.moveThisOccurrence(pendingMoveDate, pickedTime)
        moveOccPendingDate = null
      },
      onDismiss = { moveOccPendingDate = null },
    )
  }

  if (showCustomOffsetDialog) {
    AddCustomOffsetDialog(
      defaultDate = state.date,
      defaultChannel = state.prefs.defaultDeliveryChannel,
      onAddRelative = { type, value, channel ->
        viewModel.onAddCustomRelativeOffset(type, value, channel)
        showCustomOffsetDialog = false
      },
      onAddExact = { date, time, channel ->
        viewModel.onAddCustomExactOffset(date, time, channel)
        showCustomOffsetDialog = false
      },
      onDismiss = { showCustomOffsetDialog = false },
    )
  }

  if (showDeleteConfirm) {
    Dialog(onDismissRequest = { showDeleteConfirm = false }) {
      GlassSurface(
        level = GlassLevel.DIALOG,
        shape = RoundedCornerShape(28.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "Delete reminder?",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Text(
            text = "\"${state.title}\" will be removed. You can undo right after.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            GlassPillButton(
              text = "Cancel",
              isPrimary = false,
              onClick = { showDeleteConfirm = false },
              modifier = Modifier.weight(1f),
            )
            GlassPillButton(
              text = "Delete",
              isPrimary = true,
              onClick = {
                showDeleteConfirm = false
                viewModel.delete()
              },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AddCustomOffsetDialog(
  defaultDate: IstDate,
  defaultChannel: DeliveryChannel,
  onAddRelative: (OffsetType, Int, DeliveryChannel) -> Unit,
  onAddExact: (IstDate, IstTime, DeliveryChannel) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  var isExactMode by remember { mutableStateOf(false) }
  var relativeAmountText by remember { mutableStateOf("30") }
  var relativeUnit by remember { mutableStateOf(OffsetType.MINUTES_BEFORE) }
  var exactDate by remember { mutableStateOf(defaultDate) }
  var exactTime by remember { mutableStateOf(IstTime(9, 0)) }
  var channel by remember { mutableStateOf(defaultChannel) }

  var pickExactDate by remember { mutableStateOf(false) }
  var pickExactTime by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    GlassSurface(
      level = GlassLevel.DIALOG,
      shape = RoundedCornerShape(28.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = "Add custom reminder",
          style = MaterialTheme.typography.titleLarge,
          color = palette.textPrimary,
        )

        // Before vs Exact segmented glass pill
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(CircleShape)
              .background(palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.14f else 0.55f))
              .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          listOf(false to "Before", true to "Exact date").forEach { (exact, title) ->
            val selected = isExactMode == exact
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(CircleShape)
                  .background(if (selected) palette.primaryAccent else Color.Transparent)
                  .clickable { isExactMode = exact }
                  .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) palette.onPrimaryAccent else palette.textPrimary,
              )
            }
          }
        }

        AnimatedContent(
          targetState = isExactMode,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "custom_offset_mode",
        ) { exactMode ->
          if (!exactMode) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              GlassSurface(level = GlassLevel.CARD, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                  Text(
                    text = "AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.72f),
                  )
                  BasicTextField(
                    value = relativeAmountText,
                    onValueChange = { input ->
                      relativeAmountText = input.filter { it.isDigit() }.take(4)
                    },
                    singleLine = true,
                    textStyle =
                      MaterialTheme.typography.titleLarge.tabular().copy(color = palette.textPrimary),
                    cursorBrush = SolidColor(palette.primaryAccent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("custom_offset_amount"),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                listOf(
                  OffsetType.MINUTES_BEFORE to "Minutes",
                  OffsetType.HOURS_BEFORE to "Hours",
                  OffsetType.DAYS_BEFORE to "Days",
                ).forEach { (unitType, label) ->
                  val selected = relativeUnit == unitType
                  Box(
                    modifier =
                      Modifier.weight(1f)
                        .clip(CircleShape)
                        .background(
                          if (selected) palette.primaryAccent.copy(alpha = 0.18f)
                          else palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.10f else 0.50f)
                        )
                        .border(
                          1.dp,
                          if (selected) palette.primaryAccent else palette.glassBorderTop,
                          CircleShape,
                        )
                        .clickable { relativeUnit = unitType }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium,
                      color = if (selected) palette.primaryAccent else palette.textPrimary,
                    )
                  }
                }
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              GlassPillButton(
                text = "Date: ${exactDate.formatShort()} IST",
                isPrimary = false,
                onClick = { pickExactDate = true },
                modifier = Modifier.fillMaxWidth(),
              )
              GlassPillButton(
                text = "Time: ${exactTime.format12Hour()} IST",
                isPrimary = false,
                onClick = { pickExactTime = true },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }

        // Notify vs Alarm segmented pill
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(CircleShape)
              .background(palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.14f else 0.55f))
              .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          listOf(DeliveryChannel.NOTIFICATION to "Notify", DeliveryChannel.ALARM to "Alarm").forEach { (ch, label) ->
            val selected = channel == ch
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(CircleShape)
                  .background(if (selected) palette.primaryAccent else Color.Transparent)
                  .clickable { channel = ch }
                  .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) palette.onPrimaryAccent else palette.textPrimary,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          GlassPillButton(
            text = "Cancel",
            isPrimary = false,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
          )
          GlassPillButton(
            text = "Add",
            isPrimary = true,
            onClick = {
              if (isExactMode) {
                onAddExact(exactDate, exactTime, channel)
              } else {
                val amount = relativeAmountText.toIntOrNull()?.coerceAtLeast(1) ?: 10
                onAddRelative(relativeUnit, amount, channel)
              }
            },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }

  if (pickExactDate) {
    IstDatePickerDialog(
      initialDate = exactDate,
      onDateSelected = { exactDate = it },
      onDismiss = { pickExactDate = false },
    )
  }

  if (pickExactTime) {
    IstTimePickerDialog(
      initialTime = exactTime,
      onTimeSelected = { exactTime = it },
      onDismiss = { pickExactTime = false },
    )
  }
}

private fun repeatRuleLabel(rule: RepeatRuleType): String =
  when (rule) {
    RepeatRuleType.NONE -> "One-time"
    RepeatRuleType.DAILY -> "Daily"
    RepeatRuleType.WEEKLY -> "Weekly"
    RepeatRuleType.MONTHLY -> "Monthly"
    RepeatRuleType.YEARLY -> "Yearly"
    RepeatRuleType.EVERY_N_DAYS -> "Every N days"
  }
