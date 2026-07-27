package com.yassernull.shappky.receivers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.AppKillHandler
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.providers.ShappkyListWidgetProvider
import java.util.concurrent.Executors

/**
 * Non-exported receiver for list-widget click/refresh.
 * PendingIntents from our app can reach it; other apps cannot.
 */
class ListWidgetActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, ShappkyListWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    when (action) {
      ACTION_APP_CLICK -> {
        val packageName = intent.getStringExtra("package_name") ?: return
        val appName = intent.getStringExtra("app_name") ?: ""
        val appRam = intent.getStringExtra("app_ram") ?: ""
        if (ProtectionManager.isProtected(context, packageName)) {
          Toast.makeText(context, context.getString(R.string.force_kill_protected_message), Toast.LENGTH_SHORT).show()
          return
        }
        val pendingResult = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val shellExecutor = Executors.newSingleThreadExecutor()
        val shellManager = ShellManager(context, handler, shellExecutor)
        fun cleanup() {
          shellManager.removeShizukuPermissionListener()
          shellExecutor.shutdown()
          pendingResult.finish()
        }
        if (!shellManager.hasAnyShellPermission()) {
          Toast.makeText(context, context.getString(R.string.shell_permission_required), Toast.LENGTH_SHORT).show()
          cleanup()
          return
        }
        AppKillHandler(context, handler, shellManager).killApp(
          packageName = packageName,
          onComplete = {
            val localCtx = localizedContext(context)
            Toast.makeText(context, localCtx.getString(R.string.free_up_memory, appRam), Toast.LENGTH_SHORT).show()
            @Suppress("DEPRECATION")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            for (id in appWidgetIds) {
              ShappkyListWidgetProvider.updateAppWidget(context, appWidgetManager, id)
            }
            cleanup()
          },
          getAppRamKb = { 0L },
          formatMemorySize = { appRam },
        )
      }
      ACTION_REFRESH -> {
        @Suppress("DEPRECATION")
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
        for (id in appWidgetIds) {
          ShappkyListWidgetProvider.updateAppWidget(context, appWidgetManager, id)
        }
        ShappkyListWidgetProvider.startAutoRefresh(context)
      }
    }
  }

  companion object {
    const val ACTION_APP_CLICK = "com.yassernull.shappky.ACTION_APP_CLICK"
    const val ACTION_REFRESH = "com.yassernull.shappky.ACTION_REFRESH"

    fun clickPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
      val intent = Intent(context, ListWidgetActionReceiver::class.java).apply {
        action = ACTION_APP_CLICK
      }
      return PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    }

    fun refreshPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
      val intent = Intent(context, ListWidgetActionReceiver::class.java).apply {
        action = ACTION_REFRESH
      }
      return PendingIntent.getBroadcast(
        context,
        appWidgetId + 100000,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    }

    private fun localizedContext(context: Context): Context {
      val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
      val language = prefs.getString("appLanguage", "system") ?: "system"
      if (language != "system") {
        val locale = java.util.Locale.forLanguageTag(language)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
      }
      return context
    }
  }
}
