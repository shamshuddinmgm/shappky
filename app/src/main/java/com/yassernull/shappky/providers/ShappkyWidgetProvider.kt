package com.yassernull.shappky.providers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.core.preferences.WidgetPreferences
import com.yassernull.shappky.receivers.TriggerWidgetActionReceiver

class ShappkyWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    for (appWidgetId in appWidgetIds) {
      if (prefs.contains(WidgetPreferences.getTriggerIdKey(appWidgetId))) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
      } else {
        val views = RemoteViews(context.packageName, R.layout.shappky_widget)
        val defaultBgColor = context.getColor(R.color.control_blue)
        views.setInt(R.id.widget_background, "setColorFilter", defaultBgColor)
        views.setFloat(R.id.widget_background, "setAlpha", 1.0f)
        views.setInt(R.id.widget_icon, "setColorFilter", Color.WHITE)
        views.setViewVisibility(R.id.widget_edit_icon, android.view.View.GONE)

        val density = context.resources.displayMetrics.density
        val bgPaddingPx = ((60 - 48) / 2f * density).toInt()
        val iconPaddingPx = ((60 - 50) / 2f * density).toInt()
        views.setViewPadding(R.id.widget_background, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
        views.setViewPadding(R.id.widget_edit_icon, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
        views.setViewPadding(R.id.widget_icon, iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)

        val configIntent = Intent(context, com.yassernull.shappky.ui.activities.widgetConfig.WidgetConfigActivity::class.java).apply {
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
          context,
          appWidgetId,
          configIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_edit_icon, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
      val manager = AppWidgetManager.getInstance(context)
      val owned = manager.getAppWidgetIds(
        android.content.ComponentName(context, ShappkyWidgetProvider::class.java),
      ).toSet()
      val requested = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.toSet().orEmpty()
      if (requested.isNotEmpty() && requested.none { it in owned }) {
        return
      }
    }
    super.onReceive(context, intent)
    // Clicks handled by non-exported TriggerWidgetActionReceiver
  }

  companion object {
    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val iconColor = prefs.getInt(WidgetPreferences.getIconColorKey(appWidgetId), Color.WHITE)
      val defaultBgColor = context.getColor(R.color.control_blue)
      val bgColor = prefs.getInt(WidgetPreferences.getBgColorKey(appWidgetId), defaultBgColor)
      val bgSize = prefs.getInt(WidgetPreferences.getBgSizeKey(appWidgetId), 48)
      val iconSize = 50

      val views = RemoteViews(context.packageName, R.layout.shappky_widget)
      views.setInt(R.id.widget_background, "setColorFilter", bgColor)
      views.setFloat(R.id.widget_background, "setAlpha", 1.0f)
      views.setInt(R.id.widget_icon, "setColorFilter", iconColor)
      views.setViewVisibility(R.id.widget_edit_icon, android.view.View.GONE)

      val density = context.resources.displayMetrics.density
      val bgPaddingPx = ((60 - bgSize) / 2f * density).toInt()
      val iconPaddingPx = ((60 - iconSize) / 2f * density).toInt()
      views.setViewPadding(R.id.widget_background, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_edit_icon, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_icon, iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)

      val showLabel = prefs.getBoolean(WidgetPreferences.getShowLabelKey(appWidgetId), true)
      if (showLabel) {
        val triggerId = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""
        val triggerName = TriggerManager.getTriggers(context).firstOrNull { it.id == triggerId }?.name ?: ""
        views.setViewVisibility(R.id.widget_label, android.view.View.VISIBLE)
        views.setTextViewText(R.id.widget_label, triggerName)
      } else {
        views.setViewVisibility(R.id.widget_label, android.view.View.GONE)
      }

      views.setOnClickPendingIntent(
        R.id.widget_root,
        TriggerWidgetActionReceiver.clickPendingIntent(context, appWidgetId),
      )

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: android.os.Bundle,
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    if (prefs.contains(WidgetPreferences.getTriggerIdKey(appWidgetId))) {
      updateAppWidget(context, appWidgetManager, appWidgetId)

      val views = RemoteViews(context.packageName, R.layout.shappky_widget)
      // Re-apply edit icon after updateAppWidget hid it for normal use
      val iconColor = prefs.getInt(WidgetPreferences.getIconColorKey(appWidgetId), Color.WHITE)
      val defaultBgColor = context.getColor(R.color.control_blue)
      val bgColor = prefs.getInt(WidgetPreferences.getBgColorKey(appWidgetId), defaultBgColor)
      val bgSize = prefs.getInt(WidgetPreferences.getBgSizeKey(appWidgetId), 48)
      val iconSize = 50

      views.setInt(R.id.widget_background, "setColorFilter", bgColor)
      views.setFloat(R.id.widget_background, "setAlpha", 1.0f)
      views.setInt(R.id.widget_icon, "setColorFilter", iconColor)
      views.setViewVisibility(R.id.widget_edit_icon, android.view.View.VISIBLE)

      val density = context.resources.displayMetrics.density
      val bgPaddingPx = ((60 - bgSize) / 2f * density).toInt()
      val iconPaddingPx = ((60 - iconSize) / 2f * density).toInt()
      views.setViewPadding(R.id.widget_background, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_edit_icon, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_icon, iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)

      val showLabel = prefs.getBoolean(WidgetPreferences.getShowLabelKey(appWidgetId), true)
      if (showLabel) {
        val triggerId = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""
        val triggerName = TriggerManager.getTriggers(context).firstOrNull { it.id == triggerId }?.name ?: ""
        views.setViewVisibility(R.id.widget_label, android.view.View.VISIBLE)
        views.setTextViewText(R.id.widget_label, triggerName)
      } else {
        views.setViewVisibility(R.id.widget_label, android.view.View.GONE)
      }

      views.setOnClickPendingIntent(
        R.id.widget_root,
        TriggerWidgetActionReceiver.clickPendingIntent(context, appWidgetId),
      )

      val configIntent = Intent(context, com.yassernull.shappky.ui.activities.widgetConfig.WidgetConfigActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        configIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      views.setOnClickPendingIntent(R.id.widget_edit_icon, pendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}
