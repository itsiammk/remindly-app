package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class ReliabilityStatus(
  val notificationsEnabled: Boolean,
  val exactAlarmsAllowed: Boolean,
  val fullScreenIntentAllowed: Boolean,
  val batteryUnrestricted: Boolean,
  val dndBypassGranted: Boolean,
  val manufacturerLabel: String,
) {
  val allCoreChecksPassed: Boolean
    get() = notificationsEnabled && exactAlarmsAllowed && fullScreenIntentAllowed
}

data class OemBatteryGuide(
  val id: String,
  val brandTitle: String,
  val uiName: String,
  val steps: List<String>,
)

object PermissionAndOemHelper {

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
    return am.canScheduleExactAlarms()
  }

  fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(NotificationManager::class.java) ?: return false
    return nm.canUseFullScreenIntent()
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun isDndPolicyGranted(context: Context): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java) ?: return false
    return nm.isNotificationPolicyAccessGranted
  }

  fun inspectStatus(context: Context): ReliabilityStatus =
    ReliabilityStatus(
      notificationsEnabled = hasNotificationPermission(context),
      exactAlarmsAllowed = canScheduleExactAlarms(context),
      fullScreenIntentAllowed = canUseFullScreenIntent(context),
      batteryUnrestricted = isIgnoringBatteryOptimizations(context),
      dndBypassGranted = isDndPolicyGranted(context),
      manufacturerLabel = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }.ifBlank { "Android" },
    )

  fun detectedOemGuideId(): String {
    val mfg = Build.MANUFACTURER.orEmpty().lowercase()
    return when {
      "xiaomi" in mfg || "redmi" in mfg || "poco" in mfg -> "xiaomi"
      "realme" in mfg -> "realme"
      "oppo" in mfg -> "oppo"
      "vivo" in mfg || "iqoo" in mfg -> "vivo"
      "oneplus" in mfg -> "oneplus"
      "samsung" in mfg -> "samsung"
      else -> "stock"
    }
  }

  fun oemGuides(): List<OemBatteryGuide> {
    val detected = detectedOemGuideId()
    val all =
      listOf(
        OemBatteryGuide(
          id = "xiaomi",
          brandTitle = "Xiaomi / Redmi / Poco",
          uiName = "MIUI / HyperOS",
          steps =
            listOf(
              "Open App Info -> Battery saver -> Choose \"No restrictions\".",
              "Turn ON \"Autostart\" (or Background autostart) for Remindly.",
              "In Recent Apps, long-press Remindly and tap the Lock icon so MIUI does not swipe-kill it.",
            ),
        ),
        OemBatteryGuide(
          id = "oppo",
          brandTitle = "Oppo",
          uiName = "ColorOS",
          steps =
            listOf(
              "Open App Info -> Battery usage -> Allow background activity & Allow auto-launch.",
              "Disable \"Pause app activity if unused\".",
              "In Recent Apps, tap the menu on Remindly and select \"Lock\".",
            ),
        ),
        OemBatteryGuide(
          id = "realme",
          brandTitle = "Realme",
          uiName = "Realme UI",
          steps =
            listOf(
              "Open App Info -> Battery usage -> Enable \"Allow background activity\" and \"Allow auto-launch\".",
              "Turn OFF \"App quick freeze\" in Phone Manager / Battery settings.",
            ),
        ),
        OemBatteryGuide(
          id = "vivo",
          brandTitle = "Vivo / iQOO",
          uiName = "Funtouch OS",
          steps =
            listOf(
              "Open App Info -> Battery -> Select \"Unrestricted\" (or High background power consumption).",
              "Open iManager -> App manager -> Autostart -> Enable Remindly.",
            ),
        ),
        OemBatteryGuide(
          id = "oneplus",
          brandTitle = "OnePlus",
          uiName = "OxygenOS",
          steps =
            listOf(
              "Open App Info -> Battery usage -> Enable \"Allow background activity\" & \"Allow auto-launch\".",
              "Disable \"Sleep standby optimization\" under Battery -> More settings if alarms delay overnight.",
            ),
        ),
        OemBatteryGuide(
          id = "samsung",
          brandTitle = "Samsung",
          uiName = "One UI",
          steps =
            listOf(
              "Open App Info -> Battery -> Select \"Unrestricted\".",
              "Go to Settings -> Battery -> Background usage limits -> Never auto sleeping apps -> Add Remindly.",
            ),
        ),
        OemBatteryGuide(
          id = "stock",
          brandTitle = "Pixel / Motorola / Nothing / Other",
          uiName = "Stock Android",
          steps =
            listOf(
              "Open App Info -> App battery usage -> Choose \"Unrestricted\".",
              "Keep \"Pause app activity if unused\" turned OFF.",
            ),
        ),
      )
    return all.sortedByDescending { it.id == detected }
  }

  fun openNotificationSettings(context: Context) {
    val intent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
          putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
      } else {
        appDetailsIntent(context)
      }
    launchSafe(context, intent)
  }

  fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = Uri.fromParts("package", context.packageName, null)
        }
      launchSafe(context, intent)
    } else {
      openAppDetails(context)
    }
  }

  fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val intent =
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
          data = Uri.fromParts("package", context.packageName, null)
        }
      launchSafe(context, intent)
    } else {
      openNotificationSettings(context)
    }
  }

  fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    launchSafe(context, intent)
  }

  fun openDndAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    launchSafe(context, intent)
  }

  fun openAppDetails(context: Context) {
    launchSafe(context, appDetailsIntent(context))
  }

  fun openOemAutostartOrAppDetails(context: Context) {
    val candidates =
      listOf(
        Intent().setComponent(
          ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        ),
        Intent().setComponent(
          ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
        ),
        Intent().setComponent(
          ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity")
        ),
        Intent().setComponent(
          ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        ),
        Intent().setComponent(
          ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
        ),
        Intent().setComponent(
          ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
        ),
      )

    for (intent in candidates) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (intent.resolveActivity(context.packageManager) != null) {
        val launched = runCatching { context.startActivity(intent) }.isSuccess
        if (launched) return
      }
    }
    openAppDetails(context)
  }

  private fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
    }

  private fun launchSafe(context: Context, intent: Intent) {
    val prepared =
      intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val ok = runCatching { context.startActivity(prepared) }.isSuccess
    if (!ok) {
      runCatching {
        context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      }
    }
  }
}
