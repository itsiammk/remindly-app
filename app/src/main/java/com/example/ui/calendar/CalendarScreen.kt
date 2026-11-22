package com.example.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Reminder
import com.example.domain.model.UserPreferences
import com.example.domain.time.IstDate
import com.example.domain.time.RecurrenceCalculator
import com.example.ui.components.IstMonthCalendarGrid
import com.example.ui.components.ReminderCard
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import kotlinx.coroutines.launch

private val MONTH_NAMES_FULL =
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
fun CalendarScreen(
  reminders: List<Reminder>,
  preferences: UserPreferences,
  nowMillis: Long,
  onEditReminder: (Long) -> Unit,
  onMarkDone: (Reminder) -> Unit,
  onTogglePause: (Reminder) -> Unit,
  onDelete: (Reminder) -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val scope = rememberCoroutineScope()

  val todayIst = remember(nowMillis) { IstDate.today(nowMillis) }
  var selectedDate by remember { mutableStateOf(todayIst) }

  val basePage = 1200
  val pagerState = rememberPagerState(initialPage = basePage, pageCount = { 2401 })

  fun monthForPage(page: Int): Pair<Int, Int> {
    val offsetMonths = page - basePage
    val totalMonths = (todayIst.year * 12 + (todayIst.month - 1)) + offsetMonths
    val year = Math.floorDiv(totalMonths, 12)
    val month = Math.floorMod(totalMonths, 12) + 1
    return year to month
  }

  val (currentYear, currentMonth) = monthForPage(pagerState.currentPage)
  val currentMonthMap =
    remember(reminders, currentYear, currentMonth, preferences.defaultHour, preferences.defaultMinute) {
      RecurrenceCalculator.remindersByDayInMonth(
        reminders = reminders,
        year = currentYear,
        month = currentMonth,
        defaultHour = preferences.defaultHour,
        defaultMinute = preferences.defaultMinute,
      )
    }

  val selectedDayReminders =
    remember(reminders, selectedDate, preferences.defaultHour, preferences.defaultMinute) {
      val map =
        RecurrenceCalculator.remindersByDayInMonth(
          reminders = reminders,
          year = selectedDate.year,
          month = selectedDate.month,
          defaultHour = preferences.defaultHour,
          defaultMinute = preferences.defaultMinute,
        )
      map[selectedDate.day].orEmpty()
    }

  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item(key = "calendar_header") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = strings.navCalendar,
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary,
              )
              Text(
                text = "${todayIst.formatWithWeekday()} • IST",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary.copy(alpha = 0.72f),
              )
            }
          }
        }

        item(key = "calendar_glass_card") {
          GlassSurface(
            level = GlassLevel.CARD,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().testTag("calendar_month_card"),
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
                GlassIconButton(
                  icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                  contentDescription = "Previous month",
                  size = 40.dp,
                  onClick = {
                    scope.launch {
                      pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                  },
                )
                Text(
                  text = "${MONTH_NAMES_FULL[currentMonth - 1]} $currentYear",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = palette.textPrimary,
                )
                GlassIconButton(
                  icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = "Next month",
                  size = 40.dp,
                  onClick = {
                    scope.launch {
                      pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(2400))
                    }
                  },
                )
              }

              HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
              ) { page ->
                val (y, m) = monthForPage(page)
                val monthMap =
                  remember(reminders, y, m, preferences.defaultHour, preferences.defaultMinute) {
                    if (y == currentYear && m == currentMonth) {
                      currentMonthMap
                    } else {
                      RecurrenceCalculator.remindersByDayInMonth(
                        reminders = reminders,
                        year = y,
                        month = m,
                        defaultHour = preferences.defaultHour,
                        defaultMinute = preferences.defaultMinute,
                      )
                    }
                  }
                val dotColorsByDay =
                  remember(monthMap, palette) {
                    monthMap.mapValues { (_, list) ->
                      list.take(3).map { rem ->
                        palette.categoryTints[rem.category]?.second ?: palette.primaryAccent
                      }
                    }
                  }

                IstMonthCalendarGrid(
                  year = y,
                  month = m,
                  today = todayIst,
                  selectedDate = selectedDate,
                  dotColorsByDay = dotColorsByDay,
                  onDaySelected = { day ->
                    selectedDate = IstDate(y, m, day)
                  },
                )
              }
            }
          }
        }

        item(key = "selected_day_header") {
          Text(
            text = "${selectedDate.formatWithWeekday()} • ${selectedDayReminders.size}".uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary.copy(alpha = 0.70f),
            modifier = Modifier.padding(top = 4.dp),
          )
        }

        if (selectedDayReminders.isEmpty()) {
          item(key = "selected_day_empty") {
            AnimatedContent(
              targetState = selectedDate,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "calendar_empty_day",
            ) { _ ->
              GlassSurface(
                level = GlassLevel.CARD,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(22.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = strings.calendarEmptyDay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary.copy(alpha = 0.80f),
                  )
                }
              }
            }
          }
        } else {
          items(
            items = selectedDayReminders,
            key = { "cal_rem_${selectedDate}_${it.id}" },
          ) { reminder ->
            val bucket = RecurrenceCalculator.classifyBucket(reminder, nowMillis)
            ReminderCard(
              reminder = reminder,
              bucket = bucket,
              nowMillis = nowMillis,
              onClick = { onEditReminder(reminder.id) },
              onMarkDone = { onMarkDone(reminder) },
              onTogglePause = { onTogglePause(reminder) },
              onDelete = { onDelete(reminder) },
            )
          }
        }
      }
    }
  }
}
