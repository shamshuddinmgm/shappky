package com.yassernull.shappky.services

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.preferences.AppsListPreferences
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShappkyService : Service() {
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var shellManager: ShellManager

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    // Must call startForeground promptly when launched via startForegroundService.
    val bootstrapNotification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.shappky_service))
      .setContentText(getString(R.string.shappky_service_notification_text))
      .setSmallIcon(R.drawable.ic_shappky)
      .setOngoing(true)
      .build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(1, bootstrapNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      startForeground(1, bootstrapNotification)
    }

    val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    // Hard gate: never run the killer loop unless the user explicitly enabled it.
    if (!prefs.getBoolean(KEY_SERVICE_ENABLED, false)) {
      Log.w(TAG, "ShappkyService started without service_enabled — stopping immediately")
      setRunningState(false)
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return
    }

    shellManager = ShellManager(this, handler, executor)
    setRunningState(true)
    requestTileUpdate()
    startKillerLoop()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val enabled = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_SERVICE_ENABLED, false)
    if (!enabled) {
      stopSelf()
      return START_NOT_STICKY
    }
    // Do not sticky-restart after crash/swipe — user must opt in again via menu/tile.
    return START_NOT_STICKY
  }

  private fun startKillerLoop() {
    if (!executor.isShutdown) {
      executor.execute {
        while (isRunning) {
          try {
            killBackgroundApps()
            val sharedpreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val intervalMs = sharedpreferences.getLong("service_duration", 18000L)
            Thread.sleep(intervalMs)
          } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            break
          }
        }
      }
    }
  }

  private fun getUsedRamMb(): Int = try {
    val process = Runtime.getRuntime().exec("cat /proc/meminfo")
    BufferedReader(java.io.InputStreamReader(process.inputStream)).use { reader ->
      var memTotal = 0
      var memFree = 0
      var line = reader.readLine()
      while (line != null) {
        if (line.startsWith("MemTotal")) {
          memTotal = line.split(Regex("\\s+"))[1].toInt()
        } else if (line.startsWith("MemAvailable")) {
          memFree = line.split(Regex("\\s+"))[1].toInt()
        }
        line = reader.readLine()
      }
      process.waitFor()
      if (memTotal > 0 && memFree >= 0) (memTotal - memFree) / 1024 else 0
    }
  } catch (_: Exception) {
    0
  }

  private fun killBackgroundApps() {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      return
    }

    val sharedpreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val killAllOnRamLimit = sharedpreferences.getBoolean("service_kill_all_on_ram_limit", false)
    val killAllRamThreshold = sharedpreferences.getInt("service_kill_all_ram_threshold", 0)

    if (killAllOnRamLimit && killAllRamThreshold > 0) {
      val usedRamMb = getUsedRamMb()
      if (usedRamMb < killAllRamThreshold) {
        return
      }
    }

    val selectUserApps = sharedpreferences.getBoolean("service_select_user_apps", true)
    val selectSystemApps = sharedpreferences.getBoolean("service_select_system_apps", false)
    val serviceExcludedApps = HashSet(sharedpreferences.getStringSet("service_excluded_apps", emptySet()) ?: emptySet())
    val serviceManuallySelectedApps = HashSet(sharedpreferences.getStringSet("service_manually_selected_apps", emptySet()) ?: emptySet())
    val killAppOnRamLimit = sharedpreferences.getBoolean("service_kill_app_on_ram_limit", false)
    val killAppRamThreshold = sharedpreferences.getInt("service_kill_app_ram_threshold", 0)

    val hiddenApps = HashSet(sharedpreferences.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet())

    val protectedApps = com.yassernull.shappky.core.managers.ProtectionManager.getProtectedApps(this)

    val dumpOutput = shellManager.runShellCommandAndGetFullOutput(
      "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity|mFocusedApp|topActivity'",
    ) ?: return
    val psOutput =
      shellManager.runShellCommandAndGetFullOutput("${com.yassernull.shappky.core.managers.ShellManager.toyboxPath()} ps -A -o rss,name | grep '\\.' | grep -v '[-@]'")
        ?: return

    val runningPackages = HashSet<String>()
    val packageRamUsage = HashMap<String, Int>()
    val pm = packageManager
    try {
      BufferedReader(StringReader(psOutput)).use { reader ->
        var line = reader.readLine()
        while (line != null) {
          val parts = line.trim().split(Regex("\\s+"))
          if (parts.size >= 2) {
            val rawPackageName = parts[1].trim()
            val packageName = if (rawPackageName.contains(":")) rawPackageName.substringBefore(":") else rawPackageName
            val rssKb = parts[0].trim().toLongOrNull() ?: 0L
            if (packageName.isNotEmpty() && packageName.contains(".")) {
              try {
                pm.getApplicationInfo(packageName, 0)
                runningPackages.add(packageName)
                packageRamUsage[packageName] = (packageRamUsage[packageName] ?: 0) + (rssKb / 1024L).toInt() // Convert to MB
              } catch (_: PackageManager.NameNotFoundException) {
              }
            }
          }
          line = reader.readLine()
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }

    val toKill = runningPackages.filter { pkg ->
      try {
        if (
          isProtected(pkg, protectedApps) ||
          com.yassernull.shappky.utils.PackageMatchUtils.dumpContainsPackage(dumpOutput, pkg)
        ) {
          return@filter false
        }

        if (hiddenApps.contains(pkg)) {
          return@filter false
        }

        if (serviceExcludedApps.contains(pkg)) {
          return@filter false
        }

        if (serviceManuallySelectedApps.isNotEmpty() && !serviceManuallySelectedApps.contains(pkg)) {
          return@filter false
        }

        val appInfo = pm.getApplicationInfo(pkg, 0)
        val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

        if (serviceManuallySelectedApps.isEmpty()) {
          val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
          if (isPersistent) {
            return@filter false
          }
          if (isSystem && !selectSystemApps) {
            return@filter false
          }
          if (!isSystem && !selectUserApps) {
            return@filter false
          }
        }

        if (serviceManuallySelectedApps.isNotEmpty()) {
          val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
          if (isPersistent) {
            return@filter false
          }
        }

        if (killAppOnRamLimit && killAppRamThreshold > 0) {
          val appRamMb = packageRamUsage[pkg] ?: 0
          if (appRamMb < killAppRamThreshold) {
            return@filter false
          }
        }

        true
      } catch (_: PackageManager.NameNotFoundException) {
        false
      }
    }

    if (toKill.isNotEmpty()) {
      val finalCommand = com.yassernull.shappky.core.managers.BackgroundAppManager.buildSmartKillCommand(toKill)
      shellManager.runShellCommandAndGetFullOutput(finalCommand) ?: return

      val lines = toKill.map { pkg ->
        val ramMb = packageRamUsage[pkg] ?: 0
        val ramStr = getString(R.string.mb_format, ramMb.toFloat())
        getString(R.string.ram_freed_from_pkg, ramStr, pkg)
      }
      val contentText = lines.first()
      val bigText = lines.joinToString("\n")
      updateNotification(contentText, bigText)
    }
  }

  private fun isProtected(
    packageName: String,
    @Suppress("UNUSED_PARAMETER") protectedApps: Set<String>,
  ): Boolean = ProtectionManager.isProtected(this, packageName)

  override fun onDestroy() {
    setRunningState(false)
    requestTileUpdate()
    if (::shellManager.isInitialized) {
      shellManager.removeShizukuPermissionListener()
    }
    super.onDestroy()
    executor.shutdownNow()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun requestTileUpdate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      TileService.requestListeningState(this, ComponentName(this, ShappkyQuickTile::class.java))
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.shappky_service_channel),
        NotificationManager.IMPORTANCE_LOW,
      )
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  private fun updateNotification(text: String, bigText: String? = null) {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.shappky_service))
      .setContentText(text)
      .setSmallIcon(R.drawable.ic_shappky)
      .setOngoing(true)

    if (bigText != null) {
      builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(1, builder.build())
  }

  companion object {
    private var isRunning = false
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()
    private const val CHANNEL_ID = "ShappkyChannel"
    private const val PREFERENCES_NAME = "AppPreferences"
    private const val KEY_HIDDEN_APPS = "hidden_apps"
    private const val KEY_SERVICE_ENABLED = AppsListPreferences.KEY_SERVICE_ENABLED
    private const val TAG = "ShappkyService"

    @JvmStatic
    fun isRunning(): Boolean = isRunning

    @JvmStatic
    fun isRunning(context: Context): Boolean {
      if (isRunning) return true
      val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      @Suppress("DEPRECATION")
      return am.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == ShappkyService::class.java.name }
    }

    @JvmStatic
    fun registerListener(listener: (Boolean) -> Unit) {
      listeners.add(listener)
      listener(isRunning)
    }

    @JvmStatic
    fun unregisterListener(listener: (Boolean) -> Unit) {
      listeners.remove(listener)
    }

    private fun setRunningState(running: Boolean) {
      isRunning = running
      listeners.forEach { it(running) }
    }
  }
}
