package com.yassernull.shappky.ui.activities.main

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.preferences.AppsListPreferences
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.services.ShappkyService

object MainActions {
  fun onOpenSettings(context: android.content.Context) {
    context.startActivity(android.content.Intent(context, com.yassernull.shappky.ui.activities.settings.SettingsActivity::class.java))
  }

  fun onOpenDonate(context: android.content.Context) {
    try {
      context.startActivity(
        android.content.Intent(
          android.content.Intent.ACTION_VIEW,
          android.net.Uri.parse(context.getString(R.string.donate_url)),
        ),
      )
    } catch (_: Exception) {}
  }

  fun onOpenTriggers(context: android.content.Context) {
    context.startActivity(android.content.Intent(context, com.yassernull.shappky.ui.activities.triggers.TriggersActivity::class.java))
  }

  fun setServiceEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(AppsListPreferences.KEY_SERVICE_ENABLED, enabled)
      .commit()
  }

  fun onToggleService(
    activity: ComponentActivity,
    start: Boolean,
    shellManager: ShellManager,
  ) {
    if (!PermissionHandler.hasNotificationPermission(activity)) {
      PermissionHandler.checkAndRequestNotificationPermission(activity)
    } else {
      if (start) {
        if (!shellManager.hasAnyShellPermission()) {
          shellManager.checkShellPermissions()
          android.widget.Toast.makeText(activity, activity.getString(R.string.shell_permission_required), android.widget.Toast.LENGTH_SHORT).show()
        } else {
          setServiceEnabled(activity, true)
          ContextCompat.startForegroundService(
            activity,
            android.content.Intent(activity, ShappkyService::class.java),
          )
        }
      } else {
        setServiceEnabled(activity, false)
        activity.stopService(android.content.Intent(activity, ShappkyService::class.java))
      }
    }
  }

  /** Stop killer service if it was left running without an explicit opt-in. */
  fun ensureKillerServiceMatchesPreference(context: Context) {
    val enabled = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .getBoolean(AppsListPreferences.KEY_SERVICE_ENABLED, false)
    if (!enabled && ShappkyService.isRunning(context)) {
      setServiceEnabled(context, false)
      context.stopService(android.content.Intent(context, ShappkyService::class.java))
    }
  }
}
