package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.AccentPalette
import com.example.domain.model.AppThemeMode

data class VisualPerformanceConfig(
  val lowPowerVisuals: Boolean = false,
  val reducedMotion: Boolean = false,
)

val LocalIsMonochrome = staticCompositionLocalOf { false }
val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalRemindlyPalette =
  staticCompositionLocalOf { visualPaletteFor(AppThemeMode.AURORA, systemDark = true) }
val LocalVisualPerformance = staticCompositionLocalOf { VisualPerformanceConfig() }

val RemindlyShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

private fun readSystemReducedMotion(context: Context): Boolean =
  runCatching {
    Settings.Global.getFloat(
      context.contentResolver,
      Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    ) == 0f
  }.getOrDefault(false)

private fun readPowerSaveMode(context: Context): Boolean =
  runCatching {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    pm?.isPowerSaveMode == true
  }.getOrDefault(false)

@Composable
fun RemindlyTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  @Suppress("UNUSED_PARAMETER") accentPalette: AccentPalette = AccentPalette.SAGE,
  lowPowerVisuals: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()
  val targetPalette = remember(themeMode, systemDark) { visualPaletteFor(themeMode, systemDark) }

  val systemPowerSave = remember(context) { readPowerSaveMode(context) }
  val systemReducedMotion = remember(context) { readSystemReducedMotion(context) }
  val effectiveLowPower = lowPowerVisuals || systemPowerSave
  val animDuration = if (systemReducedMotion) 0 else 360

  val animBg by animateColorAsState(targetPalette.background, tween(animDuration), label = "theme_bg")
  val animOrb1 by animateColorAsState(targetPalette.orbPrimary, tween(animDuration), label = "theme_orb1")
  val animOrb2 by animateColorAsState(targetPalette.orbSecondary, tween(animDuration), label = "theme_orb2")
  val animOrb3 by animateColorAsState(targetPalette.orbTertiary, tween(animDuration), label = "theme_orb3")
  val animAccent by animateColorAsState(targetPalette.primaryAccent, tween(animDuration), label = "theme_accent")
  val animOnAccent by animateColorAsState(targetPalette.onPrimaryAccent, tween(animDuration), label = "theme_on_accent")
  val animTextPrimary by animateColorAsState(targetPalette.textPrimary, tween(animDuration), label = "theme_text1")
  val animTextSecondary by animateColorAsState(targetPalette.textSecondary, tween(animDuration), label = "theme_text2")

  val animatedPalette =
    targetPalette.copy(
      background = animBg,
      orbPrimary = animOrb1,
      orbSecondary = animOrb2,
      orbTertiary = animOrb3,
      primaryAccent = animAccent,
      onPrimaryAccent = animOnAccent,
      textPrimary = animTextPrimary,
      textSecondary = animTextSecondary,
    )

  val colorScheme = animatedPalette.toMaterialColorScheme()

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !targetPalette.isDark
        controller.isAppearanceLightNavigationBars = !targetPalette.isDark
      }
    }
  }

  CompositionLocalProvider(
    LocalIsMonochrome provides targetPalette.isMonochrome,
    LocalIsDarkTheme provides targetPalette.isDark,
    LocalRemindlyPalette provides animatedPalette,
    LocalVisualPerformance provides
      VisualPerformanceConfig(
        lowPowerVisuals = effectiveLowPower,
        reducedMotion = systemReducedMotion,
      ),
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = RemindlyTypography,
      shapes = RemindlyShapes,
      content = content,
    )
  }
}
