package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.RemindlyApplication
import com.example.domain.model.AppThemeMode
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.time.IstDate
import com.example.domain.time.RecurrenceCalculator
import com.example.ui.localization.RemindlyStrings
import com.example.ui.localization.stringsForLanguage
import com.example.ui.theme.RemindlyVisualPalette
import com.example.ui.theme.visualPaletteFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val WidgetReminderIdKey = ActionParameters.Key<Long>("widget_reminder_id")

class RemindlyWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = RemindlyGlanceWidget()
}

private data class WidgetDataSnapshot(
  val reminders: List<Reminder>,
  val strings: RemindlyStrings,
  val themeMode: AppThemeMode,
  val nowMillis: Long,
)

class RemindlyGlanceWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode =
    SizeMode.Responsive(
      setOf(
        SMALL_WIDGET,
        MEDIUM_WIDGET,
        LARGE_WIDGET,
      )
    )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snapshot =
      withContext(Dispatchers.IO) {
        val app = context.applicationContext as RemindlyApplication
        val prefs = app.container.preferencesRepository.currentPreferences()
        val now = System.currentTimeMillis()
        val items =
          app.container.reminderRepository
            .getAllActiveRemindersOnce()
            .filter { it.isActive && !it.isCompleted && !it.isDeleted }
            .sortedBy { it.nextOccurrenceEpochMillis }
            .take(5)
        WidgetDataSnapshot(
          reminders = items,
          strings = stringsForLanguage(prefs.appLanguage),
          themeMode = prefs.themeMode,
          nowMillis = now,
        )
      }

    provideContent {
      val systemDark =
        (context.resources.configuration.uiMode and
          android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
          android.content.res.Configuration.UI_MODE_NIGHT_YES
      val palette = visualPaletteFor(snapshot.themeMode, systemDark)

      WidgetContent(
        context = context,
        reminders = snapshot.reminders,
        strings = snapshot.strings,
        palette = palette,
        nowMillis = snapshot.nowMillis,
      )
    }
  }

  companion object {
    private val SMALL_WIDGET = DpSize(180.dp, 110.dp)
    private val MEDIUM_WIDGET = DpSize(260.dp, 140.dp)
    private val LARGE_WIDGET = DpSize(300.dp, 220.dp)
  }
}

@Composable
private fun WidgetContent(
  context: Context,
  reminders: List<Reminder>,
  strings: RemindlyStrings,
  palette: RemindlyVisualPalette,
  nowMillis: Long,
) {
  val size = LocalSize.current
  val maxRows =
    when {
      size.height >= 200.dp -> 3
      size.height >= 135.dp -> 2
      else -> 1
    }
  val visibleReminders = reminders.take(maxRows)

  val openAppIntent =
    Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
  val addReminderIntent =
    Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(MainActivity.EXTRA_OPEN_NEW_REMINDER, true)
    }

  val missedCount =
    reminders.count { RecurrenceCalculator.classifyBucket(it, nowMillis) == ReminderBucket.MISSED }

  val outerBg =
    if (palette.isDark) palette.dialogSolidFallback.copy(alpha = 0.92f)
    else palette.dialogSolidFallback.copy(alpha = 0.94f)
  val borderTint = palette.glassBorderTop
  val rowBg =
    if (palette.isDark) Color.White.copy(alpha = 0.08f)
    else Color.White.copy(alpha = 0.74f)

  Box(
    modifier =
      GlanceModifier.fillMaxSize()
        .background(ColorProvider(borderTint))
        .cornerRadius(28.dp)
        .padding(1.dp),
  ) {
    Column(
      modifier =
        GlanceModifier.fillMaxSize()
          .background(ColorProvider(outerBg))
          .cornerRadius(27.dp)
          .padding(14.dp),
    ) {
      // Header
      Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openAppIntent)),
        ) {
          Text(
            text = strings.widgetTitle,
            style =
              TextStyle(
                color = ColorProvider(palette.textPrimary),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
              ),
            maxLines = 1,
          )
          Text(
            text =
              if (missedCount > 0) "$missedCount ${strings.missedBadge.lowercase()} • IST"
              else "${IstDate.today(nowMillis).formatShort()} • IST",
            style =
              TextStyle(
                color =
                  ColorProvider(
                    if (missedCount > 0) palette.dangerColor
                    else palette.textSecondary.copy(alpha = 0.80f)
                  ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
              ),
            maxLines = 1,
          )
        }

        Box(
          modifier =
            GlanceModifier.background(ColorProvider(palette.primaryAccent))
              .cornerRadius(16.dp)
              .clickable(actionStartActivity(addReminderIntent))
              .padding(horizontal = 11.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "+",
            style =
              TextStyle(
                color = ColorProvider(palette.onPrimaryAccent),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
              ),
          )
        }
      }

      Spacer(modifier = GlanceModifier.height(10.dp))

      if (visibleReminders.isEmpty()) {
        Box(
          modifier =
            GlanceModifier.fillMaxSize()
              .background(ColorProvider(rowBg))
              .cornerRadius(18.dp)
              .clickable(actionStartActivity(addReminderIntent))
              .padding(12.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = strings.widgetEmpty,
            style =
              TextStyle(
                color = ColorProvider(palette.textSecondary),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
              ),
          )
        }
      } else {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
          visibleReminders.forEachIndexed { index, reminder ->
            if (index > 0) {
              Spacer(modifier = GlanceModifier.height(6.dp))
            }
            WidgetReminderRow(
              context = context,
              reminder = reminder,
              palette = palette,
              rowBg = rowBg,
              strings = strings,
              nowMillis = nowMillis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WidgetReminderRow(
  context: Context,
  reminder: Reminder,
  palette: RemindlyVisualPalette,
  rowBg: Color,
  strings: RemindlyStrings,
  nowMillis: Long,
) {
  val isMissed = RecurrenceCalculator.classifyBucket(reminder, nowMillis) == ReminderBucket.MISSED
  val countdown =
    if (isMissed) strings.missedBadge
    else RecurrenceCalculator.countdownLabel(reminder.nextOccurrenceEpochMillis, nowMillis)
  val dotColor =
    if (isMissed) palette.dangerColor
    else palette.categoryTints[reminder.category]?.second ?: palette.primaryAccent

  val editIntent =
    Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(MainActivity.EXTRA_EDIT_REMINDER_ID, reminder.id)
    }

  Row(
    modifier =
      GlanceModifier.fillMaxWidth()
        .background(
          ColorProvider(
            if (isMissed) palette.dangerColor.copy(alpha = 0.16f) else rowBg
          )
        )
        .cornerRadius(16.dp)
        .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(editIntent)),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Category colored dot
      Box(
        modifier =
          GlanceModifier.size(8.dp)
            .background(ColorProvider(dotColor))
            .cornerRadius(4.dp),
      ) {}
      Spacer(modifier = GlanceModifier.width(8.dp))
      Text(
        text = reminder.title,
        style =
          TextStyle(
            color = ColorProvider(palette.textPrimary),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
          ),
        maxLines = 1,
        modifier = GlanceModifier.defaultWeight(),
      )
      Spacer(modifier = GlanceModifier.width(6.dp))
      Text(
        text = countdown,
        style =
          TextStyle(
            color = ColorProvider(if (isMissed) palette.dangerColor else palette.primaryAccent),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          ),
        maxLines = 1,
      )
    }

    Spacer(modifier = GlanceModifier.width(8.dp))

    Box(
      modifier =
        GlanceModifier.background(ColorProvider(palette.primaryAccent.copy(alpha = 0.18f)))
          .cornerRadius(10.dp)
          .clickable(
            actionRunCallback<MarkDoneWidgetAction>(
              actionParametersOf(WidgetReminderIdKey to reminder.id)
            )
          )
          .padding(horizontal = 8.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "✓",
        style =
          TextStyle(
            color = ColorProvider(palette.primaryAccent),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
          ),
      )
    }
  }
}

class MarkDoneWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val reminderId = parameters[WidgetReminderIdKey] ?: return
    val app = context.applicationContext as RemindlyApplication
    withContext(Dispatchers.IO) {
      app.container.reminderRepository.markReminderDone(reminderId)
    }
    RemindlyWidgetUpdater.requestUpdate(context)
  }
}
