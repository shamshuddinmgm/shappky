package com.yassernull.shappky.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerAlarmManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.data.models.TriggerModel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "AlarmReceiver"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val triggerId = intent.getStringExtra("trigger_id") ?: return
    Log.d(TAG, "Alarm fired for trigger ID: $triggerId")

    val triggers = TriggerManager.getTriggers(context)
    val trigger = triggers.firstOrNull { it.id == triggerId }

    if (trigger != null && trigger.isEnabled) {
      val pendingResult = goAsync()
      val executor = Executors.newSingleThreadExecutor()
      executor.execute {
        try {
          executeAlarmTrigger(context, trigger)
          TriggerAlarmManager.scheduleAlarmForTrigger(context, trigger)
        } finally {
          pendingResult.finish()
          executor.shutdown()
        }
      }
    }
  }

  private fun executeAlarmTrigger(context: Context, trigger: TriggerModel) {
    Log.d(TAG, "executeAlarmTrigger: Executing trigger '${trigger.name}'")

    val handler = Handler(Looper.getMainLooper())
    val executor = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, executor)
    try {
      shellManager.checkShellPermissions()

      if (!shellManager.hasAnyShellPermission()) {
        Log.w(TAG, "executeAlarmTrigger: Skipped due to lack of shell permissions")
        return
      }

      val appManager = BackgroundAppManager(context, handler, executor, shellManager)
      val loadLatch = CountDownLatch(1)
      var runningApps = emptyList<com.yassernull.shappky.data.models.AppModel>()
      appManager.loadBackgroundApps { apps ->
        runningApps = apps
        loadLatch.countDown()
      }
      if (!loadLatch.await(60, TimeUnit.SECONDS)) {
        Log.w(TAG, "executeAlarmTrigger: Timed out loading apps")
        return
      }

      val toKill = runningApps.filter { app ->
        val matchesUser = !app.isSystemApp && !app.isPersistentApp && trigger.selectUserApps
        val matchesSystem = app.isSystemApp && trigger.selectSystemApps
        val matchesPersistent = app.isPersistentApp && trigger.selectPersistentApps
        val matchesManual = trigger.manuallySelectedApps.contains(app.packageName)
        val isExcluded = trigger.excludedApps.contains(app.packageName)

        (matchesUser || matchesSystem || matchesPersistent || matchesManual) && !isExcluded && !app.isProtected
      }

      val packageNamesToKill = toKill.map { it.packageName }
      Log.d(TAG, "executeAlarmTrigger: Target packages to kill: $packageNamesToKill")
      if (packageNamesToKill.isNotEmpty()) {
        val killLatch = CountDownLatch(1)
        appManager.killPackages(packageNamesToKill, {
          val totalKb = toKill.sumOf { it.ramKb }
          val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          Log.d(TAG, "executeAlarmTrigger: Kill completed successfully. Freed memory: $freedText")
          com.yassernull.shappky.utils.NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
          killLatch.countDown()
        }, showToast = false)
        killLatch.await(120, TimeUnit.SECONDS)
      } else {
        Log.d(TAG, "executeAlarmTrigger: No packages matched search filters to kill")
      }
    } finally {
      shellManager.removeShizukuPermissionListener()
      executor.shutdown()
    }
  }
}
