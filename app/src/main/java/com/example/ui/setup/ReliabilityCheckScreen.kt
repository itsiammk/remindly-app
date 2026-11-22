package com.example.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturbOff
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.R
import com.example.alarm.OemBatteryGuide
import com.example.alarm.PermissionAndOemHelper
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette

@Composable
fun ReliabilityCheckScreen(
  activeRemindersCount: Int,
  fromOnboarding: Boolean,
  onNavigateBack: () -> Unit,
  onContinueToHome: () -> Unit,
) {
  val context = LocalContext.current
  val palette = LocalRemindlyPalette.current
  var refreshTick by remember { mutableIntStateOf(0) }
  var status by remember(refreshTick) { mutableStateOf(PermissionAndOemHelper.inspectStatus(context)) }
  val guides = remember { PermissionAndOemHelper.oemGuides() }
  val detectedGuideId = remember { PermissionAndOemHelper.detectedOemGuideId() }
  var expandedGuideId by remember { mutableStateOf<String?>(detectedGuideId) }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    refreshTick++
  }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      refreshTick++
      if (!granted) {
        PermissionAndOemHelper.openNotificationSettings(context)
      }
    }

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
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (!fromOnboarding) {
            GlassIconButton(
              icon = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.nav_back),
              size = 42.dp,
              onClick = onNavigateBack,
              modifier = Modifier.testTag("reliability_back_button"),
            )
          }
          Text(
            text = if (fromOnboarding) "Reliability Setup" else "Reliability Check",
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
        }

        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Summary Banner
          val bannerAccent =
            if (status.allCoreChecksPassed) palette.primaryAccent else palette.dangerColor
          GlassSurface(
            level = GlassLevel.CARD,
            tintOverlay = bannerAccent,
            accentBorder = bannerAccent.copy(alpha = 0.45f),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Icon(
                imageVector =
                  if (status.allCoreChecksPassed) Icons.Outlined.CheckCircle
                  else Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = bannerAccent,
                modifier = Modifier.size(30.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    if (status.allCoreChecksPassed) "Core alarm permissions are ready"
                    else "Action needed for guaranteed alerts",
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                )
                Text(
                  text =
                    "$activeRemindersCount active reminder(s) • Device: ${status.manufacturerLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary.copy(alpha = 0.80f),
                )
              }
            }
          }

          Text(
            text = "PERMISSIONS & SYSTEM CHECKS",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary.copy(alpha = 0.72f),
          )

          PermissionCheckCard(
            icon = Icons.Outlined.NotificationsActive,
            title = "Notifications",
            explanation = "Lets Remindly show quiet alerts and Snooze/Done action buttons.",
            isOk = status.notificationsEnabled,
            okLabel = "Allowed",
            fixLabel = "Allow",
            onFix = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                PermissionAndOemHelper.openNotificationSettings(context)
              }
            },
            tag = "check_notifications",
          )

          PermissionCheckCard(
            icon = Icons.Outlined.Alarm,
            title = "Exact Alarms",
            explanation = "Wakes your phone at the exact minute, even during Doze or Battery Saver.",
            isOk = status.exactAlarmsAllowed,
            okLabel = "Allowed",
            fixLabel = "Enable",
            onFix = { PermissionAndOemHelper.openExactAlarmSettings(context) },
            tag = "check_exact_alarms",
          )

          PermissionCheckCard(
            icon = Icons.Outlined.Fullscreen,
            title = "Lock-Screen Alarm Display",
            explanation = "Shows ringing alarms over the lock screen with big Snooze and Done buttons.",
            isOk = status.fullScreenIntentAllowed,
            okLabel = "Allowed",
            fixLabel = "Enable",
            onFix = { PermissionAndOemHelper.openFullScreenIntentSettings(context) },
            tag = "check_fullscreen",
          )

          PermissionCheckCard(
            icon = Icons.Outlined.BatteryChargingFull,
            title = "Battery Optimization",
            explanation =
              "Set Remindly to Unrestricted so the phone never kills scheduled alarms in the background.",
            isOk = status.batteryUnrestricted,
            okLabel = "Unrestricted",
            fixLabel = "Configure",
            onFix = { PermissionAndOemHelper.openBatteryOptimizationSettings(context) },
            tag = "check_battery",
          )

          PermissionCheckCard(
            icon = Icons.Outlined.DoNotDisturbOff,
            title = "Do Not Disturb Bypass (Optional)",
            explanation =
              "Allows ringing alarms to sound even while Do Not Disturb is active.",
            isOk = status.dndBypassGranted,
            okLabel = "Granted",
            fixLabel = "Grant",
            onFix = { PermissionAndOemHelper.openDndAccessSettings(context) },
            tag = "check_dnd",
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "PHONE BRAND RELIABILITY GUIDE",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary.copy(alpha = 0.72f),
          )
          Text(
            text =
              "Many Indian Android phones aggressively stop background apps. Follow the quick steps for your phone so alarms never fail.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary.copy(alpha = 0.78f),
          )

          guides.forEach { guide ->
            OemGuideCard(
              guide = guide,
              isDetectedPhone = guide.id == detectedGuideId,
              expanded = expandedGuideId == guide.id,
              onToggle = {
                expandedGuideId = if (expandedGuideId == guide.id) null else guide.id
              },
              onOpenSettings = { PermissionAndOemHelper.openOemAutostartOrAppDetails(context) },
            )
          }

          Spacer(modifier = Modifier.height(20.dp))
        }

        if (fromOnboarding) {
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
              GlassPillButton(
                text = "Continue to Remindly",
                isPrimary = true,
                onClick = onContinueToHome,
                modifier =
                  Modifier.fillMaxWidth()
                    .height(52.dp)
                    .testTag("reliability_continue_button"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionCheckCard(
  icon: ImageVector,
  title: String,
  explanation: String,
  isOk: Boolean,
  okLabel: String,
  fixLabel: String,
  onFix: () -> Unit,
  tag: String,
) {
  val palette = LocalRemindlyPalette.current
  val accent = if (isOk) palette.primaryAccent else palette.dangerColor

  GlassSurface(
    level = GlassLevel.CARD,
    tintOverlay = if (!isOk) palette.dangerColor else null,
    accentBorder = if (!isOk) palette.dangerColor.copy(alpha = 0.45f) else null,
    modifier = Modifier.fillMaxWidth().testTag(tag),
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
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(20.dp),
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = explanation,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary.copy(alpha = 0.78f),
        )
      }

      if (isOk) {
        Box(
          modifier =
            Modifier.clip(CircleShape)
              .background(palette.primaryAccent.copy(alpha = 0.16f))
              .border(1.dp, palette.primaryAccent.copy(alpha = 0.28f), CircleShape)
              .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
          Text(
            text = okLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.primaryAccent,
          )
        }
      } else {
        GlassPillButton(
          text = fixLabel,
          isPrimary = true,
          onClick = onFix,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun OemGuideCard(
  guide: OemBatteryGuide,
  isDetectedPhone: Boolean,
  expanded: Boolean,
  onToggle: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current

  GlassSurface(
    level = GlassLevel.CARD,
    tintOverlay = if (isDetectedPhone) palette.primaryAccent else null,
    accentBorder = if (isDetectedPhone) palette.primaryAccent.copy(alpha = 0.50f) else null,
    modifier = Modifier.fillMaxWidth().testTag("oem_guide_${guide.id}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable(onClick = onToggle)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.PhoneAndroid,
          contentDescription = null,
          tint = palette.primaryAccent,
          modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = guide.brandTitle,
              style = MaterialTheme.typography.titleMedium,
              color = palette.textPrimary,
            )
            if (isDetectedPhone) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(palette.primaryAccent.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "Your phone",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.primaryAccent,
                )
              }
            }
          }
          Text(
            text = guide.uiName,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary.copy(alpha = 0.78f),
          )
        }

        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = if (expanded) "Collapse" else "Expand",
          tint = palette.textSecondary,
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          guide.steps.forEachIndexed { idx, step ->
            Text(
              text = "${idx + 1}. $step",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textPrimary,
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          GlassPillButton(
            text = "Open Autostart / App Info",
            isPrimary = false,
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}
