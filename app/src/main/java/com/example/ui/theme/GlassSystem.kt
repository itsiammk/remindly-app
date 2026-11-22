package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.localization.LocalStrings
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class GlassLevel {
  CARD, // Level 1: 8-12% tint
  BAR, // Level 2: 14-20% tint
  DIALOG, // Level 3: 22-28% tint
}

@Composable
fun AmbientOrbBackground(
  modifier: Modifier = Modifier,
  pulseBoost: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val animateOrbs = !perf.lowPowerVisuals && !perf.reducedMotion

  val infiniteTransition = rememberInfiniteTransition(label = "ambient_orbs")
  val driftPhase by
    if (animateOrbs) {
      infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = if (pulseBoost) 9_000 else 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
        label = "orb_drift_phase",
      )
    } else {
      remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

  val pulseFactor by
    if (animateOrbs && pulseBoost) {
      infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 3_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "orb_pulse_factor",
      )
    } else {
      remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

  Box(modifier = modifier.fillMaxSize().background(palette.background)) {
    val orbBlurModifier =
      if (!perf.lowPowerVisuals && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.fillMaxSize().blur(if (pulseBoost) 38.dp else 28.dp)
      } else {
        Modifier.fillMaxSize()
      }

    Canvas(modifier = orbBlurModifier) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      val primaryAlpha = if (palette.isMonochrome) (if (palette.isDark) 0.45f else 0.48f) else (if (pulseBoost) 0.42f else 0.28f)
      val secondaryAlpha = if (palette.isMonochrome) (if (palette.isDark) 0.35f else 0.40f) else (if (pulseBoost) 0.36f else 0.24f)
      val tertiaryAlpha = if (palette.isMonochrome) (if (palette.isDark) 0.25f else 0.32f) else (if (pulseBoost) 0.28f else 0.18f)

      val c1 =
        Offset(
          x = w * (0.20f + 0.08f * cos(driftPhase)),
          y = h * (0.16f + 0.06f * sin(driftPhase)),
        )
      val r1 = (w.coerceAtLeast(h) * 0.48f) * pulseFactor

      val c2 =
        Offset(
          x = w * (0.82f + 0.09f * sin(driftPhase * 0.8f)),
          y = h * (0.42f + 0.08f * cos(driftPhase * 0.8f)),
        )
      val r2 = (w.coerceAtLeast(h) * 0.44f) * pulseFactor

      val c3 =
        Offset(
          x = w * (0.36f + 0.10f * sin(driftPhase * 1.15f + 1.4f)),
          y = h * (0.84f + 0.06f * cos(driftPhase * 1.15f)),
        )
      val r3 = (w.coerceAtLeast(h) * 0.42f)

      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(palette.orbPrimary.copy(alpha = primaryAlpha), Color.Transparent),
            center = c1,
            radius = r1,
          ),
        center = c1,
        radius = r1,
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(palette.orbSecondary.copy(alpha = secondaryAlpha), Color.Transparent),
            center = c2,
            radius = r2,
          ),
        center = c2,
        radius = r2,
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(palette.orbTertiary.copy(alpha = tertiaryAlpha), Color.Transparent),
            center = c3,
            radius = r3,
          ),
        center = c3,
        radius = r3,
      )
    }

    content()
  }
}

@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  level: GlassLevel = GlassLevel.CARD,
  shape: Shape = RoundedCornerShape(24.dp),
  tintOverlay: Color? = null,
  accentBorder: Color? = null,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by
    animateFloatAsState(
      targetValue = if (onClick != null && isPressed && !perf.reducedMotion) 0.965f else 1f,
      animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
      label = "glass_press_scale",
    )

  val (topAlpha, bottomAlpha, solidFallback, shadowElevation) =
    when (level) {
      GlassLevel.CARD ->
        Quadruple(
          if (palette.isDark) 0.12f else 0.78f,
          if (palette.isDark) 0.07f else 0.56f,
          palette.cardSolidFallback,
          if (palette.isDark) 6.dp else 10.dp,
        )
      GlassLevel.BAR ->
        Quadruple(
          if (palette.isDark) 0.20f else 0.86f,
          if (palette.isDark) 0.14f else 0.68f,
          palette.barSolidFallback,
          if (palette.isDark) 10.dp else 14.dp,
        )
      GlassLevel.DIALOG ->
        Quadruple(
          if (palette.isDark) 0.92f else 0.94f,
          if (palette.isDark) 0.84f else 0.88f,
          palette.dialogSolidFallback,
          18.dp,
        )
    }

  val baseColor =
    when (level) {
      GlassLevel.CARD -> palette.cardGlassBase
      GlassLevel.BAR -> palette.barGlassBase
      GlassLevel.DIALOG -> palette.dialogGlassBase
    }

  val backgroundBrush =
    if (perf.lowPowerVisuals) {
      val fallbackFill =
        if (tintOverlay != null) lerpColor(solidFallback, tintOverlay, 0.16f) else solidFallback
      Brush.verticalGradient(listOf(fallbackFill, fallbackFill))
    } else {
      val cTop =
        if (tintOverlay != null) lerpColor(baseColor.copy(alpha = topAlpha), tintOverlay, 0.22f)
        else baseColor.copy(alpha = topAlpha)
      val cBottom =
        if (tintOverlay != null) lerpColor(baseColor.copy(alpha = bottomAlpha), tintOverlay, 0.16f)
        else baseColor.copy(alpha = bottomAlpha)
      Brush.verticalGradient(listOf(cTop, cBottom))
    }

  val borderBrush =
    if (accentBorder != null) {
      Brush.verticalGradient(listOf(accentBorder.copy(alpha = 0.78f), accentBorder.copy(alpha = 0.30f)))
    } else {
      Brush.verticalGradient(listOf(palette.glassBorderTop, palette.glassBorderBottom))
    }

  val clickableMod =
    if (onClick != null) {
      Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
        },
      )
    } else {
      Modifier
    }

  val shadowColor =
    if (palette.isDark) Color.Black.copy(alpha = 0.42f) else Color(0xFF101828).copy(alpha = 0.10f)

  Box(
    modifier =
      modifier
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .then(
          if (!palette.isMonochrome || !palette.isDark) {
            Modifier.shadow(
              elevation = shadowElevation,
              shape = shape,
              ambientColor = shadowColor,
              spotColor = shadowColor,
            )
          } else {
            Modifier
          }
        )
        .clip(shape)
        .background(backgroundBrush)
        .border(BorderStroke(1.dp, borderBrush), shape)
        .then(clickableMod),
    content = content,
  )
}

@Composable
fun GlassIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = LocalRemindlyPalette.current.textPrimary,
  accentHighlighted: Boolean = false,
  size: Dp = 44.dp,
) {
  val palette = LocalRemindlyPalette.current
  GlassSurface(
    level = GlassLevel.BAR,
    shape = CircleShape,
    tintOverlay = if (accentHighlighted) palette.primaryAccent else null,
    accentBorder = if (accentHighlighted) palette.primaryAccent else null,
    onClick = onClick,
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .size(size)
        .semantics { this.contentDescription = contentDescription },
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (accentHighlighted) palette.primaryAccent else tint,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun GlassPillButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  isPrimary: Boolean = true,
  enabled: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by
    animateFloatAsState(
      targetValue = if (enabled && isPressed && !perf.reducedMotion) 0.96f else 1f,
      animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
      label = "pill_scale",
    )

  val bgColor =
    when {
      !enabled && isPrimary -> palette.primaryAccent.copy(alpha = 0.35f)
      isPrimary -> palette.primaryAccent
      else -> palette.barGlassBase.copy(alpha = if (palette.isDark) 0.16f else 0.72f)
    }
  val contentColor =
    when {
      !enabled && isPrimary -> palette.onPrimaryAccent.copy(alpha = 0.7f)
      isPrimary -> palette.onPrimaryAccent
      else -> palette.textPrimary
    }

  val glowShadow =
    if (isPrimary && enabled && !palette.isMonochrome) {
      Modifier.shadow(
        elevation = 14.dp,
        shape = CircleShape,
        ambientColor = palette.primaryAccent.copy(alpha = 0.45f),
        spotColor = palette.primaryAccent.copy(alpha = 0.45f),
      )
    } else {
      Modifier
    }

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .then(glowShadow)
        .clip(CircleShape)
        .background(bgColor)
        .then(
          if (!isPrimary) {
            Modifier.border(
              BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(palette.glassBorderTop, palette.glassBorderBottom)),
              ),
              CircleShape,
            )
          } else {
            Modifier
          }
        )
        .clickable(
          enabled = enabled,
          role = Role.Button,
          interactionSource = interactionSource,
          indication = null,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
          },
        )
        .padding(contentPadding),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
      )
    }
  }
}

@Composable
fun GlassSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val haptic = LocalHapticFeedback.current

  val trackColor by
    animateColorAsState(
      targetValue =
        if (checked) palette.primaryAccent
        else palette.textSecondary.copy(alpha = if (palette.isDark) 0.25f else 0.18f),
      animationSpec = tween(if (perf.reducedMotion) 0 else 220),
      label = "switch_track",
    )

  val knobColor by
    animateColorAsState(
      targetValue = if (checked) palette.onPrimaryAccent else Color.White,
      animationSpec = tween(if (perf.reducedMotion) 0 else 220),
      label = "switch_knob",
    )

  val knobOffset by
    animateDpAsState(
      targetValue = if (checked) 22.dp else 2.dp,
      animationSpec =
        if (perf.reducedMotion) tween(0)
        else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
      label = "switch_knob_offset",
    )

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(CircleShape)
        .clickable(
          enabled = enabled,
          role = Role.Switch,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
          },
        )
        .alpha(if (enabled) 1f else 0.5f),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.width(50.dp)
          .height(30.dp)
          .clip(CircleShape)
          .background(trackColor)
          .border(
            width = 1.dp,
            color =
              if (checked) palette.primaryAccent.copy(alpha = 0.4f)
              else palette.glassBorderTop.copy(alpha = 0.55f),
            shape = CircleShape,
          ),
      contentAlignment = Alignment.CenterStart,
    ) {
      Box(
        modifier =
          Modifier.offset(x = knobOffset)
            .size(24.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(knobColor),
      )
    }
  }
}

@Composable
fun AnimatedCheckmark(
  checked: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 24.dp,
  checkedFillColor: Color = LocalRemindlyPalette.current.primaryAccent,
  checkmarkColor: Color = LocalRemindlyPalette.current.onPrimaryAccent,
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current

  val progress by
    animateFloatAsState(
      targetValue = if (checked) 1f else 0f,
      animationSpec =
        if (perf.reducedMotion) tween(0)
        else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
      label = "check_draw",
    )

  val fillColor by
    animateColorAsState(
      targetValue =
        if (checked) checkedFillColor
        else palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.08f else 0.45f),
      animationSpec = tween(if (perf.reducedMotion) 0 else 180),
      label = "check_fill",
    )

  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(fillColor)
        .border(
          width = 1.5.dp,
          color = if (checked) checkedFillColor else palette.textSecondary.copy(alpha = 0.45f),
          shape = CircleShape,
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (progress > 0.02f) {
      Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
        val w = this.size.width
        val h = this.size.height
        val fullPath =
          Path().apply {
            moveTo(w * 0.14f, h * 0.54f)
            lineTo(w * 0.40f, h * 0.80f)
            lineTo(w * 0.86f, h * 0.24f)
          }
        val measure = PathMeasure().apply { setPath(fullPath, false) }
        val animatedPath = Path()
        measure.getSegment(0f, measure.length * progress, animatedPath, true)
        drawPath(
          path = animatedPath,
          color = checkmarkColor,
          style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }
    }
  }
}

@Composable
fun SkeletonReminderPlaceholder(modifier: Modifier = Modifier) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val shimmerAlpha by
    if (!perf.lowPowerVisuals && !perf.reducedMotion) {
      rememberInfiniteTransition(label = "skeleton_shimmer").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "shimmer_alpha",
      )
    } else {
      remember { androidx.compose.runtime.mutableFloatStateOf(0.55f) }
    }

  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    repeat(4) {
      GlassSurface(
        level = GlassLevel.CARD,
        modifier = Modifier.fillMaxWidth().height(84.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier.size(44.dp)
                .clip(CircleShape)
                .background(palette.textSecondary.copy(alpha = 0.14f * shimmerAlpha)),
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(0.58f)
                  .height(14.dp)
                  .clip(CircleShape)
                  .background(palette.textSecondary.copy(alpha = 0.18f * shimmerAlpha)),
            )
            Box(
              modifier =
                Modifier.fillMaxWidth(0.38f)
                  .height(11.dp)
                  .clip(CircleShape)
                  .background(palette.textSecondary.copy(alpha = 0.12f * shimmerAlpha)),
            )
          }
          Box(
            modifier =
              Modifier.width(58.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(palette.textSecondary.copy(alpha = 0.12f * shimmerAlpha)),
          )
        }
      }
    }
  }
}

enum class MainNavTab {
  HOME,
  CALENDAR,
  SETTINGS,
}

@Composable
fun FloatingGlassBottomBar(
  selectedTab: MainNavTab,
  onSelectTab: (MainNavTab) -> Unit,
  onAddClick: () -> Unit,
  isMinimized: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  val perf = LocalVisualPerformance.current
  val strings = LocalStrings.current
  val haptic = LocalHapticFeedback.current

  val barScale by
    animateFloatAsState(
      targetValue = if (isMinimized) 0.90f else 1f,
      animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
      label = "bottom_bar_scale",
    )
  val barAlpha by
    animateFloatAsState(
      targetValue = if (isMinimized) 0.72f else 1f,
      animationSpec = tween(if (perf.reducedMotion) 0 else 200),
      label = "bottom_bar_alpha",
    )

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    GlassSurface(
      level = GlassLevel.BAR,
      shape = CircleShape,
      modifier =
        Modifier.widthIn(max = 460.dp)
          .fillMaxWidth()
          .height(68.dp)
          .graphicsLayer {
            scaleX = barScale
            scaleY = barScale
            alpha = barAlpha
          },
    ) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Left tabs: Home & Calendar with sliding indicator
        val leftTabs = listOf(MainNavTab.HOME, MainNavTab.CALENDAR)
        BoxWithConstraints(
          modifier = Modifier.weight(2f).height(52.dp),
        ) {
          val slotWidth = maxWidth / 2f
          val showLeftIndicator = selectedTab == MainNavTab.HOME || selectedTab == MainNavTab.CALENDAR
          val indicatorTargetX =
            when (selectedTab) {
              MainNavTab.HOME -> 0.dp
              MainNavTab.CALENDAR -> slotWidth
              MainNavTab.SETTINGS -> 0.dp
            }
          val indicatorX by
            animateDpAsState(
              targetValue = indicatorTargetX,
              animationSpec =
                if (perf.reducedMotion) tween(0)
                else spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
              label = "nav_pill_left",
            )
          if (showLeftIndicator) {
            Box(
              modifier =
                Modifier.offset { IntOffset(indicatorX.roundToPx(), 0) }
                  .width(slotWidth)
                  .height(52.dp)
                  .padding(3.dp)
                  .clip(CircleShape)
                  .background(palette.primaryAccent.copy(alpha = 0.16f))
                  .border(1.dp, palette.primaryAccent.copy(alpha = 0.30f), CircleShape),
            )
          }

          Row(modifier = Modifier.fillMaxSize()) {
            leftTabs.forEach { tab ->
              BottomNavItem(
                icon = if (tab == MainNavTab.HOME) Icons.Rounded.Home else Icons.Rounded.CalendarMonth,
                label = if (tab == MainNavTab.HOME) strings.navHome else strings.navCalendar,
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                modifier =
                  Modifier.weight(1f)
                    .height(52.dp)
                    .testTag(if (tab == MainNavTab.HOME) "nav_home" else "nav_calendar"),
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Center elevated glowing + Add button
        Box(
          modifier =
            Modifier.size(54.dp)
              .minimumInteractiveComponentSize()
              .then(
                if (!palette.isMonochrome) {
                  Modifier.shadow(
                    elevation = 14.dp,
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
              .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAddClick()
              }
              .testTag("fab_add_reminder"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = strings.addReminder,
            tint = palette.onPrimaryAccent,
            modifier = Modifier.size(26.dp),
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Right tab: Settings
        Box(
          modifier = Modifier.weight(1f).height(52.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (selectedTab == MainNavTab.SETTINGS) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .padding(3.dp)
                  .clip(CircleShape)
                  .background(palette.primaryAccent.copy(alpha = 0.16f))
                  .border(1.dp, palette.primaryAccent.copy(alpha = 0.30f), CircleShape),
            )
          }
          BottomNavItem(
            icon = Icons.Rounded.Settings,
            label = strings.navSettings,
            selected = selectedTab == MainNavTab.SETTINGS,
            onClick = { onSelectTab(MainNavTab.SETTINGS) },
            modifier = Modifier.fillMaxSize().testTag("nav_settings"),
          )
        }
      }
    }
  }
}

@Composable
private fun BottomNavItem(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  val haptic = LocalHapticFeedback.current
  val contentColor by
    animateColorAsState(
      targetValue = if (selected) palette.primaryAccent else palette.textSecondary,
      label = "nav_item_color",
    )

  Column(
    modifier =
      modifier
        .clip(CircleShape)
        .clickable {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
        }
        .padding(horizontal = 4.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = contentColor,
      modifier = Modifier.size(21.dp),
    )
    Text(
      text = label,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        ),
      color = contentColor,
      maxLines = 1,
    )
  }
}

private data class Quadruple<A, B, C, D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
)

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
  val t = fraction.coerceIn(0f, 1f)
  return Color(
    red = start.red + (end.red - start.red) * t,
    green = start.green + (end.green - start.green) * t,
    blue = start.blue + (end.blue - start.blue) * t,
    alpha = start.alpha + (end.alpha - start.alpha) * t,
  )
}
