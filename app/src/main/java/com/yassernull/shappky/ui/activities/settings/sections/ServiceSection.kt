package com.yassernull.shappky.ui.activities.settings

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.preferences.AppsListPreferences
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.services.ShappkyService
import com.yassernull.shappky.ui.activities.main.MainActions
import com.yassernull.shappky.ui.activities.main.PermissionHandler
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationActivity
import com.yassernull.shappky.ui.activities.triggers.TriggersActivity
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.SettingsHeader
import com.yassernull.shappky.ui.components.SwitchSettingRow
import java.util.concurrent.Executors

@Composable
fun ServiceSection() {
  val context = LocalContext.current
  val prefs = remember {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  }
  var serviceEnabled by remember {
    mutableStateOf(
      prefs.getBoolean(AppsListPreferences.KEY_SERVICE_ENABLED, false) &&
        ShappkyService.isRunning(context),
    )
  }

  SettingsHeader(text = stringResource(R.string.settings_service))
  SwitchSettingRow(
    icon = Icons.Filled.PowerSettingsNew,
    title = stringResource(R.string.shappky_service),
    summary = stringResource(R.string.shappky_service_settings_summary),
    checked = serviceEnabled,
    onCheckedChange = { want ->
      if (want) {
        if (!PermissionHandler.hasNotificationPermission(context)) {
          Toast.makeText(context, context.getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show()
          return@SwitchSettingRow
        }
        val handler = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()
        val shellManager = ShellManager(context, handler, executor)
        try {
          shellManager.checkShellPermissions()
          if (!shellManager.hasAnyShellPermission()) {
            Toast.makeText(context, context.getString(R.string.shell_permission_required), Toast.LENGTH_SHORT).show()
            return@SwitchSettingRow
          }
        } finally {
          shellManager.removeShizukuPermissionListener()
          executor.shutdown()
        }
        MainActions.setServiceEnabled(context, true)
        ContextCompat.startForegroundService(
          context,
          Intent(context, ShappkyService::class.java),
        )
        serviceEnabled = true
      } else {
        MainActions.setServiceEnabled(context, false)
        context.stopService(Intent(context, ShappkyService::class.java))
        serviceEnabled = false
      }
    },
  )
  ActionSettingRow(
    painter = painterResource(R.drawable.ic_shappky),
    title = stringResource(R.string.customize_service),
    summary = stringResource(R.string.customize_service_summary),
    onClick = {
      context.startActivity(Intent(context, ServiceCustomizationActivity::class.java))
    },
  )

  SettingsHeader(text = stringResource(R.string.triggers))
  ActionSettingRow(
    icon = Icons.Filled.Bolt,
    title = stringResource(R.string.triggers),
    summary = stringResource(R.string.triggers_summary),
    onClick = {
      context.startActivity(Intent(context, TriggersActivity::class.java))
    },
  )
}
