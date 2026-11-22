package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.ReminderTriggerReceiver
import com.example.domain.model.UserPreferences
import com.example.security.AppLockManager
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.localization.stringsForLanguage
import com.example.ui.navigation.RemindlyNavHost
import com.example.ui.security.AppLockScreen
import com.example.ui.theme.RemindlyTheme

class MainActivity : FragmentActivity() {

  private var incomingBackupUriState by mutableStateOf<Uri?>(null)
  private var openBackupRequestedState by mutableStateOf(false)
  private var openNewReminderRequestedState by mutableStateOf(false)
  private var editReminderIdRequestedState by mutableStateOf<Long?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    consumeIntent(intent)

    val appContainer = (application as RemindlyApplication).container

    setContent {
      val preferences by
        appContainer.preferencesRepository.userPreferences.collectAsStateWithLifecycle(
          initialValue = null as UserPreferences?
        )

      val loadedPrefs = preferences
      if (loadedPrefs != null) {
        val initialOnboardingDone = remember { loadedPrefs.hasCompletedOnboarding }
        var isAppUnlocked by remember { mutableStateOf(!loadedPrefs.appLockEnabled) }
        var lockInitialized by remember { mutableStateOf(false) }
        var backgroundedAtMillis by remember { mutableLongStateOf(0L) }

        LaunchedEffect(loadedPrefs.appLockEnabled) {
          if (!lockInitialized) {
            isAppUnlocked = !loadedPrefs.appLockEnabled
            lockInitialized = true
          } else if (!loadedPrefs.appLockEnabled) {
            isAppUnlocked = true
          }
        }

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
          if (loadedPrefs.appLockEnabled) {
            backgroundedAtMillis = System.currentTimeMillis()
          }
        }

        LifecycleEventEffect(Lifecycle.Event.ON_START) {
          if (loadedPrefs.appLockEnabled && backgroundedAtMillis > 0L) {
            val elapsedMillis = System.currentTimeMillis() - backgroundedAtMillis
            if (elapsedMillis >= loadedPrefs.appLockTimeoutSeconds * 1000L) {
              isAppUnlocked = false
            }
          }
        }

        ProvideAppLanguage(language = loadedPrefs.appLanguage) {
          RemindlyTheme(
            themeMode = loadedPrefs.themeMode,
            accentPalette = loadedPrefs.accentPalette,
            lowPowerVisuals = loadedPrefs.lowPowerVisuals,
          ) {
            if (loadedPrefs.appLockEnabled && loadedPrefs.appLockPinHash.isNotBlank() && !isAppUnlocked) {
              val strings = stringsForLanguage(loadedPrefs.appLanguage)
              val biometricAvailable =
                remember(loadedPrefs.appLockUseBiometric) {
                  loadedPrefs.appLockUseBiometric && AppLockManager.canUseBiometric(this@MainActivity)
                }
              AppLockScreen(
                pinHash = loadedPrefs.appLockPinHash,
                biometricAvailable = biometricAvailable,
                onVerifyPin = { pin, hash -> AppLockManager.verifyPin(pin, hash) },
                onRequestBiometric = {
                  AppLockManager.showBiometricPrompt(
                    activity = this@MainActivity,
                    title = strings.lockScreenTitle,
                    subtitle = strings.lockScreenSubtitle,
                    negativeText = strings.usePinButton,
                    onAuthenticated = { isAppUnlocked = true },
                  )
                },
                onUnlocked = { isAppUnlocked = true },
              )
            } else {
              RemindlyNavHost(
                container = appContainer,
                initialOnboardingCompleted = initialOnboardingDone,
                incomingBackupUri = incomingBackupUriState,
                onIncomingBackupUriConsumed = { incomingBackupUriState = null },
                openBackupRequested = openBackupRequestedState,
                onOpenBackupRequestedConsumed = { openBackupRequestedState = false },
                openNewReminderRequested = openNewReminderRequestedState,
                onOpenNewReminderConsumed = { openNewReminderRequestedState = false },
                editReminderIdRequested = editReminderIdRequestedState,
                onEditReminderIdConsumed = { editReminderIdRequestedState = null },
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    consumeIntent(intent)
  }

  private fun consumeIntent(intent: Intent?) {
    if (intent == null) return
    if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
      incomingBackupUriState = intent.data
    }
    if (intent.getBooleanExtra(ReminderTriggerReceiver.EXTRA_OPEN_BACKUP, false)) {
      openBackupRequestedState = true
    }
    if (intent.getBooleanExtra(EXTRA_OPEN_NEW_REMINDER, false)) {
      openNewReminderRequestedState = true
    }
    val editId = intent.getLongExtra(EXTRA_EDIT_REMINDER_ID, -1L)
    if (editId > 0L) {
      editReminderIdRequestedState = editId
    }
  }

  companion object {
    const val EXTRA_OPEN_NEW_REMINDER = "extra_open_new_reminder"
    const val EXTRA_EDIT_REMINDER_ID = "extra_edit_reminder_id"
  }
}
