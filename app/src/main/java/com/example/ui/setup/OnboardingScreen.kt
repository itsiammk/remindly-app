package com.example.ui.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmbientOrbBackground
import com.example.ui.theme.GlassLevel
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalRemindlyPalette
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val icon: ImageVector,
  val title: String,
  val body: String,
  val badge: String,
)

private val ONBOARDING_PAGES =
  listOf(
    OnboardingPage(
      icon = Icons.Outlined.EventAvailable,
      title = "Never miss what matters",
      body =
        "Track birthdays, anniversaries, bills, medicines, and daily habits in seconds—always using Indian Standard Time (IST).",
      badge = "Simple & Effortless",
    ),
    OnboardingPage(
      icon = Icons.Outlined.Lock,
      title = "100% offline & private",
      body =
        "All reminders stay strictly on your phone. No accounts, no cloud sync, no ads, and no internet permission.",
      badge = "Your Data Only",
    ),
    OnboardingPage(
      icon = Icons.Outlined.Alarm,
      title = "Quiet alerts or ringing alarms",
      body =
        "Choose gentle notifications or full-screen lock-screen alarms that ring on the alarm volume—even after phone restarts.",
      badge = "Built for Reliability",
    ),
  )

@Composable
fun OnboardingScreen(
  onFinishToPermissions: () -> Unit,
  onSkipToHome: () -> Unit,
) {
  val palette = LocalRemindlyPalette.current
  val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
  val scope = rememberCoroutineScope()
  val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.lastIndex

  AmbientOrbBackground {
    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 560.dp)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Skip row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onSkipToHome,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_skip_button"),
          ) {
            Text("Skip", color = palette.textSecondary)
          }
        }

        // Pager with calm Glass Hero Illustrations
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { pageIndex ->
          val page = ONBOARDING_PAGES[pageIndex]
          Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            GlassSurface(
              level = GlassLevel.CARD,
              shape = RoundedCornerShape(32.dp),
              tintOverlay = palette.primaryAccent,
              accentBorder = palette.primaryAccent.copy(alpha = 0.40f),
              modifier = Modifier.size(164.dp),
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                  drawCircle(
                    color = palette.primaryAccent.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.38f,
                    style = stroke,
                  )
                  drawCircle(
                    color = palette.primaryAccent.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.26f,
                  )
                }
                Icon(
                  imageVector = page.icon,
                  contentDescription = null,
                  tint = palette.primaryAccent,
                  modifier = Modifier.size(52.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
              text = page.badge.uppercase(),
              style = MaterialTheme.typography.titleSmall,
              color = palette.primaryAccent,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = page.title,
              style = MaterialTheme.typography.headlineMedium,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = page.body,
              style = MaterialTheme.typography.bodyLarge,
              color = palette.textSecondary.copy(alpha = 0.82f),
              textAlign = TextAlign.Center,
            )
          }
        }

        // Dots + Action Button
        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            repeat(ONBOARDING_PAGES.size) { idx ->
              val active = pagerState.currentPage == idx
              Box(
                modifier =
                  Modifier.height(8.dp)
                    .width(if (active) 26.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                      if (active) palette.primaryAccent
                      else palette.textSecondary.copy(alpha = 0.28f)
                    ),
              )
            }
          }

          GlassPillButton(
            text = if (isLastPage) "Set Up Permissions" else "Next",
            isPrimary = true,
            onClick = {
              if (isLastPage) {
                onFinishToPermissions()
              } else {
                scope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
              }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_next_button"),
          )
        }
      }
    }
  }
}
