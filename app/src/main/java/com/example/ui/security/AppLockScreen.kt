package com.example.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import com.example.ui.theme.tabular

@Composable
fun AppLockScreen(
  pinHash: String,
  biometricAvailable: Boolean,
  onVerifyPin: (String, String) -> Boolean,
  onRequestBiometric: () -> Unit,
  onUnlocked: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val strings = LocalStrings.current
  val haptic = LocalHapticFeedback.current
  var enteredPin by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }

  LaunchedEffect(biometricAvailable) {
    if (biometricAvailable) {
      onRequestBiometric()
    }
  }

  fun appendDigit(digit: String) {
    if (enteredPin.length >= 4) return
    val next = enteredPin + digit
    enteredPin = next
    showError = false
    if (next.length == 4) {
      if (onVerifyPin(next, pinHash)) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onUnlocked()
      } else {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showError = true
        enteredPin = ""
      }
    }
  }

  AmbientOrbBackground {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 28.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
      ) {
        GlassSurface(
          level = GlassLevel.CARD,
          shape = CircleShape,
          tintOverlay = palette.primaryAccent,
          accentBorder = palette.primaryAccent.copy(alpha = 0.40f),
          modifier = Modifier.size(76.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = strings.lockScreenTitle,
              tint = palette.primaryAccent,
              modifier = Modifier.size(32.dp),
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = strings.lockScreenTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
          )
          Text(
            text = strings.lockScreenSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
          )
        }

        // 4-dot Frosted PIN indicator
        Row(
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(vertical = 6.dp),
        ) {
          repeat(4) { index ->
            val filled = index < enteredPin.length
            Box(
              modifier =
                Modifier.size(16.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      showError -> palette.dangerColor.copy(alpha = 0.45f)
                      filled -> palette.primaryAccent
                      else -> palette.cardGlassBase.copy(alpha = if (palette.isDark) 0.14f else 0.55f)
                    }
                  )
                  .border(
                    width = 1.dp,
                    color =
                      when {
                        showError -> palette.dangerColor
                        filled -> palette.primaryAccent
                        else -> palette.glassBorderTop
                      },
                    shape = CircleShape,
                  ),
            )
          }
        }

        AnimatedVisibility(visible = showError) {
          Text(
            text = strings.lockScreenWrongPin,
            style = MaterialTheme.typography.bodySmall,
            color = palette.dangerColor,
            modifier = Modifier.testTag("app_lock_error"),
          )
        }

        // Tactile Frosted Glass Keypad
        val rows =
          listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
          )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          rows.forEach { rowDigits ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
              rowDigits.forEach { digit ->
                PinDigitKey(
                  label = digit,
                  onClick = { appendDigit(digit) },
                  modifier = Modifier.testTag("pin_key_$digit"),
                )
              }
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (biometricAvailable) {
              GlassSurface(
                level = GlassLevel.CARD,
                shape = CircleShape,
                tintOverlay = palette.primaryAccent,
                accentBorder = palette.primaryAccent.copy(alpha = 0.45f),
                onClick = onRequestBiometric,
                modifier = Modifier.size(72.dp).testTag("pin_key_biometric"),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = strings.unlockWithBiometric,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(30.dp),
                  )
                }
              }
            } else {
              Spacer(modifier = Modifier.size(72.dp))
            }

            PinDigitKey(
              label = "0",
              onClick = { appendDigit("0") },
              modifier = Modifier.testTag("pin_key_0"),
            )

            GlassSurface(
              level = GlassLevel.CARD,
              shape = CircleShape,
              onClick = {
                if (enteredPin.isNotEmpty()) {
                  enteredPin = enteredPin.dropLast(1)
                }
              },
              modifier = Modifier.size(72.dp).testTag("pin_key_backspace"),
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Backspace,
                  contentDescription = "Backspace",
                  tint = palette.textPrimary,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }
        }

        if (biometricAvailable) {
          GlassPillButton(
            text = strings.unlockWithBiometric,
            icon = Icons.Outlined.Fingerprint,
            isPrimary = false,
            onClick = onRequestBiometric,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.testTag("unlock_biometric_button"),
          )
        }
      }
    }
  }
}

@Composable
private fun PinDigitKey(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRemindlyPalette.current
  GlassSurface(
    level = GlassLevel.CARD,
    shape = CircleShape,
    onClick = onClick,
    modifier = modifier.size(72.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall.tabular().copy(fontWeight = FontWeight.SemiBold),
        color = palette.textPrimary,
      )
    }
  }
}
