package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.domain.model.AccentPalette
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ReminderCategory

data class RemindlyVisualPalette(
  val resolvedMode: AppThemeMode,
  val isDark: Boolean,
  val isMonochrome: Boolean,
  val background: Color,
  val orbPrimary: Color,
  val orbSecondary: Color,
  val orbTertiary: Color,
  val primaryAccent: Color,
  val onPrimaryAccent: Color,
  val primarySoft: Color,
  val onPrimarySoft: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val cardGlassBase: Color,
  val barGlassBase: Color,
  val dialogGlassBase: Color,
  val cardSolidFallback: Color,
  val barSolidFallback: Color,
  val dialogSolidFallback: Color,
  val glassBorderTop: Color,
  val glassBorderBottom: Color,
  val successColor: Color,
  val onSuccessColor: Color,
  val warningColor: Color,
  val dangerColor: Color,
  val onDangerColor: Color,
  val categoryTints: Map<ReminderCategory, Pair<Color, Color>>,
)

fun resolveThemeMode(
  mode: AppThemeMode,
  systemDark: Boolean,
): AppThemeMode =
  when (mode) {
    AppThemeMode.SYSTEM -> if (systemDark) AppThemeMode.AURORA else AppThemeMode.MONOCHROME_LIGHT
    else -> mode
  }

fun visualPaletteFor(
  mode: AppThemeMode,
  systemDark: Boolean,
): RemindlyVisualPalette {
  val resolved = resolveThemeMode(mode, systemDark)
  return when (resolved) {
    AppThemeMode.MONOCHROME_LIGHT, AppThemeMode.SYSTEM ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.MONOCHROME_LIGHT,
        isDark = false,
        isMonochrome = true,
        background = Color(0xFFF4F5F7),
        orbPrimary = Color(0xFFDDE1E8),
        orbSecondary = Color(0xFFE7EAF0),
        orbTertiary = Color(0xFFD4D8E0),
        primaryAccent = Color(0xFF111216),
        onPrimaryAccent = Color(0xFFFFFFFF),
        primarySoft = Color(0xFFE4E6EB),
        onPrimarySoft = Color(0xFF111216),
        textPrimary = Color(0xFF0E1014),
        textSecondary = Color(0xFF4F545F),
        cardGlassBase = Color(0xFFFFFFFF),
        barGlassBase = Color(0xFFFFFFFF),
        dialogGlassBase = Color(0xFFFFFFFF),
        cardSolidFallback = Color(0xFFFCFCFD),
        barSolidFallback = Color(0xFFF9FAFB),
        dialogSolidFallback = Color(0xFFFFFFFF),
        glassBorderTop = Color(0xFFFFFFFF),
        glassBorderBottom = Color(0x220E1014),
        successColor = Color(0xFF176B45),
        onSuccessColor = Color(0xFFFFFFFF),
        warningColor = Color(0xFFA05B00),
        dangerColor = Color(0xFFB3261E),
        onDangerColor = Color(0xFFFFFFFF),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x140E1014) to Color(0xFF1A1D24)),
            ReminderCategory.ANNIVERSARY to (Color(0x140E1014) to Color(0xFF1A1D24)),
            ReminderCategory.BILL to (Color(0x140E1014) to Color(0xFF1A1D24)),
            ReminderCategory.MEDICINE to (Color(0x140E1014) to Color(0xFF1A1D24)),
            ReminderCategory.OTHER to (Color(0x140E1014) to Color(0xFF1A1D24)),
          ),
      )

    AppThemeMode.MONOCHROME_BLACK ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.MONOCHROME_BLACK,
        isDark = true,
        isMonochrome = true,
        background = Color(0xFF000000),
        orbPrimary = Color(0xFF1C1C22),
        orbSecondary = Color(0xFF121216),
        orbTertiary = Color(0xFF202028),
        primaryAccent = Color(0xFFFFFFFF),
        onPrimaryAccent = Color(0xFF000000),
        primarySoft = Color(0xFF26262C),
        onPrimarySoft = Color(0xFFFFFFFF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFB4B6BE),
        cardGlassBase = Color(0xFFFFFFFF),
        barGlassBase = Color(0xFFFFFFFF),
        dialogGlassBase = Color(0xFFFFFFFF),
        cardSolidFallback = Color(0xFF121215),
        barSolidFallback = Color(0xFF18181D),
        dialogSolidFallback = Color(0xFF1E1E24),
        glassBorderTop = Color(0x3DFFFFFF),
        glassBorderBottom = Color(0x14FFFFFF),
        successColor = Color(0xFF4ADE80),
        onSuccessColor = Color(0xFF032312),
        warningColor = Color(0xFFFBBF24),
        dangerColor = Color(0xFFFF6B6B),
        onDangerColor = Color(0xFF2B0000),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x1FFFFFFF) to Color(0xFFFFFFFF)),
            ReminderCategory.ANNIVERSARY to (Color(0x1FFFFFFF) to Color(0xFFFFFFFF)),
            ReminderCategory.BILL to (Color(0x1FFFFFFF) to Color(0xFFFFFFFF)),
            ReminderCategory.MEDICINE to (Color(0x1FFFFFFF) to Color(0xFFFFFFFF)),
            ReminderCategory.OTHER to (Color(0x1FFFFFFF) to Color(0xFFFFFFFF)),
          ),
      )

    AppThemeMode.AURORA ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.AURORA,
        isDark = true,
        isMonochrome = false,
        background = Color(0xFF070B19),
        orbPrimary = Color(0xFF7B5CFF),
        orbSecondary = Color(0xFF1CE0C4),
        orbTertiary = Color(0xFF3A66FF),
        primaryAccent = Color(0xFF9B87FF),
        onPrimaryAccent = Color(0xFF0C0824),
        primarySoft = Color(0xFF221D4A),
        onPrimarySoft = Color(0xFFE7E0FF),
        textPrimary = Color(0xFFF3F6FF),
        textSecondary = Color(0xFFA7B0CE),
        cardGlassBase = Color(0xFFE6EBFF),
        barGlassBase = Color(0xFFE6EBFF),
        dialogGlassBase = Color(0xFF141B36),
        cardSolidFallback = Color(0xFF12182E),
        barSolidFallback = Color(0xFF18203B),
        dialogSolidFallback = Color(0xFF1D2647),
        glassBorderTop = Color(0x47FFFFFF),
        glassBorderBottom = Color(0x149B87FF),
        successColor = Color(0xFF34D399),
        onSuccessColor = Color(0xFF042619),
        warningColor = Color(0xFFFBBF24),
        dangerColor = Color(0xFFFF7A8A),
        onDangerColor = Color(0xFF380610),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x2EFF7AC6) to Color(0xFFFFB2DF)),
            ReminderCategory.ANNIVERSARY to (Color(0x2EA78BFA) to Color(0xFFDDD6FE)),
            ReminderCategory.BILL to (Color(0x2E38BDF8) to Color(0xFFBAE6FD)),
            ReminderCategory.MEDICINE to (Color(0x2E2DD4BF) to Color(0xFF99F6E4)),
            ReminderCategory.OTHER to (Color(0x2E818CF8) to Color(0xFFC7D2FE)),
          ),
      )

    AppThemeMode.SUNSET ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.SUNSET,
        isDark = true,
        isMonochrome = false,
        background = Color(0xFF140B0D),
        orbPrimary = Color(0xFFFF6B5B),
        orbSecondary = Color(0xFFFFB347),
        orbTertiary = Color(0xFFB5416C),
        primaryAccent = Color(0xFFFF966B),
        onPrimaryAccent = Color(0xFF2A0C04),
        primarySoft = Color(0xFF3D1D1A),
        onPrimarySoft = Color(0xFFFFE2D4),
        textPrimary = Color(0xFFFFF5F2),
        textSecondary = Color(0xFFD1B3AC),
        cardGlassBase = Color(0xFFFFECE6),
        barGlassBase = Color(0xFFFFECE6),
        dialogGlassBase = Color(0xFF281617),
        cardSolidFallback = Color(0xFF221315),
        barSolidFallback = Color(0xFF2B181A),
        dialogSolidFallback = Color(0xFF331D1F),
        glassBorderTop = Color(0x47FFF0E8),
        glassBorderBottom = Color(0x1AFF966B),
        successColor = Color(0xFF6EE7B7),
        onSuccessColor = Color(0xFF05271A),
        warningColor = Color(0xFFFCD34D),
        dangerColor = Color(0xFFFF7A7A),
        onDangerColor = Color(0xFF360606),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x30FF758F) to Color(0xFFFFBCC8)),
            ReminderCategory.ANNIVERSARY to (Color(0x30F472B6) to Color(0xFFFBCFE8)),
            ReminderCategory.BILL to (Color(0x30FBBF24) to Color(0xFFFDE68A)),
            ReminderCategory.MEDICINE to (Color(0x304ADE80) to Color(0xFFBBF7D0)),
            ReminderCategory.OTHER to (Color(0x30FB923C) to Color(0xFFFED7AA)),
          ),
      )

    AppThemeMode.FOREST ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.FOREST,
        isDark = true,
        isMonochrome = false,
        background = Color(0xFF06110C),
        orbPrimary = Color(0xFF10B981),
        orbSecondary = Color(0xFF6EE7B7),
        orbTertiary = Color(0xFF059669),
        primaryAccent = Color(0xFF4BE0A8),
        onPrimaryAccent = Color(0xFF022618),
        primarySoft = Color(0xFF113526),
        onPrimarySoft = Color(0xFFD1FAE5),
        textPrimary = Color(0xFFF0FDF6),
        textSecondary = Color(0xFFA2C9B7),
        cardGlassBase = Color(0xFFE6FFF3),
        barGlassBase = Color(0xFFE6FFF3),
        dialogGlassBase = Color(0xFF0E231A),
        cardSolidFallback = Color(0xFF0D1E17),
        barSolidFallback = Color(0xFF12281F),
        dialogSolidFallback = Color(0xFF163025),
        glassBorderTop = Color(0x42D8FFF0),
        glassBorderBottom = Color(0x1A4BE0A8),
        successColor = Color(0xFF6EE7B7),
        onSuccessColor = Color(0xFF032518),
        warningColor = Color(0xFFFBBF24),
        dangerColor = Color(0xFFFF8080),
        onDangerColor = Color(0xFF380606),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x2EF472B6) to Color(0xFFFBCFE8)),
            ReminderCategory.ANNIVERSARY to (Color(0x2E86EFAC) to Color(0xFFDCFCE7)),
            ReminderCategory.BILL to (Color(0x2E38BDF8) to Color(0xFFBAE6FD)),
            ReminderCategory.MEDICINE to (Color(0x2E34D399) to Color(0xFFA7F3D0)),
            ReminderCategory.OTHER to (Color(0x2EA7F3D0) to Color(0xFFD1FAE5)),
          ),
      )

    AppThemeMode.OCEAN ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.OCEAN,
        isDark = true,
        isMonochrome = false,
        background = Color(0xFF050D18),
        orbPrimary = Color(0xFF22D3EE),
        orbSecondary = Color(0xFF6366F1),
        orbTertiary = Color(0xFF0284C7),
        primaryAccent = Color(0xFF4DD6FA),
        onPrimaryAccent = Color(0xFF042334),
        primarySoft = Color(0xFF122C47),
        onPrimarySoft = Color(0xFFD8F5FF),
        textPrimary = Color(0xFFF0F8FF),
        textSecondary = Color(0xFFA0BED6),
        cardGlassBase = Color(0xFFE5F6FF),
        barGlassBase = Color(0xFFE5F6FF),
        dialogGlassBase = Color(0xFF0E1E33),
        cardSolidFallback = Color(0xFF0C1829),
        barSolidFallback = Color(0xFF112138),
        dialogSolidFallback = Color(0xFF152945),
        glassBorderTop = Color(0x47E0F6FF),
        glassBorderBottom = Color(0x1A4DD6FA),
        successColor = Color(0xFF34D399),
        onSuccessColor = Color(0xFF032317),
        warningColor = Color(0xFFFBBF24),
        dangerColor = Color(0xFFFF7D87),
        onDangerColor = Color(0xFF38050A),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x2EF472B6) to Color(0xFFFBCFE8)),
            ReminderCategory.ANNIVERSARY to (Color(0x2E818CF8) to Color(0xFFC7D2FE)),
            ReminderCategory.BILL to (Color(0x2E38BDF8) to Color(0xFFBAE6FD)),
            ReminderCategory.MEDICINE to (Color(0x2E2DD4BF) to Color(0xFF99F6E4)),
            ReminderCategory.OTHER to (Color(0x2E60A5FA) to Color(0xFFBFDBFE)),
          ),
      )

    AppThemeMode.ROSE ->
      RemindlyVisualPalette(
        resolvedMode = AppThemeMode.ROSE,
        isDark = false,
        isMonochrome = false,
        background = Color(0xFFFDF2F5),
        orbPrimary = Color(0xFFFDA4AF),
        orbSecondary = Color(0xFFFED7AA),
        orbTertiary = Color(0xFFF9A8D4),
        primaryAccent = Color(0xFFBE185D),
        onPrimaryAccent = Color(0xFFFFFFFF),
        primarySoft = Color(0xFFFCE0EB),
        onPrimarySoft = Color(0xFF5D0D2E),
        textPrimary = Color(0xFF28111A),
        textSecondary = Color(0xFF6B4755),
        cardGlassBase = Color(0xFFFFFFFF),
        barGlassBase = Color(0xFFFFFFFF),
        dialogGlassBase = Color(0xFFFFFFFF),
        cardSolidFallback = Color(0xFFFFF9FA),
        barSolidFallback = Color(0xFFFFF4F6),
        dialogSolidFallback = Color(0xFFFFFFFF),
        glassBorderTop = Color(0xFFFFFFFF),
        glassBorderBottom = Color(0x24BE185D),
        successColor = Color(0xFF147249),
        onSuccessColor = Color(0xFFFFFFFF),
        warningColor = Color(0xFF9A5000),
        dangerColor = Color(0xFFB31D3B),
        onDangerColor = Color(0xFFFFFFFF),
        categoryTints =
          mapOf(
            ReminderCategory.BIRTHDAY to (Color(0x1FE11D48) to Color(0xFF9F1239)),
            ReminderCategory.ANNIVERSARY to (Color(0x1FA21CAF) to Color(0xFF701A75)),
            ReminderCategory.BILL to (Color(0x1F0369A1) to Color(0xFF075985)),
            ReminderCategory.MEDICINE to (Color(0x1F0F766E) to Color(0xFF115E59)),
            ReminderCategory.OTHER to (Color(0x1F9333EA) to Color(0xFF6B21A8)),
          ),
      )
  }
}

fun RemindlyVisualPalette.toMaterialColorScheme(): ColorScheme =
  if (isDark) {
    darkColorScheme(
      primary = primaryAccent,
      onPrimary = onPrimaryAccent,
      primaryContainer = primarySoft,
      onPrimaryContainer = onPrimarySoft,
      secondary = primaryAccent.copy(alpha = 0.85f),
      onSecondary = onPrimaryAccent,
      secondaryContainer = cardSolidFallback,
      onSecondaryContainer = textPrimary,
      tertiary = orbSecondary,
      onTertiary = onPrimaryAccent,
      background = background,
      onBackground = textPrimary,
      surface = background,
      onSurface = textPrimary,
      surfaceVariant = cardSolidFallback,
      onSurfaceVariant = textSecondary,
      surfaceContainerLowest = background,
      surfaceContainerLow = cardSolidFallback,
      surfaceContainer = barSolidFallback,
      surfaceContainerHigh = dialogSolidFallback,
      surfaceContainerHighest = dialogSolidFallback,
      outline = glassBorderTop.copy(alpha = 0.35f),
      outlineVariant = glassBorderTop.copy(alpha = 0.20f),
      error = dangerColor,
      onError = onDangerColor,
      errorContainer = dangerColor.copy(alpha = 0.22f),
      onErrorContainer = textPrimary,
    )
  } else {
    lightColorScheme(
      primary = primaryAccent,
      onPrimary = onPrimaryAccent,
      primaryContainer = primarySoft,
      onPrimaryContainer = onPrimarySoft,
      secondary = textSecondary,
      onSecondary = Color.White,
      secondaryContainer = barSolidFallback,
      onSecondaryContainer = textPrimary,
      tertiary = primaryAccent,
      onTertiary = onPrimaryAccent,
      background = background,
      onBackground = textPrimary,
      surface = background,
      onSurface = textPrimary,
      surfaceVariant = barSolidFallback,
      onSurfaceVariant = textSecondary,
      surfaceContainerLowest = Color.White,
      surfaceContainerLow = cardSolidFallback,
      surfaceContainer = barSolidFallback,
      surfaceContainerHigh = dialogSolidFallback,
      surfaceContainerHighest = Color.White,
      outline = textSecondary.copy(alpha = 0.30f),
      outlineVariant = textSecondary.copy(alpha = 0.16f),
      error = dangerColor,
      onError = onDangerColor,
      errorContainer = dangerColor.copy(alpha = 0.14f),
      onErrorContainer = dangerColor,
    )
  }

fun accentColorArgbFor(mode: AppThemeMode, isSystemDark: Boolean = false): Int =
  visualPaletteFor(mode, isSystemDark).primaryAccent.toArgb()

// Legacy helpers preserved for compatibility
fun lightColorSchemeFor(@Suppress("UNUSED_PARAMETER") palette: AccentPalette): ColorScheme =
  visualPaletteFor(AppThemeMode.MONOCHROME_LIGHT, false).toMaterialColorScheme()

fun darkColorSchemeFor(@Suppress("UNUSED_PARAMETER") palette: AccentPalette): ColorScheme =
  visualPaletteFor(AppThemeMode.AURORA, true).toMaterialColorScheme()

fun accentSwatchColor(palette: AccentPalette): Color =
  when (palette) {
    AccentPalette.SAGE -> Color(0xFF10B981)
    AccentPalette.OCEAN -> Color(0xFF38BDF8)
    AccentPalette.TERRACOTTA -> Color(0xFFFF8C61)
    AccentPalette.LAVENDER -> Color(0xFF9B87FF)
  }
