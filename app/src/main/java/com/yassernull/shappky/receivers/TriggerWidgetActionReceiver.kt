package com.yassernull.shappky.receivers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.core.preferences.WidgetPreferences
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.providers.ShappkyWidgetProvider
import com.yassernull.shappky.utils.NotificationUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Non-exported receiver for trigger-widget clicks.
 * PendingIntents from our app can reach it; other apps cannot.
 */
class TriggerWidgetActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != ACTION_WIDGET_CLICK) return
    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

    val pendingResult = goAsync()
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
      try {
        executeWidgetTrigger(context, appWidgetId)
      } finally {
        pendingResult.finish()
        executor.shutdown()
      }
    }
  }

  private fun executeWidgetTrigger(context: Context, appWidgetId: Int) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val triggerId = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""
    val triggers = TriggerManager.getTriggers(context)
    val trigger = triggers.firstOrNull { it.id == triggerId }
    val handler = Handler(Looper.getMainLooper())

    if (trigger == null) {
      handler.post {
        Toast.makeText(context, context.getString(R.string.please_select_trigger), Toast.LENGTH_SHORT).show()
      }
      return
    }

    val shellExecutor = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, shellExecutor)
    try {
      shellManager.checkShellPermissions()
      if (!shellManager.hasAnyShellPermission()) {
        handler.post {
          Toast.makeText(context, context.getString(R.string.shell_permission_required), Toast.LENGTH_LONG).show()
        }
        return
      }

      val appManager = BackgroundAppManager(context, handler, shellExecutor, shellManager)
      val latch = CountDownLatch(1)
      var loadedApps = emptyList<AppModel>()
      appManager.loadBackgroundApps { result ->
        loadedApps = result
        latch.countDown()
      }
      latch.await()

      val toKill = loadedApps.filter { app ->
        val matchesUser = !app.isSystemApp && !app.isPersistentApp && trigger.selectUserApps
        val matchesSystem = app.isSystemApp && trigger.selectSystemApps
        val matchesPersistent = app.isPersistentApp && trigger.selectPersistentApps
        val matchesManual = trigger.manuallySelectedApps.contains(app.packageName)
        val isExcluded = trigger.excludedApps.contains(app.packageName)
        (matchesUser || matchesSystem || matchesPersistent || matchesManual) &&
          !isExcluded &&
          !ProtectionManager.isProtected(context, app.packageName)
      }

      if (toKill.isNotEmpty()) {
        val totalKb = toKill.sumOf { it.ramKb }
        val killLatch = CountDownLatch(1)
        appManager.killPackages(
          toKill.map { it.packageName },
          { killLatch.countDown() },
          showToast = false,
        )
        killLatch.await()
        val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
        NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
      } else {
        handler.post {
          Toast.makeText(context, context.getString(R.string.no_apps_to_kill), Toast.LENGTH_SHORT).show()
        }
      }
    } finally {
      shellManager.removeShizukuPermissionListener()
      shellExecutor.shutdown()
    }

    val appWidgetManager = AppWidgetManager.getInstance(context)
    ShappkyWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
  }

  companion object {
    const val ACTION_WIDGET_CLICK = "com.yassernull.shappky.ACTION_WIDGET_CLICK"

    fun clickPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
      val intent = Intent(context, TriggerWidgetActionReceiver::class.java).apply {
        action = ACTION_WIDGET_CLICK
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      return PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}
