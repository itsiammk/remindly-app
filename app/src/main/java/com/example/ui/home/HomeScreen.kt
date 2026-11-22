package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.alarm.PermissionAndOemHelper
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderSortOption
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.domain.time.RecurrenceCalculator
import com.example.ui.components.IstDatePickerDialog
import com.example.ui.components.IstTimePickerDialog
import com.example.ui.components.ReminderCard
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryStyle
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.SkeletonReminderPlaceholder
import com.example.ui.theme.tabular
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onAddReminder: () -> Unit,
  onEditReminder: (Long) -> Unit,
  onOpenSettings: () -> Unit,
  onOpenReliabilityCheck: () -> Unit,
  onScrollMinimizedChanged: (Boolean) -> Unit = {},
  showTopSettingsButton: Boolean = true,
  pendingDeletedReminder: Pair<Long, String>? = null,
  onPendingDeletedConsumed: () -> Unit = {},
) {
  val context = LocalContext.current
  val strings = LocalStrings.current
  val palette = LocalRemindlyPalette.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()

  var showSearchBar by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }
  var permissionsOk by remember { mutableStateOf(true) }
  var moveDateTarget by remember { mutableStateOf<Reminder?>(null) }
  var moveTimeTarget by remember { mutableStateOf<Pair<Reminder, IstDate>?>(null) }
  var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

  val isSelectionMode = selectedIds.isNotEmpty()
  BackHandler(enabled = isSelectionMode) {
    selectedIds = emptySet()
  }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.refreshNow()
    permissionsOk = PermissionAndOemHelper.inspectStatus(context).allCoreChecksPassed
  }

  // Track vertical scroll direction to gently minimize the floating bottom bar
  var prevIndex by remember { mutableIntStateOf(0) }
  var prevOffset by remember { mutableIntStateOf(0) }
  LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
      .distinctUntilChanged()
      .collect { (idx, off) ->
        if (idx == 0 && off < 20) {
          onScrollMinimizedChanged(false)
        } else if (idx > prevIndex || (idx == prevIndex && off > prevOffset + 24)) {
          onScrollMinimizedChanged(true)
        } else if (idx < prevIndex || (idx == prevIndex && off < prevOffset - 24)) {
          onScrollMinimizedChanged(false)
        }
        prevIndex = idx
        prevOffset = off
      }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is HomeEvent.DeletedWithUndo -> {
          val result =
            snackbarHostState.showSnackbar(
              message = "Deleted \"${event.title}\"",
              actionLabel = "Undo",
              duration = SnackbarDuration.Short,
            )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete(event.reminderId)
          }
        }
        is HomeEvent.CompletedOneTimeWithUndo -> {
          val result =
            snackbarHostState.showSnackbar(
              message = "Completed \"${event.title}\"",
              actionLabel = "Undo",
              duration = SnackbarDuration.Short,
            )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoCompleteOneTime(event.reminderId)
          }
        }
        is HomeEvent.CompletedRecurring -> {
          snackbarHostState.showSnackbar(
            message = "\"${event.title}\" done • Next: ${event.nextDateFormatted}",
            duration = SnackbarDuration.Short,
          )
        }
        is HomeEvent.SkippedOccurrenceWithUndo -> {
          val result =
            snackbarHostState.showSnackbar(
              message = "Skipped once • Next: ${event.nextDateFormatted}",
              actionLabel = "Undo",
              duration = SnackbarDuration.Short,
            )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoOccurrenceOverride(event.previousState)
          }
        }
        is HomeEvent.MovedOccurrenceWithUndo -> {
          val result =
            snackbarHostState.showSnackbar(
              message = "Moved once to ${event.movedDateTimeFormatted} IST",
              actionLabel = "Undo",
              duration = SnackbarDuration.Short,
            )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoOccurrenceOverride(event.previousState)
          }
        }
      }
    }
  }

  LaunchedEffect(pendingDeletedReminder) {
    val deleted = pendingDeletedReminder ?: return@LaunchedEffect
    onPendingDeletedConsumed()
    val result =
      snackbarHostState.showSnackbar(
        message = "Deleted \"${deleted.second}\"",
        actionLabel = "Undo",
        duration = SnackbarDuration.Short,
      )
    if (result == SnackbarResult.ActionPerformed) {
      viewModel.undoDelete(deleted.first)
    }
  }

  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      val ready = state as? HomeUiState.Ready
      val allReadyItems =
        remember(ready?.sections) { ready?.sections?.flatMap { it.items }.orEmpty() }
      val todayCount =
        remember(ready?.sections) {
          ready?.sections?.firstOrNull { it.bucket == ReminderBucket.TODAY }?.items?.size ?: 0
        }
      val heroReminder =
        remember(ready?.sections) {
          ready?.sections
            ?.filter { it.bucket != ReminderBucket.PAUSED }
            ?.flatMap { it.items }
            ?.filter { it.isActive && !it.isCompleted }
            ?.minByOrNull { it.nextOccurrenceEpochMillis }
        }

      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
      ) {
        // Contextual Multi-select Bar OR Personalized Greeting Top Header
        if (isSelectionMode && ready != null) {
          GlassSurface(
            level = GlassLevel.BAR,
            shape = CircleShape,
            tintOverlay = palette.primaryAccent,
            accentBorder = palette.primaryAccent.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                IconButton(
                  onClick = { selectedIds = emptySet() },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear selection",
                    tint = palette.textPrimary,
                  )
                }
                Text(
                  text = "${selectedIds.size} ${strings.selectedCountLabel}",
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                )
              }

              IconButton(
                onClick = {
                  val allIds = allReadyItems.map { it.id }.toSet()
                  selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DoneAll,
                  contentDescription = "Select all",
                  tint = palette.primaryAccent,
                )
              }
            }
          }
        } else {
          val nowMillis = ready?.nowMillis ?: System.currentTimeMillis()
          val istHour = remember(nowMillis) { IstTime.fromEpochMillis(nowMillis).hour }
          val greeting =
            when (istHour) {
              in 5..11 -> "Good morning"
              in 12..16 -> "Good afternoon"
              in 17..20 -> "Good evening"
              else -> "Good night"
            }
          val summaryText =
            if (todayCount > 0) {
              "$todayCount reminder${if (todayCount == 1) "" else "s"} today • IST"
            } else {
              "${IstDate.today(nowMillis).formatWithWeekday()} • IST"
            }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = palette.textSecondary.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              GlassIconButton(
                icon = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = "Search reminders",
                accentHighlighted = showSearchBar,
                size = 42.dp,
                onClick = {
                  if (showSearchBar && ready != null && ready.searchQuery.isNotEmpty()) {
                    viewModel.onSearchQueryChanged("")
                  }
                  showSearchBar = !showSearchBar
                },
                modifier = Modifier.testTag("search_toggle_button"),
              )

              Box {
                GlassIconButton(
                  icon = Icons.AutoMirrored.Rounded.Sort,
                  contentDescription = stringResource(R.string.sort_label),
                  size = 42.dp,
                  onClick = { showSortMenu = true },
                  modifier = Modifier.testTag("sort_button"),
                )

                DropdownMenu(
                  expanded = showSortMenu,
                  onDismissRequest = { showSortMenu = false },
                ) {
                  ReminderSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                      text = { Text(option.label) },
                      trailingIcon = {
                        if (ready?.sortOption == option) {
                          Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = palette.primaryAccent,
                          )
                        }
                      },
                      onClick = {
                        viewModel.onSortOptionSelected(option)
                        showSortMenu = false
                      },
                    )
                  }
                }
              }

              if (showTopSettingsButton) {
                GlassIconButton(
                  icon = Icons.Outlined.Settings,
                  contentDescription = stringResource(R.string.nav_settings),
                  size = 42.dp,
                  onClick = onOpenSettings,
                  modifier = Modifier.testTag("settings_button"),
                )
              }
            }
          }
        }

        // Expandable Frosted Glass Search Field
        AnimatedVisibility(
          visible = showSearchBar || (ready?.searchQuery?.isNotEmpty() == true),
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically(),
        ) {
          GlassSurface(
            level = GlassLevel.BAR,
            shape = CircleShape,
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp),
              )
              Box(modifier = Modifier.weight(1f)) {
                val queryText = ready?.searchQuery.orEmpty()
                if (queryText.isEmpty()) {
                  Text(
                    text = strings.searchPlaceholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary.copy(alpha = 0.55f),
                  )
                }
                BasicTextField(
                  value = queryText,
                  onValueChange = viewModel::onSearchQueryChanged,
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                  cursorBrush = SolidColor(palette.primaryAccent),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                  keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                  modifier = Modifier.fillMaxWidth().testTag("search_input"),
                )
              }
              if (!ready?.searchQuery.isNullOrEmpty()) {
                IconButton(
                  onClick = { viewModel.onSearchQueryChanged("") },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.clearSearch,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }

        // Category Filter Pills
        if (ready != null && (ready.totalCount > 0 || ready.selectedCategory != null)) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GlassFilterPill(
              label = strings.filterAll,
              selected = ready.selectedCategory == null,
              onClick = { viewModel.onCategorySelected(null) },
              modifier = Modifier.testTag("filter_chip_all"),
            )

            ReminderCategory.entries.forEach { category ->
              val selected = ready.selectedCategory == category
              GlassFilterPill(
                label = strings.categoryName(category),
                icon = categoryIcon(category),
                selected = selected,
                onClick = { viewModel.onCategorySelected(category) },
                modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}"),
              )
            }
          }
        }

        // Subtle Reliability Banner
        if (!permissionsOk && ready != null && ready.totalCount > 0) {
          GlassSurface(
            level = GlassLevel.CARD,
            shape = RoundedCornerShape(18.dp),
            tintOverlay = palette.dangerColor,
            accentBorder = palette.dangerColor.copy(alpha = 0.50f),
            onClick = onOpenReliabilityCheck,
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .testTag("home_reliability_banner"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.alarmPermissionBanner,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f).padding(end = 10.dp),
              )
              Text(
                text = strings.fix,
                style = MaterialTheme.typography.labelLarge,
                color = palette.dangerColor,
              )
            }
          }
        }

        // Main Content Area
        when (val uiState = state) {
          HomeUiState.Loading -> {
            SkeletonReminderPlaceholder(modifier = Modifier.fillMaxSize())
          }

          is HomeUiState.Ready -> {
            if (uiState.sections.isEmpty()) {
              val hasActiveFilters =
                uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null
              HomeEmptyState(
                hasActiveFilters = hasActiveFilters,
                onAddReminder = onAddReminder,
                onClearFilters = viewModel::clearFilters,
              )
            } else {
              LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                  PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                // Hero "Next up" Card when unfiltered & not in selection mode
                if (
                  heroReminder != null &&
                    !isSelectionMode &&
                    uiState.searchQuery.isBlank() &&
                    uiState.selectedCategory == null
                ) {
                  item(key = "hero_next_up") {
                    HeroNextUpCard(
                      reminder = heroReminder,
                      nowMillis = uiState.nowMillis,
                      onClick = { onEditReminder(heroReminder.id) },
                    )
                  }
                }

                uiState.sections.forEach { section ->
                  stickyHeader(key = "header_${section.bucket.name}") {
                    SectionStickyHeader(
                      title = strings.bucketTitle(section.bucket),
                      count = section.items.size,
                      isMissed = section.bucket == ReminderBucket.MISSED,
                    )
                  }

                  items(
                    items = section.items,
                    key = { "reminder_${it.id}" },
                  ) { reminder ->
                    val isSelected = reminder.id in selectedIds
                    ReminderCard(
                      reminder = reminder,
                      bucket = section.bucket,
                      nowMillis = uiState.nowMillis,
                      isSelectionMode = isSelectionMode,
                      isSelected = isSelected,
                      onClick = {
                        if (isSelectionMode) {
                          selectedIds =
                            if (isSelected) selectedIds - reminder.id else selectedIds + reminder.id
                        } else {
                          onEditReminder(reminder.id)
                        }
                      },
                      onLongPress = {
                        selectedIds = selectedIds + reminder.id
                      },
                      onMarkDone = { viewModel.markDone(reminder) },
                      onTogglePause = { viewModel.togglePause(reminder) },
                      onDelete = { viewModel.deleteReminder(reminder) },
                      onSkipOccurrence = { viewModel.skipOccurrence(reminder) },
                      onMoveOccurrence = { moveDateTarget = reminder },
                      modifier = Modifier.animateItem(),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Contextual Floating Bulk Action Bar when multi-selecting
      AnimatedVisibility(
        visible = isSelectionMode && ready != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 96.dp),
      ) {
        val chosenItems = allReadyItems.filter { it.id in selectedIds }
        GlassSurface(
          level = GlassLevel.BAR,
          shape = CircleShape,
          modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = {
                chosenItems.forEach { viewModel.markDone(it) }
                selectedIds = emptySet()
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = palette.successColor,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.done, color = palette.textPrimary)
            }

            TextButton(
              onClick = {
                chosenItems.forEach { viewModel.togglePause(it) }
                selectedIds = emptySet()
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Outlined.PauseCircle,
                contentDescription = null,
                tint = palette.primaryAccent,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.pause, color = palette.textPrimary)
            }

            TextButton(
              onClick = {
                chosenItems.forEach { viewModel.deleteReminder(it) }
                selectedIds = emptySet()
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = palette.dangerColor,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.delete, color = palette.dangerColor)
            }
          }
        }
      }

      // Snackbar positioned clearly above the floating glass bottom bar
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 94.dp, start = 16.dp, end = 16.dp),
      )
    }
  }

  val targetForDate = moveDateTarget
  if (targetForDate != null) {
    val initialDate = IstDate.fromEpochMillis(targetForDate.nextOccurrenceEpochMillis)
    IstDatePickerDialog(
      initialDate = initialDate,
      onDateSelected = { pickedDate ->
        moveDateTarget = null
        moveTimeTarget = targetForDate to pickedDate
      },
      onDismiss = { moveDateTarget = null },
    )
  }

  val targetForTime = moveTimeTarget
  if (targetForTime != null) {
    val (targetReminder, pickedDate) = targetForTime
    val initialTime = IstTime.fromEpochMillis(targetReminder.nextOccurrenceEpochMillis)
    IstTimePickerDialog(
      initialTime = initialTime,
      title = "Move occurrence time (IST)",
      onTimeSelected = { pickedTime ->
        viewModel.moveOccurrence(targetReminder, pickedDate, pickedTime)
        moveTimeTarget = null
      },
      onDismiss = { moveTimeTarget = null },
    )
  }
}

@Composable
private fun HeroNextUpCard(
  reminder: Reminder,
  nowMillis: Long,
  onClick: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val style = categoryStyle(reminder.category)
  val occDate = IstDate.fromEpochMillis(reminder.nextOccurrenceEpochMillis).formatWithWeekday()
  val occTime = IstTime.fromEpochMillis(reminder.nextOccurrenceEpochMillis).format12Hour()
  val countdown = RecurrenceCalculator.countdownLabel(reminder.nextOccurrenceEpochMillis, nowMillis)

  GlassSurface(
    level = GlassLevel.CARD,
    shape = RoundedCornerShape(28.dp),
    tintOverlay = palette.primaryAccent,
    accentBorder = palette.primaryAccent.copy(alpha = 0.45f),
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("hero_next_up_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier =
            Modifier.clip(CircleShape)
              .background(palette.primaryAccent.copy(alpha = 0.16f))
              .border(1.dp, palette.primaryAccent.copy(alpha = 0.30f), CircleShape)
              .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
          Text(
            text = strings.nextUpLabel.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = palette.primaryAccent,
          )
        }

        Text(
          text = countdown,
          style = MaterialTheme.typography.labelLarge.tabular().copy(fontWeight = FontWeight.Bold),
          color = palette.primaryAccent,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(style.containerColor)
              .border(1.dp, style.contentColor.copy(alpha = 0.25f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = style.icon,
            contentDescription = style.label,
            tint = style.contentColor,
            modifier = Modifier.size(23.dp),
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = reminder.title,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "$occDate • $occTime IST",
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = palette.textSecondary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun GlassFilterPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
  val palette = LocalRemindlyPalette.current
  GlassSurface(
    level = if (selected) GlassLevel.BAR else GlassLevel.CARD,
    shape = CircleShape,
    tintOverlay = if (selected) palette.primaryAccent else null,
    accentBorder = if (selected) palette.primaryAccent.copy(alpha = 0.55f) else null,
    onClick = onClick,
    modifier = modifier.height(38.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (selected) palette.primaryAccent else palette.textSecondary,
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) palette.primaryAccent else palette.textPrimary,
      )
    }
  }
}

@Composable
private fun SectionStickyHeader(
  title: String,
  count: Int,
  isMissed: Boolean,
) {
  val palette = LocalRemindlyPalette.current
  Box(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
  ) {
    GlassSurface(
      level = GlassLevel.BAR,
      shape = CircleShape,
      tintOverlay = if (isMissed) palette.dangerColor else null,
      accentBorder = if (isMissed) palette.dangerColor.copy(alpha = 0.40f) else null,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (isMissed) {
          Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(palette.dangerColor),
          )
        }
        Text(
          text = title.uppercase(),
          style = MaterialTheme.typography.titleSmall,
          color = if (isMissed) palette.dangerColor else palette.textSecondary.copy(alpha = 0.85f),
        )
        Text(
          text = count.toString(),
          style = MaterialTheme.typography.labelSmall.tabular().copy(fontWeight = FontWeight.Bold),
          color = if (isMissed) palette.dangerColor else palette.primaryAccent,
        )
      }
    }
  }
}

@Composable
private fun HomeEmptyState(
  hasActiveFilters: Boolean,
  onAddReminder: () -> Unit,
  onClearFilters: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    // Minimalist custom Canvas thin-line geometric illustration
    Box(
      modifier = Modifier.size(116.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val c = center
        drawCircle(
          color = palette.primaryAccent.copy(alpha = 0.14f),
          radius = size.minDimension * 0.46f,
          center = c,
        )
        drawCircle(
          color = palette.primaryAccent.copy(alpha = 0.45f),
          radius = size.minDimension * 0.36f,
          center = c,
          style = stroke,
        )
        if (hasActiveFilters) {
          val r = size.minDimension * 0.14f
          drawCircle(
            color = palette.textPrimary,
            radius = r,
            center = Offset(c.x - 4.dp.toPx(), c.y - 4.dp.toPx()),
            style = stroke,
          )
          drawLine(
            color = palette.textPrimary,
            start = Offset(c.x + 5.dp.toPx(), c.y + 5.dp.toPx()),
            end = Offset(c.x + 18.dp.toPx(), c.y + 18.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
        } else {
          drawLine(
            color = palette.textPrimary,
            start = c,
            end = Offset(c.x, c.y - 16.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            color = palette.primaryAccent,
            start = c,
            end = Offset(c.x + 12.dp.toPx(), c.y + 7.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = if (hasActiveFilters) strings.emptySearchTitle else strings.emptyTitle,
      style = MaterialTheme.typography.titleLarge,
      color = palette.textPrimary,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = if (hasActiveFilters) strings.emptySearchSubtitle else strings.emptySubtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = palette.textSecondary.copy(alpha = 0.78f),
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(22.dp))

    if (hasActiveFilters) {
      GlassPillButton(
        text = strings.clearFilters,
        isPrimary = false,
        onClick = onClearFilters,
        modifier = Modifier.testTag("clear_filters_button"),
      )
    } else {
      GlassPillButton(
        text = strings.createFirstReminder,
        icon = Icons.Rounded.Add,
        isPrimary = true,
        onClick = onAddReminder,
        modifier = Modifier.testTag("empty_add_button"),
      )
    }
  }
}
