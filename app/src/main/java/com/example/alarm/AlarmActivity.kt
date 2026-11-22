package com.example.alarm

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.RemindlyApplication
import com.example.domain.model.UserPreferences
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.ui.components.categoryLabel
import com.example.ui.components.categoryStyle
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.LocalVisualPerformance
import com.example.ui.theme.RemindlyTheme
import com.example.ui.theme.tabular
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureLockScreenFlags()
    enableEdgeToEdge()

    val appContainer = (application as RemindlyApplication).container

    setContent {
      val prefs by
        appContainer.preferencesRepository.userPreferences.collectAsStateWithLifecycle(
          initialValue = UserPreferences()
        )
      val ringingState by AlarmRingingService.ringingState.collectAsStateWithLifecycle()

      LaunchedEffect(ringingState) {
        if (ringingState == null) {
          finishAndRemoveTask()
        }
      }

      BackHandler(enabled = true) {}

      RemindlyTheme(
        themeMode = prefs.themeMode,
        accentPalette = prefs.accentPalette,
        lowPowerVisuals = prefs.lowPowerVisuals,
      ) {
        val current = ringingState
        if (current != null) {
          AlarmLockScreenContent(
            state = current,
            onSnooze = {
              val snoozeIntent =
                Intent(this, AlarmRingingService::class.java).apply {
                  action = AlarmRingingService.ACTION_SNOOZE
                  putExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, current.reminderId)
                  putExtra(ReminderTriggerReceiver.EXTRA_TITLE, current.title)
                  putExtra(ReminderTriggerReceiver.EXTRA_CATEGORY, current.category.name)
                  putExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, current.isTest)
                }
              startService(snoozeIntent)
              finishAndRemoveTask()
            },
            onDone = {
              val doneIntent =
                Intent(this, AlarmRingingService::class.java).apply {
                  action = AlarmRingingService.ACTION_DONE
                  putExtra(ReminderTriggerReceiver.EXTRA_REMINDER_ID, current.reminderId)
                  putExtra(ReminderTriggerReceiver.EXTRA_IS_TEST, current.isTest)
                }
              startService(doneIntent)
              finishAndRemoveTask()
            },
          )
        }
      }
    }
  }

  private fun configureLockScreenFlags() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      val km = getSystemService(KeyguardManager::class.java)
      km?.requestDismissKeyguard(this, null)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
      )
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }
}

@Composable
private fun AlarmLockScreenContent(
  state: RingingAlarmState,
  onSnooze: () -> Unit,
  onDone: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val strings = LocalStrings.current
  val catStyle = categoryStyle(state.category)

  val pulseScale by
    if (!perf.reducedMotion && !perf.lowPowerVisuals) {
      rememberInfiniteTransition(label = "alarm_pulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 1_100),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "alarm_ring_scale",
      )
    } else {
      remember { mutableFloatStateOf(1f) }
    }

  val fullTimeText = IstTime.fromEpochMillis(state.occurrenceMillis).format12Hour()
  val timeMain = fullTimeText.substringBefore(" ")
  val timePeriod = fullTimeText.substringAfter(" ", "")
  val dateText = IstDate.fromEpochMillis(state.occurrenceMillis).formatWithWeekday()

  AmbientOrbBackground(pulseBoost = true) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 28.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Category Pill & Large Light Tabular IST Clock
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          GlassSurface(
            level = GlassLevel.BAR,
            shape = CircleShape,
            tintOverlay = catStyle.contentColor,
            accentBorder = catStyle.contentColor.copy(alpha = 0.35f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = catStyle.icon,
                contentDescription = null,
                tint = catStyle.contentColor,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = categoryLabel(state.category),
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary,
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = timeMain,
              style = MaterialTheme.typography.displayLarge.tabular(),
              color = palette.textPrimary,
              maxLines = 1,
            )
            if (timePeriod.isNotBlank()) {
              Text(
                text = timePeriod,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = palette.primaryAccent,
                modifier = Modifier.padding(bottom = 18.dp),
              )
            }
          }
          Text(
            text = "$dateText • IST",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary.copy(alpha = 0.80f),
          )
        }

        // Center Pulsing Category Glass Circle + Title + Note
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(vertical = 14.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            // Outer pulsing aura ring
            Box(
              modifier =
                Modifier.size(152.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(catStyle.contentColor.copy(alpha = 0.14f))
                  .border(1.5.dp, catStyle.contentColor.copy(alpha = 0.35f), CircleShape),
            )

            GlassSurface(
              level = GlassLevel.DIALOG,
              shape = CircleShape,
              tintOverlay = catStyle.contentColor,
              accentBorder = catStyle.contentColor.copy(alpha = 0.50f),
              modifier = Modifier.size(116.dp),
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = catStyle.icon,
                  contentDescription = "Ringing alarm",
                  tint = catStyle.contentColor,
                  modifier = Modifier.size(48.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(26.dp))

          Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
          )

          if (state.offsetLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            GlassSurface(level = GlassLevel.CARD, shape = CircleShape) {
              Text(
                text = state.offsetLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.primaryAccent,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              )
            }
          }

          if (state.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = state.note,
              style = MaterialTheme.typography.bodyLarge,
              color = palette.textSecondary.copy(alpha = 0.82f),
              textAlign = TextAlign.Center,
            )
          }
        }

        // Bottom Frosted Snooze Pill + Interactive Slide-to-Done Track
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          GlassPillButton(
            text = "Snooze (${state.snoozeMinutes} min)",
            icon = Icons.Outlined.Snooze,
            isPrimary = false,
            onClick = onSnooze,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("alarm_snooze_button"),
          )

          SlideToDoneGlassTrack(
            label = strings.slideToDone,
            onCompleted = onDone,
            modifier = Modifier.fillMaxWidth().testTag("alarm_done_button"),
          )
        }
      }
    }
  }
}

@Composable
private fun SlideToDoneGlassTrack(
  label: String,
  onCompleted: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val thumbSizeDp = 56.dp
  val trackPadDp = 5.dp

  var dragOffsetPx by remember { mutableFloatStateOf(0f) }
  var isDragging by remember { mutableStateOf(false) }
  var completed by remember { mutableStateOf(false) }

  val animatedOffsetPx by
    animateFloatAsState(
      targetValue = dragOffsetPx,
      animationSpec =
        if (isDragging) tween(0)
        else spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
      label = "slide_to_done_offset",
    )

  BoxWithConstraints(
    modifier =
      modifier
        .height(66.dp)
        .clip(CircleShape)
        .clickable(
          role = Role.Button,
          onClickLabel = label,
          onClick = {
            if (!completed) {
              completed = true
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onCompleted()
            }
          },
        )
        .semantics { contentDescription = label },
  ) {
    val trackWidthPx = with(density) { maxWidth.toPx() }
    val thumbWidthPx = with(density) { thumbSizeDp.toPx() }
    val padPx = with(density) { trackPadDp.toPx() }
    val maxTravelPx = (trackWidthPx - thumbWidthPx - padPx * 2f).coerceAtLeast(1f)
    val progress = (animatedOffsetPx / maxTravelPx).coerceIn(0f, 1f)

    GlassSurface(
      level = GlassLevel.DIALOG,
      shape = CircleShape,
      tintOverlay = palette.primaryAccent,
      accentBorder = palette.primaryAccent.copy(alpha = 0.45f),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(trackPadDp),
        contentAlignment = Alignment.CenterStart,
      ) {
        // Subtle fill trail behind knob
        if (progress > 0.02f) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .background(palette.primaryAccent.copy(alpha = 0.14f + 0.18f * progress)),
          )
        }

        // Centered Track Prompt
        Text(
          text = label,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
          modifier = Modifier.align(Alignment.Center).alpha((1f - progress * 1.4f).coerceIn(0f, 1f)),
        )

        // Draggable Glowing Thumb
        Box(
          modifier =
            Modifier.offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
              .size(thumbSizeDp)
              .then(
                if (!palette.isMonochrome) {
                  Modifier.shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = palette.primaryAccent.copy(alpha = 0.55f),
                    spotColor = palette.primaryAccent.copy(alpha = 0.55f),
                  )
                } else {
                  Modifier
                }
              )
              .clip(CircleShape)
              .background(palette.primaryAccent)
              .pointerInput(maxTravelPx) {
                detectHorizontalDragGestures(
                  onDragStart = { isDragging = true },
                  onDragCancel = {
                    isDragging = false
                    if (!completed) dragOffsetPx = 0f
                  },
                  onDragEnd = {
                    isDragging = false
                    if (!completed) {
                      if (dragOffsetPx >= maxTravelPx * 0.72f) {
                        completed = true
                        dragOffsetPx = maxTravelPx
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleted()
                      } else {
                        dragOffsetPx = 0f
                      }
                    }
                  },
                  onHorizontalDrag = { _, dragAmount ->
                    if (!completed) {
                      dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxTravelPx)
                    }
                  },
                )
              },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              if (progress >= 0.70f || completed) Icons.Rounded.Check
              else Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = palette.onPrimaryAccent,
            modifier = Modifier.size(24.dp),
          )
        }
      }
    }
  }
}
