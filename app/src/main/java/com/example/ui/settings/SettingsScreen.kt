package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.alarm.AlarmSoundPlayer
import com.example.alarm.PermissionAndOemHelper
import com.example.domain.model.AlarmSoundOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DeliveryChannel
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderBucket
import com.example.domain.model.ReminderCategory
import com.example.domain.model.RepeatRuleType
import com.example.domain.model.VibrationPatternOption
import com.example.domain.time.IstDate
import com.example.domain.time.IstTime
import com.example.security.AppLockManager
import com.example.ui.components.IstTimePickerDialog
import com.example.ui.components.ReminderCard
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
import com.example.ui.theme.visualPaletteFor
import com.example.widget.RemindlyWidgetUpdater
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  onOpenReliabilityCheck: () -> Unit,
  onOpenBackupShare: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenRecycleBin: () -> Unit,
  onReplayOnboarding: () -> Unit,
  showBackButton: Boolean = true,
  extraBottomPadding: Dp = 24.dp,
) {
  val context = LocalContext.current
  val strings = LocalStrings.current
  val palette = LocalRemindlyPalette.current
  val prefs by viewModel.preferences.collectAsStateWithLifecycle()
  val deletedCount by viewModel.deletedRemindersCount.collectAsStateWithLifecycle()
  val historyCount by viewModel.historyCount.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var showDefaultTimePicker by remember { mutableStateOf(false) }
  var showSoundPicker by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var refreshTick by remember { mutableIntStateOf(0) }
  var reliabilityStatus by remember(refreshTick) {
    mutableStateOf(PermissionAndOemHelper.inspectStatus(context))
  }
  val biometricAvailable = remember(context) { AppLockManager.canUseBiometric(context) }
  var hiddenTestTapCount by remember { mutableIntStateOf(0) }
  var showHiddenTestSection by remember { mutableStateOf(false) }

  val soundPlayer = remember(context) { AlarmSoundPlayer(context) }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    refreshTick++
  }

  DisposableEffect(Unit) {
    onDispose { soundPlayer.stop() }
  }

  val nowMillis = remember { System.currentTimeMillis() }
  val previewReminder =
    remember(prefs.defaultHour, prefs.defaultMinute) {
      val today = IstDate.today(nowMillis)
      Reminder(
        id = 999L,
        title = "Ananya's Birthday",
        note = "Order dark chocolate cake",
        category = ReminderCategory.BIRTHDAY,
        anchorYear = today.year - 24,
        anchorMonth = today.month,
        anchorDay = today.day,
        yearKnown = true,
        hasExplicitTime = false,
        hour = prefs.defaultHour,
        minute = prefs.defaultMinute,
        repeatRule = RepeatRuleType.YEARLY,
        nextOccurrenceEpochMillis = today.toEpochMillis(prefs.defaultHour, prefs.defaultMinute),
      )
    }

  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = extraBottomPadding),
        verticalArrangement = Arrangement.spacedBy(22.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (showBackButton) {
            GlassIconButton(
              icon = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = strings.back,
              size = 42.dp,
              onClick = onNavigateBack,
              modifier = Modifier.testTag("settings_back_button"),
            )
          }
          Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.displaySmall,
            color = palette.textPrimary,
          )
        }

        // 1. APPEARANCE — Live Miniature Theme Preview Carousel + Live ReminderCard Preview + Low Power Visuals
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SettingsSectionLabel(stringResource(R.string.settings_theme_mode))

          val systemDark = isSystemInDarkTheme()
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
          ) {
            items(AppThemeMode.entries, key = { it.name }) { mode ->
              ThemeMiniPreviewCard(
                mode = mode,
                selected = prefs.themeMode == mode,
                systemDark = systemDark,
                onClick = { viewModel.setThemeMode(mode) },
              )
            }
          }

          Text(
            text = stringResource(R.string.settings_preview_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary.copy(alpha = 0.70f),
            modifier = Modifier.padding(top = 2.dp),
          )

          ReminderCard(
            reminder = previewReminder,
            bucket = ReminderBucket.TODAY,
            nowMillis = nowMillis,
            onClick = {},
            onMarkDone = {},
            onTogglePause = {},
            onDelete = {},
            showActionsMenu = false,
          )

          GlassSurface(level = GlassLevel.CARD) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  text = strings.lowPowerVisualsTitle,
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                Text(
                  text = strings.lowPowerVisualsSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary.copy(alpha = 0.75f),
                )
              }
              GlassSwitch(
                checked = prefs.lowPowerVisuals,
                onCheckedChange = viewModel::setLowPowerVisuals,
                modifier = Modifier.testTag("low_power_visuals_switch"),
              )
            }
          }
        }

        // 2. REMINDERS DEFAULTS
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingsSectionLabel("Defaults for New Reminders")
          GlassSurface(level = GlassLevel.CARD) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable { showDefaultTimePicker = true }
                    .minimumInteractiveComponentSize()
                    .padding(16.dp)
                    .testTag("default_time_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                  )
                  Column {
                    Text(
                      text = "Date-only time • ${IstTime(prefs.defaultHour, prefs.defaultMinute).format12Hour()} IST",
                      style = MaterialTheme.typography.bodyLarge.tabular(),
                      color = palette.textPrimary,
                    )
                    Text(
                      text = stringResource(R.string.settings_default_time),
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary.copy(alpha = 0.75f),
                    )
                  }
                }
                Text(
                  text = "Change",
                  style = MaterialTheme.typography.labelLarge,
                  color = palette.primaryAccent,
                )
              }

              GlassDivider()

              // Default delivery channel segmented glass pills
              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "Default alert type",
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(CircleShape)
                      .background(palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.14f else 0.55f))
                      .padding(4.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  listOf(
                    Triple(DeliveryChannel.NOTIFICATION, "Notification", "default_channel_notify") to Icons.Outlined.NotificationsNone,
                    Triple(DeliveryChannel.ALARM, "Ringing Alarm", "default_channel_alarm") to Icons.Outlined.Alarm,
                  ).forEach { (meta, icon) ->
                    val (ch, label, tag) = meta
                    val selected = prefs.defaultDeliveryChannel == ch
                    Row(
                      modifier =
                        Modifier.weight(1f)
                          .clip(CircleShape)
                          .background(if (selected) palette.primaryAccent else Color.Transparent)
                          .clickable { viewModel.setDefaultDeliveryChannel(ch) }
                          .minimumInteractiveComponentSize()
                          .padding(horizontal = 10.dp, vertical = 8.dp)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              }

              GlassDivider()

              // Default checked offsets
              Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                  text = "Default checked offsets",
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                DefaultOffsetCheckboxRow(
                  label = "1 day before",
                  checked = prefs.defaultInclude1DayBefore,
                  onCheckedChange = viewModel::setDefaultOffset1DayBefore,
                  testTag = "default_offset_1d",
                )
                DefaultOffsetCheckboxRow(
                  label = "4 hours before",
                  checked = prefs.defaultInclude4HoursBefore,
                  onCheckedChange = viewModel::setDefaultOffset4HoursBefore,
                  testTag = "default_offset_4h",
                )
                DefaultOffsetCheckboxRow(
                  label = "5 minutes before",
                  checked = prefs.defaultInclude5MinutesBefore,
                  onCheckedChange = viewModel::setDefaultOffset5MinutesBefore,
                  testTag = "default_offset_5m",
                )
                DefaultOffsetCheckboxRow(
                  label = "At reminder time (when specific time is set)",
                  checked = prefs.defaultIncludeAtTimeForTimed,
                  onCheckedChange = viewModel::setDefaultAtTimeForTimed,
                  testTag = "default_offset_at_time",
                )
                DefaultOffsetCheckboxRow(
                  label = "8:00 AM fallback on the day",
                  checked = prefs.defaultIncludeFallback8Am,
                  onCheckedChange = viewModel::setDefaultOffsetFallback8Am,
                  testTag = "default_offset_fallback",
                )
              }

              GlassDivider()

              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    text = "Cancel remaining alerts when Done",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = "Stop later offsets for this occurrence once marked Done",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
                GlassSwitch(
                  checked = prefs.cancelRemainingOffsetsOnDone,
                  onCheckedChange = viewModel::setCancelRemainingOffsetsOnDone,
                  modifier = Modifier.testTag("cancel_offsets_on_done_switch"),
                )
              }
            }
          }
        }

        // 3. ALARMS & RELIABILITY
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingsSectionLabel("Alarms & Reliability")
          GlassSurface(level = GlassLevel.CARD) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable { showSoundPicker = true }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("alarm_sound_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                  )
                  Column {
                    Text(
                      text = "Alarm ringtone",
                      style = MaterialTheme.typography.bodyLarge,
                      color = palette.textPrimary,
                    )
                    Text(
                      text = "${prefs.alarmSound.label} • ${prefs.alarmSound.subtitle}",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary.copy(alpha = 0.75f),
                    )
                  }
                }
                GlassIconButton(
                  icon = Icons.Rounded.PlayArrow,
                  contentDescription = "Preview alarm sound",
                  size = 40.dp,
                  tint = palette.primaryAccent,
                  onClick = { soundPlayer.preview(prefs.alarmSound, prefs.vibrationPattern, scope) },
                  modifier = Modifier.testTag("preview_sound_button"),
                )
              }

              GlassDivider()

              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "Vibration pattern",
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  VibrationPatternOption.entries.forEach { option ->
                    GlassChoiceChip(
                      label = option.label,
                      selected = prefs.vibrationPattern == option,
                      onClick = { viewModel.setVibrationPattern(option) },
                      modifier = Modifier.testTag("vibration_${option.name.lowercase()}"),
                    )
                  }
                }
              }

              GlassDivider()

              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "Snooze duration",
                  style = MaterialTheme.typography.bodyLarge,
                  color = palette.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(5, 10, 15, 30).forEach { mins ->
                    GlassChoiceChip(
                      label = "$mins min",
                      selected = prefs.snoozeDurationMinutes == mins,
                      onClick = { viewModel.setSnoozeDurationMinutes(mins) },
                      modifier = Modifier.testTag("snooze_${mins}m"),
                    )
                  }
                }
              }

              GlassDivider()

              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    text = "Alarms bypass Do Not Disturb",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text =
                      if (reliabilityStatus.dndBypassGranted)
                        "Granted by system • alarms ring during DND"
                      else "Tap switch to grant Do Not Disturb access",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
                GlassSwitch(
                  checked = prefs.alarmBypassDnd,
                  onCheckedChange = { checked ->
                    viewModel.setAlarmBypassDnd(checked)
                    if (checked && !PermissionAndOemHelper.isDndPolicyGranted(context)) {
                      PermissionAndOemHelper.openDndAccessSettings(context)
                    }
                  },
                  modifier = Modifier.testTag("bypass_dnd_switch"),
                )
              }

              GlassDivider()

              SettingsNavigationRow(
                icon = Icons.Outlined.VerifiedUser,
                iconTint =
                  if (reliabilityStatus.allCoreChecksPassed) palette.primaryAccent
                  else palette.dangerColor,
                title = "Reliability Check & Phone Setup",
                subtitle =
                  if (reliabilityStatus.allCoreChecksPassed)
                    "Core permissions OK • View ${reliabilityStatus.manufacturerLabel} guide"
                  else "Action needed for reliable alarms & battery setup",
                onClick = onOpenReliabilityCheck,
                modifier = Modifier.testTag("settings_reliability_row"),
              )
            }
          }
        }

        // 4. LANGUAGE, SECURITY & WIDGET
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingsSectionLabel(strings.sectionSecurityAndLanguage)
          GlassSurface(level = GlassLevel.CARD) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                  )
                  Column {
                    Text(
                      text = strings.languageLabel,
                      style = MaterialTheme.typography.bodyLarge,
                      color = palette.textPrimary,
                    )
                    Text(
                      text = strings.languageSubtitle,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary.copy(alpha = 0.75f),
                    )
                  }
                }
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(CircleShape)
                      .background(palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.14f else 0.55f))
                      .padding(4.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  AppLanguage.entries.forEach { language ->
                    val selected = prefs.appLanguage == language
                    Box(
                      modifier =
                        Modifier.weight(1f)
                          .clip(CircleShape)
                          .background(if (selected) palette.primaryAccent else Color.Transparent)
                          .clickable { viewModel.setAppLanguage(language) }
                          .minimumInteractiveComponentSize()
                          .padding(vertical = 8.dp)
                          .testTag("language_${language.tag}"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = language.nativeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) palette.onPrimaryAccent else palette.textPrimary,
                      )
                    }
                  }
                }
              }

              GlassDivider()

              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable { showPinDialog = true }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_app_lock_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                  )
                  Column {
                    Text(
                      text = strings.appLockLabel,
                      style = MaterialTheme.typography.bodyLarge,
                      color = palette.textPrimary,
                    )
                    Text(
                      text =
                        if (prefs.appLockEnabled)
                          strings.appLockEnabledSubtitle(prefs.appLockUseBiometric && biometricAvailable)
                        else strings.appLockDisabledSubtitle,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary.copy(alpha = 0.75f),
                    )
                  }
                }
                GlassSwitch(
                  checked = prefs.appLockEnabled,
                  onCheckedChange = { checked ->
                    if (checked) {
                      showPinDialog = true
                    } else {
                      viewModel.disableAppLock()
                    }
                  },
                  modifier = Modifier.testTag("app_lock_switch"),
                )
              }

              AnimatedVisibility(visible = prefs.appLockEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  GlassDivider()
                  if (biometricAvailable) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = strings.useFingerprintFace,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                      )
                      GlassSwitch(
                        checked = prefs.appLockUseBiometric,
                        onCheckedChange = viewModel::setAppLockUseBiometric,
                        modifier = Modifier.testTag("app_lock_biometric_switch"),
                      )
                    }
                    GlassDivider()
                  }

                  Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = strings.lockTimeoutLabel,
                      style = MaterialTheme.typography.bodyLarge,
                      color = palette.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      listOf(
                        0 to strings.lockTimeoutImmediate,
                        60 to strings.lockTimeout1Min,
                        300 to strings.lockTimeout5Min,
                      ).forEach { (secs, label) ->
                        GlassChoiceChip(
                          label = label,
                          selected = prefs.appLockTimeoutSeconds == secs,
                          onClick = { viewModel.setAppLockTimeoutSeconds(secs) },
                          modifier = Modifier.testTag("lock_timeout_$secs"),
                        )
                      }
                    }
                  }
                }
              }

              GlassDivider()

              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Widgets,
                  contentDescription = null,
                  tint = palette.primaryAccent,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = strings.homeWidgetTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = strings.homeWidgetSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
                TextButton(
                  onClick = {
                    RemindlyWidgetUpdater.requestUpdate(context)
                    val pinned = RemindlyWidgetUpdater.requestPin(context)
                    scope.launch {
                      snackbarHostState.showSnackbar(
                        if (pinned) strings.widgetPinRequested else strings.widgetPinUnsupported
                      )
                    }
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("pin_widget_button"),
                ) {
                  Text(strings.pinWidgetButton, color = palette.primaryAccent)
                }
              }
            }
          }
        }

        // 5. BACKUP & ACTIVITY
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingsSectionLabel(strings.sectionActivityAndRecovery)
          GlassSurface(level = GlassLevel.CARD) {
            Column(modifier = Modifier.fillMaxWidth()) {
              SettingsNavigationRow(
                icon = Icons.Outlined.ImportExport,
                iconTint = palette.primaryAccent,
                title = strings.backupRowTitle,
                subtitle =
                  prefs.lastBackupEpochMillis?.let { ts ->
                    "Last backup: ${IstDate.fromEpochMillis(ts).formatShort()}"
                  } ?: "Save offline backups, share via apps, or restore",
                onClick = onOpenBackupShare,
                modifier = Modifier.testTag("settings_backup_row"),
              )
              GlassDivider()
              SettingsNavigationRow(
                icon = Icons.Outlined.History,
                iconTint = palette.primaryAccent,
                title = strings.historyTitle,
                subtitle =
                  if (historyCount > 0) "$historyCount • ${strings.historySubtitle}"
                  else strings.historySubtitle,
                onClick = onOpenHistory,
                modifier = Modifier.testTag("settings_history_row"),
              )
              GlassDivider()
              SettingsNavigationRow(
                icon = Icons.Outlined.DeleteOutline,
                iconTint = palette.primaryAccent,
                title = strings.recycleBinTitle,
                subtitle =
                  if (deletedCount > 0) strings.recycleBinCountSubtitle(deletedCount)
                  else strings.recycleBinEmptySubtitle,
                badgeCount = if (deletedCount > 0) deletedCount else null,
                onClick = onOpenRecycleBin,
                modifier = Modifier.testTag("settings_recycle_bin_row"),
              )
            }
          }
        }

        // 6. ABOUT & WALKTHROUGH
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingsSectionLabel("About")
          GlassSurface(level = GlassLevel.CARD) {
            Column(modifier = Modifier.fillMaxWidth()) {
              SettingsNavigationRow(
                icon = Icons.Outlined.School,
                iconTint = palette.primaryAccent,
                title = "Replay welcome walkthrough",
                subtitle = "Review Remindly basics and permissions",
                onClick = onReplayOnboarding,
                modifier = Modifier.testTag("replay_walkthrough_row"),
              )
              GlassDivider()
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      hiddenTestTapCount++
                      if (hiddenTestTapCount >= 3) {
                        showHiddenTestSection = true
                      }
                    }
                    .minimumInteractiveComponentSize()
                    .padding(16.dp)
                    .testTag("offline_badge"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Lock,
                  contentDescription = null,
                  tint = palette.primaryAccent,
                  modifier = Modifier.size(20.dp),
                )
                Column {
                  Text(
                    text = stringResource(R.string.settings_offline_badge),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                  )
                  Text(
                    text =
                      if (showHiddenTestSection) "v1.0 • 1-minute test unlocked"
                      else "v1.0 • Tap 3× for 1-minute reliability test",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.72f),
                  )
                }
              }
            }
          }
        }

        // Hidden 1-Minute Reliability Test Panel
        AnimatedVisibility(visible = showHiddenTestSection) {
          GlassSurface(
            level = GlassLevel.CARD,
            tintOverlay = palette.primaryAccent,
            accentBorder = palette.primaryAccent.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth().testTag("hidden_test_panel"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "1-Minute Reliability Test",
                style = MaterialTheme.typography.titleSmall,
                color = palette.primaryAccent,
              )
              Text(
                text =
                  "Schedule a test alert 1 minute from now, then lock your screen to verify delivery.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                GlassPillButton(
                  text = "Notify (1m)",
                  icon = Icons.Outlined.NotificationsActive,
                  isPrimary = false,
                  onClick = {
                    val fireAt = viewModel.scheduleTestInOneMinute(DeliveryChannel.NOTIFICATION)
                    val timeLabel = IstTime.fromEpochMillis(fireAt).format12Hour()
                    scope.launch {
                      snackbarHostState.showSnackbar("Test notification scheduled for $timeLabel IST")
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                  modifier = Modifier.weight(1f).testTag("test_notif_1m"),
                )
                GlassPillButton(
                  text = "Alarm (1m)",
                  icon = Icons.Outlined.Alarm,
                  isPrimary = false,
                  onClick = {
                    val fireAt = viewModel.scheduleTestInOneMinute(DeliveryChannel.ALARM)
                    val timeLabel = IstTime.fromEpochMillis(fireAt).format12Hour()
                    scope.launch {
                      snackbarHostState.showSnackbar("Test alarm scheduled for $timeLabel IST")
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                  modifier = Modifier.weight(1f).testTag("test_alarm_1m"),
                )
              }
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = extraBottomPadding),
      )
    }
  }

  if (showDefaultTimePicker) {
    IstTimePickerDialog(
      initialTime = IstTime(prefs.defaultHour, prefs.defaultMinute),
      title = "Default time (IST)",
      onTimeSelected = viewModel::setDefaultTime,
      onDismiss = { showDefaultTimePicker = false },
    )
  }

  if (showSoundPicker) {
    Dialog(
      onDismissRequest = {
        soundPlayer.stop()
        showSoundPicker = false
      },
    ) {
      GlassSurface(level = GlassLevel.DIALOG, shape = RoundedCornerShape(28.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Alarm ringtone",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Column {
            AlarmSoundOption.entries.forEach { option ->
              val selected = prefs.alarmSound == option
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                      viewModel.setAlarmSound(option)
                      soundPlayer.preview(option, prefs.vibrationPattern, scope)
                    }
                    .minimumInteractiveComponentSize()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                RadioButton(
                  selected = selected,
                  onClick = null,
                  colors =
                    RadioButtonDefaults.colors(
                      selectedColor = palette.primaryAccent,
                      unselectedColor = palette.textSecondary,
                    ),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary.copy(alpha = 0.75f),
                  )
                }
              }
            }
          }
          GlassPillButton(
            text = strings.done,
            isPrimary = true,
            onClick = {
              soundPlayer.stop()
              showSoundPicker = false
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }

  if (showPinDialog) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { showPinDialog = false }) {
      GlassSurface(level = GlassLevel.DIALOG, shape = RoundedCornerShape(28.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = strings.setPinTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )

          GlassPinInputBox(
            label = strings.enterNewPinHint,
            value = pin1,
            onValueChange = { input ->
              if (input.length <= 4 && input.all { it.isDigit() }) {
                pin1 = input
                pinError = null
              }
            },
            modifier = Modifier.testTag("pin_setup_field_1"),
          )

          GlassPinInputBox(
            label = strings.confirmPinHint,
            value = pin2,
            onValueChange = { input ->
              if (input.length <= 4 && input.all { it.isDigit() }) {
                pin2 = input
                pinError = null
              }
            },
            modifier = Modifier.testTag("pin_setup_field_2"),
          )

          if (pinError != null) {
            Text(
              text = pinError.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = palette.dangerColor,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GlassPillButton(
              text = strings.cancel,
              isPrimary = false,
              onClick = { showPinDialog = false },
              modifier = Modifier.weight(1f),
            )
            GlassPillButton(
              text = if (prefs.appLockEnabled) strings.changePin else strings.enableAppLock,
              isPrimary = true,
              onClick = {
                when {
                  pin1.length != 4 -> pinError = strings.pinTooShortError
                  pin1 != pin2 -> pinError = strings.pinMismatchError
                  else -> {
                    viewModel.configurePinLock(
                      pin = pin1,
                      useBiometric = prefs.appLockUseBiometric && biometricAvailable,
                      timeoutSeconds = prefs.appLockTimeoutSeconds,
                    )
                    showPinDialog = false
                  }
                }
              },
              modifier = Modifier.weight(1f).testTag("pin_setup_confirm_button"),
            )
          }

          if (prefs.appLockEnabled) {
            TextButton(
              onClick = {
                viewModel.disableAppLock()
                showPinDialog = false
              },
              modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
              Text(strings.disableAppLock, color = palette.dangerColor)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeMiniPreviewCard(
  mode: AppThemeMode,
  selected: Boolean,
  systemDark: Boolean,
  onClick: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val previewPalette = remember(mode, systemDark) { visualPaletteFor(mode, systemDark) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier =
      Modifier.width(120.dp)
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onClick)
        .padding(2.dp)
        .testTag("theme_mode_${mode.name.lowercase()}"),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(104.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(previewPalette.background)
          .border(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) palette.primaryAccent else palette.glassBorderTop,
            shape = RoundedCornerShape(20.dp),
          ),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(previewPalette.orbPrimary.copy(alpha = 0.65f), Color.Transparent),
              center = Offset(size.width * 0.25f, size.height * 0.28f),
              radius = size.minDimension * 0.58f,
            ),
          center = Offset(size.width * 0.25f, size.height * 0.28f),
          radius = size.minDimension * 0.58f,
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(previewPalette.orbSecondary.copy(alpha = 0.55f), Color.Transparent),
              center = Offset(size.width * 0.80f, size.height * 0.72f),
              radius = size.minDimension * 0.52f,
            ),
          center = Offset(size.width * 0.80f, size.height * 0.72f),
          radius = size.minDimension * 0.52f,
        )
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Mini glass bar + pill
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.width(42.dp)
                .height(8.dp)
                .clip(CircleShape)
                .background(previewPalette.textPrimary.copy(alpha = 0.75f)),
          )
          Box(
            modifier =
              Modifier.size(16.dp)
                .clip(CircleShape)
                .background(previewPalette.primaryAccent),
          )
        }

        // Miniature frosted card
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                previewPalette.cardGlassBase.copy(alpha = if (previewPalette.isDark) 0.20f else 0.78f)
              )
              .border(1.dp, previewPalette.glassBorderTop, RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
              modifier =
                Modifier.fillMaxWidth(0.75f)
                  .height(7.dp)
                  .clip(CircleShape)
                  .background(previewPalette.textPrimary.copy(alpha = 0.85f)),
            )
            Box(
              modifier =
                Modifier.fillMaxWidth(0.45f)
                  .height(5.dp)
                  .clip(CircleShape)
                  .background(previewPalette.primaryAccent.copy(alpha = 0.85f)),
            )
          }
        }
      }
    }

    Text(
      text = mode.label,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      ),
      color = if (selected) palette.primaryAccent else palette.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SettingsSectionLabel(title: String) {
  val palette = LocalRemindlyPalette.current
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.titleSmall,
    color = palette.textSecondary.copy(alpha = 0.72f),
  )
}

@Composable
private fun SettingsNavigationRow(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeCount: Int? = null,
) {
  val palette = LocalRemindlyPalette.current
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = palette.textPrimary,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textSecondary.copy(alpha = 0.75f),
      )
    }
    if (badgeCount != null) {
      Box(
        modifier =
          Modifier.clip(CircleShape)
            .background(palette.primaryAccent.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
      ) {
        Text(
          text = badgeCount.toString(),
          style = MaterialTheme.typography.labelSmall.tabular().copy(fontWeight = FontWeight.Bold),
          color = palette.primaryAccent,
        )
      }
    }
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = palette.textSecondary.copy(alpha = 0.65f),
    )
  }
}

@Composable
private fun GlassChoiceChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  Box(
    modifier =
      modifier
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
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) palette.primaryAccent else palette.textPrimary,
    )
  }
}

@Composable
private fun DefaultOffsetCheckboxRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  val palette = LocalRemindlyPalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { onCheckedChange(!checked) }
        .minimumInteractiveComponentSize()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AnimatedCheckmark(checked = checked, size = 22.dp)
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = palette.textPrimary,
    )
  }
}

@Composable
private fun GlassPinInputBox(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  GlassSurface(level = GlassLevel.CARD, shape = RoundedCornerShape(16.dp)) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = palette.textSecondary.copy(alpha = 0.72f),
      )
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = MaterialTheme.typography.titleMedium.tabular().copy(color = palette.textPrimary),
        cursorBrush = SolidColor(palette.primaryAccent),
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun GlassDivider() {
  val palette = LocalRemindlyPalette.current
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(1.dp)
        .background(palette.glassBorderBottom)
  )
}
