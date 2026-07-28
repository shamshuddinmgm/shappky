package com.yassernull.shappky.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.yassernull.shappky.providers.ShappkyListWidgetProvider
import com.yassernull.shappky.providers.ShappkyWidgetProvider

/** Guards exported widget-config activities against forged appWidgetIds. */
object WidgetOwnershipUtils {
  fun isOwnedTriggerWidget(context: Context, appWidgetId: Int): Boolean {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
    val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId) ?: return false
    val expected = ComponentName(context, ShappkyWidgetProvider::class.java)
    return info.provider == expected
  }

  fun isOwnedListWidget(context: Context, appWidgetId: Int): Boolean {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
    val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId) ?: return false
    val expected = ComponentName(context, ShappkyListWidgetProvider::class.java)
    return info.provider == expected
  }
}
