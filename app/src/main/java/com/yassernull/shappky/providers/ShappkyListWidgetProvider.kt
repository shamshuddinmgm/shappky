package com.yassernull.shappky.providers

import android.app.ActivityManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.yassernull.shappky.App
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.receivers.ListWidgetActionReceiver
import com.yassernull.shappky.services.ShappkyWidgetService
import java.util.concurrent.Executors

class ShappkyListWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    for (appWidgetId in appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId)
    }
    startAutoRefresh(context)
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    startAutoRefresh(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    stopAutoRefresh()
  }

  override fun onReceive(context: Context, intent: Intent) {
    // Only honor widget updates for IDs that belong to this provider (mitigate cross-app UPDATE spam).
    if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
      val manager = AppWidgetManager.getInstance(context)
      val owned = manager.getAppWidgetIds(ComponentName(context, ShappkyListWidgetProvider::class.java)).toSet()
      val requested = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.toSet().orEmpty()
      if (requested.isNotEmpty() && requested.none { it in owned }) {
        return
      }
    }
    super.onReceive(context, intent)
    // Click/refresh handled by non-exported ListWidgetActionReceiver
  }

  companion object {
    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    fun startAutoRefresh(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val componentName = ComponentName(context, ShappkyListWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
      if (appWidgetIds.isEmpty()) {
        stopAutoRefresh()
        return
      }

      val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
      var isRefreshEnabled = false
      var minIntervalMs = Long.MAX_VALUE

      for (id in appWidgetIds) {
        val autoRefresh = prefs.getBoolean("widget_list_auto_refresh_apps_$id", prefs.getBoolean("appsAutoRefresh", true))
        val ramUsageRefresh = prefs.getBoolean("widget_list_auto_refresh_ram_$id", prefs.getBoolean("appsRamUsageAutoRefresh", true))

        if (autoRefresh || ramUsageRefresh) {
          isRefreshEnabled = true
          val interval = when {
            autoRefresh && ramUsageRefresh -> {
              val autoInt = prefs.getLong("appsAutoRefreshIntervalMs", 1000L)
              val ramInt = prefs.getLong("appsRamUsageRefreshIntervalMs", 1000L)
              kotlin.math.min(autoInt, ramInt)
            }
            autoRefresh -> prefs.getLong("appsAutoRefreshIntervalMs", 1000L)
            else -> prefs.getLong("appsRamUsageRefreshIntervalMs", 1000L)
          }
          if (interval < minIntervalMs) {
            minIntervalMs = interval
          }
        }
      }

      if (!isRefreshEnabled) {
        stopAutoRefresh()
        return
      }

      val resolvedIntervalMs = minIntervalMs.coerceAtLeast(1000L)

      refreshRunnable?.let { handler.removeCallbacks(it) }

      refreshRunnable = object : Runnable {
        override fun run() {
          val innerComponentName = ComponentName(context, ShappkyListWidgetProvider::class.java)
          val innerWidgetIds = appWidgetManager.getAppWidgetIds(innerComponentName)
          if (innerWidgetIds.isEmpty()) {
            stopAutoRefresh()
            return
          }

          val innerPrefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
          var innerRefreshEnabled = false
          var innerMinInterval = Long.MAX_VALUE

          for (id in innerWidgetIds) {
            val autoRefresh = innerPrefs.getBoolean("widget_list_auto_refresh_apps_$id", innerPrefs.getBoolean("appsAutoRefresh", true))
            val ramUsageRefresh = innerPrefs.getBoolean("widget_list_auto_refresh_ram_$id", innerPrefs.getBoolean("appsRamUsageAutoRefresh", true))

            if (autoRefresh || ramUsageRefresh) {
              innerRefreshEnabled = true
              val interval = when {
                autoRefresh && ramUsageRefresh -> {
                  val autoInt = innerPrefs.getLong("appsAutoRefreshIntervalMs", 1000L)
                  val ramInt = innerPrefs.getLong("appsRamUsageRefreshIntervalMs", 1000L)
                  kotlin.math.min(autoInt, ramInt)
                }
                autoRefresh -> innerPrefs.getLong("appsAutoRefreshIntervalMs", 1000L)
                else -> innerPrefs.getLong("appsRamUsageRefreshIntervalMs", 1000L)
              }
              if (interval < innerMinInterval) {
                innerMinInterval = interval
              }
            }
          }

          if (innerRefreshEnabled) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isInteractive = pm.isInteractive
            val isAppInForeground = App.isAppInForeground

            if (isInteractive && !isAppInForeground) {
              @Suppress("DEPRECATION")
              appWidgetManager.notifyAppWidgetViewDataChanged(innerWidgetIds, R.id.widget_list_view)
              for (id in innerWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
              }
            }
            handler.postDelayed(this, innerMinInterval.coerceAtLeast(1000L))
          } else {
            stopAutoRefresh()
          }
        }
      }
      handler.postDelayed(refreshRunnable!!, resolvedIntervalMs)
    }

    fun stopAutoRefresh() {
      refreshRunnable?.let { handler.removeCallbacks(it) }
      refreshRunnable = null
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      val localCtx = getLocalizedContext(context)
      val views = RemoteViews(context.packageName, R.layout.shappky_list_widget)

      val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

      val appTheme = prefs.getString("appTheme", "dark") ?: "dark"
      val dynamic = prefs.getBoolean("dynamicColors", false)

      val autoBg = prefs.getBoolean("widget_list_auto_bg_$appWidgetId", true)
      val resolvedBgColor = if (autoBg) {
        when (appTheme) {
          "white" -> {
            if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              context.getColor(android.R.color.system_neutral1_10)
            } else {
              0xFFFFFFFF.toInt()
            }
          }
          "black" -> 0xFF000000.toInt()
          else -> {
            if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              context.getColor(android.R.color.system_neutral1_900)
            } else {
              0xFF17181C.toInt()
            }
          }
        }
      } else {
        prefs.getInt("widget_list_bg_color_$appWidgetId", 0xFF0088FF.toInt())
      }
      views.setInt(R.id.widget_background_image, "setColorFilter", resolvedBgColor)

      val isWhiteTheme = appTheme == "white"
      val elementColor = if (isWhiteTheme) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
      val secondaryElementColor = if (isWhiteTheme) 0x90111111.toInt() else 0xB0FFFFFF.toInt()
      val progressBgColor = if (isWhiteTheme) 0x15000000.toInt() else 0x30FFFFFF.toInt()

      views.setTextColor(R.id.widget_title, elementColor)
      views.setTextColor(R.id.widget_ram_text, secondaryElementColor)
      views.setTextColor(R.id.widget_empty_view, secondaryElementColor)
      views.setInt(R.id.widget_refresh_button, "setColorFilter", elementColor)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        views.setColorStateList(R.id.widget_ram_bar, "setProgressBackgroundTintList", android.content.res.ColorStateList.valueOf(progressBgColor))
      }

      views.setTextViewText(R.id.widget_title, localCtx.getString(R.string.app_name))

      val serviceIntent = Intent(context, ShappkyWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
      }
      @Suppress("DEPRECATION")
      views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
      views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

      val shellHandler = Handler(Looper.getMainLooper())
      val shellExecutor = Executors.newSingleThreadExecutor()
      val shellManager = ShellManager(context, shellHandler, shellExecutor)
      try {
        if (shellManager.hasAnyShellPermission()) {
          views.setTextViewText(R.id.widget_empty_view, localCtx.getString(R.string.no_apps_to_kill))
        } else {
          views.setTextViewText(R.id.widget_empty_view, localCtx.getString(R.string.permission_denied))
        }
      } finally {
        shellManager.removeShizukuPermissionListener()
        shellExecutor.shutdown()
      }

      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val memoryInfo = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(memoryInfo)
      val totalMb = memoryInfo.totalMem / (1024 * 1024)
      val availMb = memoryInfo.availMem / (1024 * 1024)
      val usedMb = totalMb - availMb
      val percentage = if (totalMb > 0) (usedMb * 100 / totalMb).toInt() else 0

      val ramBarRefresh = prefs.getBoolean("widget_list_ram_bar_refresh_$appWidgetId", true)
      if (ramBarRefresh) {
        views.setViewVisibility(R.id.widget_ram_bar, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_ram_text, android.view.View.VISIBLE)
        views.setProgressBar(R.id.widget_ram_bar, 100, percentage, false)
        views.setTextViewText(R.id.widget_ram_text, "$percentage% (${usedMb}MB / ${totalMb}MB)")
      } else {
        views.setViewVisibility(R.id.widget_ram_bar, android.view.View.GONE)
        views.setViewVisibility(R.id.widget_ram_text, android.view.View.GONE)
      }

      views.setPendingIntentTemplate(R.id.widget_list_view, ListWidgetActionReceiver.clickPendingIntent(context, appWidgetId))
      views.setOnClickPendingIntent(R.id.widget_refresh_button, ListWidgetActionReceiver.refreshPendingIntent(context, appWidgetId))

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}

private fun getLocalizedContext(context: Context): Context {
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
