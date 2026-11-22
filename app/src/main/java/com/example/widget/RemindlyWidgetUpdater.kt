package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object RemindlyWidgetUpdater {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun requestUpdate(context: Context) {
    val appContext = context.applicationContext
    scope.launch {
      runCatching { RemindlyGlanceWidget().updateAll(appContext) }
    }
  }

  fun canRequestPin(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
    return runCatching { manager.isRequestPinAppWidgetSupported }.getOrDefault(false)
  }

  fun requestPin(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
    if (!runCatching { manager.isRequestPinAppWidgetSupported }.getOrDefault(false)) return false
    val provider = ComponentName(context, RemindlyWidgetReceiver::class.java)
    return runCatching { manager.requestPinAppWidget(provider, null, null) }.getOrDefault(false)
  }
}
