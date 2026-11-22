package com.example.ui.navigation

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.domain.model.UserPreferences
import com.example.ui.backup.BackupShareScreen
import com.example.ui.backup.BackupShareViewModel
import com.example.ui.calendar.CalendarScreen
import com.example.ui.editor.ReminderEditorScreen
import com.example.ui.editor.ReminderEditorSheetOverlay
import com.example.ui.editor.ReminderEditorViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeUiState
import com.example.ui.home.HomeViewModel
import com.example.ui.recyclebin.RecycleBinScreen
import com.example.ui.recyclebin.RecycleBinViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.setup.OnboardingScreen
import com.example.ui.setup.ReliabilityCheckScreen
import com.example.ui.theme.FloatingGlassBottomBar
import com.example.ui.theme.LocalVisualPerformance
import com.example.ui.theme.MainNavTab
import kotlinx.coroutines.launch

object RemindlyRoutes {
  const val ONBOARDING = "onboarding"
  const val RELIABILITY = "reliability?fromOnboarding={fromOnboarding}"
  const val HOME = "home"
  const val EDITOR = "editor?reminderId={reminderId}"
  const val SETTINGS = "settings"
  const val BACKUP = "backup"
  const val HISTORY = "history"
  const val RECYCLE_BIN = "recycle_bin"

  fun editor(reminderId: Long? = null): String =
    if (reminderId != null) "editor?reminderId=$reminderId" else "editor"

  fun reliability(fromOnboarding: Boolean = false): String =
    "reliability?fromOnboarding=$fromOnboarding"
}

private data class EditorSheetRequest(
  val reminderId: Long?,
  val nonce: Int,
)

@Composable
fun RemindlyNavHost(
  container: AppContainer,
  initialOnboardingCompleted: Boolean,
  incomingBackupUri: Uri? = null,
  onIncomingBackupUriConsumed: () -> Unit = {},
  openBackupRequested: Boolean = false,
  onOpenBackupRequestedConsumed: () -> Unit = {},
  openNewReminderRequested: Boolean = false,
  onOpenNewReminderConsumed: () -> Unit = {},
  editReminderIdRequested: Long? = null,
  onEditReminderIdConsumed: () -> Unit = {},
) {
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var pendingDeletedFromEditor by remember { mutableStateOf<Pair<Long, String>?>(null) }

  var selectedMainTab by remember { mutableStateOf(MainNavTab.HOME) }
  var editorSheetRequest by remember { mutableStateOf<EditorSheetRequest?>(null) }
  var editorNonce by remember { mutableIntStateOf(0) }

  val startDestination =
    remember(initialOnboardingCompleted) {
      if (initialOnboardingCompleted) RemindlyRoutes.HOME else RemindlyRoutes.ONBOARDING
    }

  fun openEditorSheet(reminderId: Long?) {
    editorNonce++
    editorSheetRequest = EditorSheetRequest(reminderId = reminderId, nonce = editorNonce)
  }

  LaunchedEffect(incomingBackupUri, openBackupRequested, openNewReminderRequested, editReminderIdRequested) {
    when {
      incomingBackupUri != null || openBackupRequested -> {
        if (openBackupRequested) onOpenBackupRequestedConsumed()
        navController.navigate(RemindlyRoutes.BACKUP) {
          launchSingleTop = true
        }
      }
      openNewReminderRequested -> {
        onOpenNewReminderConsumed()
        selectedMainTab = MainNavTab.HOME
        openEditorSheet(null)
      }
      editReminderIdRequested != null && editReminderIdRequested > 0L -> {
        val targetId = editReminderIdRequested
        onEditReminderIdConsumed()
        selectedMainTab = MainNavTab.HOME
        openEditorSheet(targetId)
      }
    }
  }

  NavHost(
    navController = navController,
    startDestination = startDestination,
    enterTransition = {
      fadeIn(animationSpec = tween(220)) +
        slideInHorizontally(animationSpec = tween(220)) { it / 12 }
    },
    exitTransition = { fadeOut(animationSpec = tween(180)) },
    popEnterTransition = { fadeIn(animationSpec = tween(200)) },
    popExitTransition = {
      fadeOut(animationSpec = tween(200)) +
        slideOutHorizontally(animationSpec = tween(200)) { it / 12 }
    },
  ) {
    composable(route = RemindlyRoutes.ONBOARDING) {
      OnboardingScreen(
        onFinishToPermissions = {
          navController.navigate(RemindlyRoutes.reliability(fromOnboarding = true))
        },
        onSkipToHome = {
          scope.launch { container.preferencesRepository.setOnboardingCompleted(true) }
          selectedMainTab = MainNavTab.HOME
          navController.navigate(RemindlyRoutes.HOME) {
            popUpTo(RemindlyRoutes.ONBOARDING) { inclusive = true }
          }
        },
      )
    }

    composable(
      route = RemindlyRoutes.RELIABILITY,
      arguments =
        listOf(
          navArgument("fromOnboarding") {
            type = NavType.BoolType
            defaultValue = false
          }
        ),
    ) { backStackEntry ->
      val fromOnboarding = backStackEntry.arguments?.getBoolean("fromOnboarding") ?: false
      val activeReminders by
        container.reminderRepository.activeReminders.collectAsStateWithLifecycle(
          initialValue = emptyList()
        )
      ReliabilityCheckScreen(
        activeRemindersCount = activeReminders.count { it.isActive },
        fromOnboarding = fromOnboarding,
        onNavigateBack = { navController.popBackStack() },
        onContinueToHome = {
          scope.launch { container.preferencesRepository.setOnboardingCompleted(true) }
          selectedMainTab = MainNavTab.HOME
          navController.navigate(RemindlyRoutes.HOME) {
            popUpTo(RemindlyRoutes.ONBOARDING) { inclusive = true }
          }
        },
      )
    }

    composable(route = RemindlyRoutes.HOME) {
      val homeViewModel: HomeViewModel =
        viewModel(
          factory =
            HomeViewModel.factory(
              reminderRepository = container.reminderRepository,
              preferencesRepository = container.preferencesRepository,
            )
        )
      val settingsViewModel: SettingsViewModel =
        viewModel(
          factory =
            SettingsViewModel.factory(
              context = context,
              preferencesRepository = container.preferencesRepository,
              reminderRepository = container.reminderRepository,
              scheduler = container.scheduler,
            )
        )
      val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
      val activeReminders by
        container.reminderRepository.activeReminders.collectAsStateWithLifecycle(
          initialValue = emptyList()
        )
      val prefs by
        container.preferencesRepository.userPreferences.collectAsStateWithLifecycle(
          initialValue = UserPreferences()
        )

      var bottomBarMinimized by remember { mutableStateOf(false) }
      val perf = LocalVisualPerformance.current
      val isSheetOpen = editorSheetRequest != null

      BackHandler(enabled = !isSheetOpen && selectedMainTab != MainNavTab.HOME) {
        selectedMainTab = MainNavTab.HOME
      }

      val backdropBlurModifier =
        if (isSheetOpen && !perf.lowPowerVisuals && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Modifier.fillMaxSize().blur(18.dp)
        } else {
          Modifier.fillMaxSize()
        }

      Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = backdropBlurModifier) {
          Crossfade(
            targetState = selectedMainTab,
            animationSpec = tween(if (perf.reducedMotion) 0 else 220),
            label = "main_tab_crossfade",
          ) { tab ->
            when (tab) {
              MainNavTab.HOME -> {
                HomeScreen(
                  viewModel = homeViewModel,
                  onAddReminder = { openEditorSheet(null) },
                  onEditReminder = { id -> openEditorSheet(id) },
                  onOpenSettings = { selectedMainTab = MainNavTab.SETTINGS },
                  onOpenReliabilityCheck = {
                    navController.navigate(RemindlyRoutes.reliability(fromOnboarding = false))
                  },
                  onScrollMinimizedChanged = { minimized -> bottomBarMinimized = minimized },
                  showTopSettingsButton = true,
                  pendingDeletedReminder = pendingDeletedFromEditor,
                  onPendingDeletedConsumed = { pendingDeletedFromEditor = null },
                )
              }

              MainNavTab.CALENDAR -> {
                val nowMillis = (homeUiState as? HomeUiState.Ready)?.nowMillis ?: System.currentTimeMillis()
                CalendarScreen(
                  reminders = activeReminders,
                  preferences = prefs,
                  nowMillis = nowMillis,
                  onEditReminder = { id -> openEditorSheet(id) },
                  onMarkDone = { rem -> homeViewModel.markDone(rem) },
                  onTogglePause = { rem -> homeViewModel.togglePause(rem) },
                  onDelete = { rem -> homeViewModel.deleteReminder(rem) },
                )
              }

              MainNavTab.SETTINGS -> {
                SettingsScreen(
                  viewModel = settingsViewModel,
                  onNavigateBack = { selectedMainTab = MainNavTab.HOME },
                  onOpenReliabilityCheck = {
                    navController.navigate(RemindlyRoutes.reliability(fromOnboarding = false))
                  },
                  onOpenBackupShare = {
                    navController.navigate(RemindlyRoutes.BACKUP)
                  },
                  onOpenHistory = {
                    navController.navigate(RemindlyRoutes.HISTORY)
                  },
                  onOpenRecycleBin = {
                    navController.navigate(RemindlyRoutes.RECYCLE_BIN)
                  },
                  onReplayOnboarding = {
                    navController.navigate(RemindlyRoutes.ONBOARDING)
                  },
                  showBackButton = false,
                  extraBottomPadding = 112.dp,
                )
              }
            }
          }

          FloatingGlassBottomBar(
            selectedTab = selectedMainTab,
            onSelectTab = { nextTab ->
              bottomBarMinimized = false
              selectedMainTab = nextTab
            },
            onAddClick = { openEditorSheet(null) },
            isMinimized = bottomBarMinimized && selectedMainTab == MainNavTab.HOME,
            modifier = Modifier.align(Alignment.BottomCenter),
          )
        }

        // Tall Frosted-Glass Bottom Sheet for Add / Edit Reminder
        val activeRequest = editorSheetRequest
        AnimatedVisibility(
          visible = activeRequest != null,
          enter =
            fadeIn(tween(220)) +
              slideInVertically(animationSpec = tween(260)) { fullHeight -> fullHeight / 3 },
          exit =
            fadeOut(tween(180)) +
              slideOutVertically(animationSpec = tween(200)) { fullHeight -> fullHeight / 3 },
        ) {
          if (activeRequest != null) {
            val editorViewModel: ReminderEditorViewModel =
              viewModel(
                key = "editor_sheet_${activeRequest.reminderId ?: "new"}_${activeRequest.nonce}",
                factory =
                  ReminderEditorViewModel.factory(
                    reminderId = activeRequest.reminderId,
                    reminderRepository = container.reminderRepository,
                    preferencesRepository = container.preferencesRepository,
                  ),
              )
            ReminderEditorSheetOverlay(
              viewModel = editorViewModel,
              onDismiss = { editorSheetRequest = null },
              onDeleted = { id, title ->
                pendingDeletedFromEditor = id to title
                editorSheetRequest = null
                selectedMainTab = MainNavTab.HOME
              },
            )
          }
        }
      }
    }

    composable(
      route = RemindlyRoutes.EDITOR,
      arguments =
        listOf(
          navArgument("reminderId") {
            type = NavType.LongType
            defaultValue = -1L
          }
        ),
    ) { backStackEntry ->
      val argId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
      val reminderId = if (argId > 0L) argId else null
      val editorViewModel: ReminderEditorViewModel =
        viewModel(
          factory =
            ReminderEditorViewModel.factory(
              reminderId = reminderId,
              reminderRepository = container.reminderRepository,
              preferencesRepository = container.preferencesRepository,
            )
        )
      ReminderEditorScreen(
        viewModel = editorViewModel,
        onNavigateBack = { navController.popBackStack() },
        onDeleted = { id, title ->
          pendingDeletedFromEditor = id to title
          navController.popBackStack()
        },
      )
    }

    composable(route = RemindlyRoutes.SETTINGS) {
      val settingsViewModel: SettingsViewModel =
        viewModel(
          factory =
            SettingsViewModel.factory(
              context = context,
              preferencesRepository = container.preferencesRepository,
              reminderRepository = container.reminderRepository,
              scheduler = container.scheduler,
            )
        )
      SettingsScreen(
        viewModel = settingsViewModel,
        onNavigateBack = { navController.popBackStack() },
        onOpenReliabilityCheck = {
          navController.navigate(RemindlyRoutes.reliability(fromOnboarding = false))
        },
        onOpenBackupShare = {
          navController.navigate(RemindlyRoutes.BACKUP)
        },
        onOpenHistory = {
          navController.navigate(RemindlyRoutes.HISTORY)
        },
        onOpenRecycleBin = {
          navController.navigate(RemindlyRoutes.RECYCLE_BIN)
        },
        onReplayOnboarding = {
          navController.navigate(RemindlyRoutes.ONBOARDING)
        },
      )
    }

    composable(route = RemindlyRoutes.HISTORY) {
      val historyViewModel: HistoryViewModel =
        viewModel(factory = HistoryViewModel.factory(container.reminderRepository))
      HistoryScreen(
        viewModel = historyViewModel,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    composable(route = RemindlyRoutes.RECYCLE_BIN) {
      val recycleBinViewModel: RecycleBinViewModel =
        viewModel(factory = RecycleBinViewModel.factory(container.reminderRepository))
      RecycleBinScreen(
        viewModel = recycleBinViewModel,
        onNavigateBack = { navController.popBackStack() },
      )
    }

    composable(route = RemindlyRoutes.BACKUP) {
      val backupViewModel: BackupShareViewModel =
        viewModel(
          factory =
            BackupShareViewModel.factory(
              context = context,
              reminderRepository = container.reminderRepository,
              preferencesRepository = container.preferencesRepository,
              scheduler = container.scheduler,
            )
        )
      BackupShareScreen(
        viewModel = backupViewModel,
        incomingFileUri = incomingBackupUri,
        onIncomingFileConsumed = onIncomingBackupUriConsumed,
        onNavigateBack = { navController.popBackStack() },
        onImportedToHome = {
          selectedMainTab = MainNavTab.HOME
          navController.navigate(RemindlyRoutes.HOME) {
            popUpTo(RemindlyRoutes.HOME) { inclusive = true }
          }
        },
      )
    }
  }
}
