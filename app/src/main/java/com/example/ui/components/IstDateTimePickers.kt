package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.tabular
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val FULL_MONTH_NAMES =
  listOf(
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  )

@Composable
fun IstDatePickerDialog(
  initialDate: IstDate,
  onDateSelected: (IstDate) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val today = remember { IstDate.today() }
  var selectedDate by remember { mutableStateOf(initialDate) }
  var displayYear by remember { mutableIntStateOf(initialDate.year) }
  var displayMonth by remember { mutableIntStateOf(initialDate.month) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    GlassSurface(
      level = GlassLevel.DIALOG,
      shape = RoundedCornerShape(30.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).widthIn(max = 420.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Selected header & quick Today/Tomorrow pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "SELECT DATE (IST)",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textSecondary.copy(alpha = 0.72f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = selectedDate.formatWithWeekday(),
              style = MaterialTheme.typography.headlineSmall,
              color = palette.textPrimary,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickDateChip(
              label = "Today",
              selected = selectedDate == today,
              onClick = {
                selectedDate = today
                displayYear = today.year
                displayMonth = today.month
              },
            )
            val tomorrow = remember(today) { today.plusDays(1) }
            QuickDateChip(
              label = "Tom.",
              selected = selectedDate == tomorrow,
              onClick = {
                selectedDate = tomorrow
                displayYear = tomorrow.year
                displayMonth = tomorrow.month
              },
            )
          }
        }

        // Month Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Previous month",
            size = 40.dp,
            onClick = {
              if (displayMonth == 1) {
                displayMonth = 12
                displayYear -= 1
              } else {
                displayMonth -= 1
              }
            },
          )
          Text(
            text = "${FULL_MONTH_NAMES[displayMonth - 1]} $displayYear",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Next month",
            size = 40.dp,
            onClick = {
              if (displayMonth == 12) {
                displayMonth = 1
                displayYear += 1
              } else {
                displayMonth += 1
              }
            },
          )
        }

        IstMonthCalendarGrid(
          year = displayYear,
          month = displayMonth,
          today = today,
          selectedDate = selectedDate,
          dotColorsByDay = emptyMap(),
          onDaySelected = { day ->
            selectedDate = IstDate(displayYear, displayMonth, day)
          },
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Text("Cancel", color = palette.textSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          GlassPillButton(
            text = "Select",
            onClick = {
              onDateSelected(selectedDate)
              onDismiss()
            },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun QuickDateChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  Box(
    modifier =
      Modifier.clip(CircleShape)
        .background(
          if (selected) palette.primaryAccent.copy(alpha = 0.22f)
          else palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.08f else 0.45f)
        )
        .border(
          1.dp,
          if (selected) palette.primaryAccent else palette.glassBorderBottom,
          CircleShape,
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) palette.primaryAccent else palette.textSecondary,
    )
  }
}

@Composable
fun IstMonthCalendarGrid(
  year: Int,
  month: Int,
  today: IstDate,
  selectedDate: IstDate?,
  dotColorsByDay: Map<Int, List<Color>>,
  onDaySelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  val haptic = LocalHapticFeedback.current

  val firstOfMonth = remember(year, month) { IstDate(year, month, 1) }
  val daysInMonth = remember(year, month) { IstDate.lastDayOfMonth(year, month) }
  // Mon=0 .. Sun=6
  val leadingEmpty = firstOfMonth.weekday().ordinal
  val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7
  val rowCount = totalCells / 7

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(modifier = Modifier.fillMaxWidth()) {
      listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayHeader ->
        Text(
          text = dayHeader,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = palette.textSecondary.copy(alpha = 0.65f),
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        )
      }
    }

    for (row in 0 until rowCount) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        for (col in 0..6) {
          val cellIndex = row * 7 + col
          val dayNumber = cellIndex - leadingEmpty + 1
          if (dayNumber !in 1..daysInMonth) {
            Spacer(modifier = Modifier.weight(1f).height(46.dp))
          } else {
            val isToday = today.year == year && today.month == month && today.day == dayNumber
            val isSelected =
              selectedDate != null &&
                selectedDate.year == year &&
                selectedDate.month == month &&
                selectedDate.day == dayNumber
            val dots = dotColorsByDay[dayNumber].orEmpty()

            Box(
              modifier =
                Modifier.weight(1f)
                  .height(46.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDaySelected(dayNumber)
                  },
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Box(
                  modifier =
                    Modifier.size(34.dp)
                      .clip(CircleShape)
                      .background(
                        when {
                          isSelected -> palette.primaryAccent
                          else -> Color.Transparent
                        }
                      )
                      .then(
                        if (isToday && !isSelected) {
                          Modifier.border(1.5.dp, palette.primaryAccent, CircleShape)
                        } else {
                          Modifier
                        }
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = dayNumber.toString(),
                    style =
                      MaterialTheme.typography.bodyMedium.tabular().copy(
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                      ),
                    color =
                      when {
                        isSelected -> palette.onPrimaryAccent
                        isToday -> palette.primaryAccent
                        else -> palette.textPrimary
                      },
                  )
                }

                if (dots.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    dots.take(3).forEach { dotColor ->
                      Box(
                        modifier =
                          Modifier.size(4.5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) palette.primaryAccent else dotColor),
                      )
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.height(6.5.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun IstTimePickerDialog(
  initialTime: IstTime,
  title: String = "Select time (IST)",
  onTimeSelected: (IstTime) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val initialHour12 =
    when (val h = initialTime.hour % 12) {
      0 -> 12
      else -> h
    }
  var selectedHour12 by remember { mutableIntStateOf(initialHour12) }
  var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }
  var isPm by remember { mutableStateOf(initialTime.hour >= 12) }

  val resolved24Hour =
    when {
      isPm && selectedHour12 < 12 -> selectedHour12 + 12
      !isPm && selectedHour12 == 12 -> 0
      else -> selectedHour12
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    GlassSurface(
      level = GlassLevel.DIALOG,
      shape = RoundedCornerShape(30.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 380.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary.copy(alpha = 0.72f),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${IstTime(resolved24Hour, selectedMinute).format12Hour()} IST",
            style = MaterialTheme.typography.headlineMedium.tabular(),
            color = palette.primaryAccent,
          )
        }

        // Smooth wheel picker columns
        Box(
          modifier = Modifier.fillMaxWidth().height(168.dp),
          contentAlignment = Alignment.Center,
        ) {
          // Center glass lens bar
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.primaryAccent.copy(alpha = 0.14f))
                .border(1.dp, palette.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            WheelPickerColumn(
              items = (1..12).map { it.toString() },
              initialIndex = (initialHour12 - 1).coerceIn(0, 11),
              onSelectedIndex = { idx -> selectedHour12 = idx + 1 },
              modifier = Modifier.weight(1f),
            )

            Text(
              text = ":",
              style = MaterialTheme.typography.headlineSmall.tabular(),
              color = palette.textPrimary,
            )

            WheelPickerColumn(
              items = (0..59).map { "%02d".format(it) },
              initialIndex = initialTime.minute.coerceIn(0, 59),
              onSelectedIndex = { idx -> selectedMinute = idx },
              modifier = Modifier.weight(1f),
            )

            // AM / PM selector
            Column(
              modifier = Modifier.weight(0.9f).padding(start = 8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              listOf(false to "AM", true to "PM").forEach { (pmFlag, label) ->
                val active = isPm == pmFlag
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(44.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .background(
                        if (active) palette.primaryAccent
                        else palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.10f else 0.45f)
                      )
                      .border(
                        1.dp,
                        if (active) palette.primaryAccent else palette.glassBorderBottom,
                        RoundedCornerShape(14.dp),
                      )
                      .clickable { isPm = pmFlag },
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (active) palette.onPrimaryAccent else palette.textPrimary,
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Text("Cancel", color = palette.textSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          GlassPillButton(
            text = "Confirm",
            onClick = {
              onTimeSelected(IstTime(resolved24Hour, selectedMinute))
              onDismiss()
            },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerColumn(
  items: List<String>,
  initialIndex: Int,
  onSelectedIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  val haptic = LocalHapticFeedback.current
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
  val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

  LaunchedEffect(listState) {
    snapshotFlow {
      val layoutInfo = listState.layoutInfo
      val centerY = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
      layoutInfo.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - centerY) }?.index
    }
      .distinctUntilChanged()
      .map { it?.coerceIn(0, items.lastIndex) }
      .collect { idx ->
        if (idx != null) {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onSelectedIndex(idx)
        }
      }
  }

  LazyColumn(
    state = listState,
    flingBehavior = flingBehavior,
    contentPadding = PaddingValues(vertical = 60.dp),
    modifier = modifier.height(168.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    items(items.size) { index ->
      val centerIdx =
        remember(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
          val offset = listState.firstVisibleItemScrollOffset
          (listState.firstVisibleItemIndex + if (offset > 60) 1 else 0).coerceIn(0, items.lastIndex)
        }
      val isCenter = index == centerIdx

      Box(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = items[index],
          style =
            MaterialTheme.typography.headlineSmall.tabular().copy(
              fontSize = if (isCenter) 24.sp else 18.sp,
              fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
            ),
          color = if (isCenter) palette.textPrimary else palette.textSecondary.copy(alpha = 0.55f),
          modifier = Modifier.alpha(if (isCenter) 1f else 0.72f),
        )
      }
    }
  }
}
