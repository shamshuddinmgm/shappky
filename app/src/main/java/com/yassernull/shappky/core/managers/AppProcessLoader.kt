package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppModel
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

class AppProcessLoader(
  private val context: Context,
  private val handler: Handler,
  private val executor: ExecutorService,
  private val shellManager: ShellManager,
) {
  val currentAppsList = mutableListOf<AppModel>()
  var showUserApps = true
  var showSystemApps = true
  var showPersistentApps = false
  var showProtectedApps = false
  var showServiceProcesses = false
  private val sharedPreferences =
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  @Volatile
  private var isCurrentlyLoadingApps = false

  private val pendingCallbacks = java.util.Collections.synchronizedList(ArrayList<Consumer<List<AppModel>>>())

  @Volatile
  private var isCurrentlyLoadingRam = false

  @Volatile
  private var cachedPssMap: Map<String, Long> = emptyMap()

  @Volatile
  private var cachedPssAtMs: Long = 0L

  fun formatMemorySize(kb: Long): String = when {
    kb < 1024 -> context.getString(R.string.kb_format, kb)
    kb < 1024 * 1024 -> context.getString(R.string.mb_format, kb / 1024f)
    else -> context.getString(R.string.gb_format, kb / (1024f * 1024f))
  }

  fun getActiveWidgetPackages(): Set<String> {
    if (!shellManager.isShellCommandReady()) return emptySet()
    val activePackages = mutableSetOf<String>()
    try {
      val output = shellManager.runShellCommandAndGetFullOutput("dumpsys appwidget") ?: ""
      val regex = Regex("provider=ComponentInfo\\{([^/]+)/")
      var inAppWidgetIds = false
      for (line in output.split('\n')) {
        val trimmed = line.trim()
        if (trimmed == "AppWidgetIds:") {
          inAppWidgetIds = true
          continue
        } else if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
          if (inAppWidgetIds && !line.contains("AppWidgetIds")) {
            inAppWidgetIds = false
          }
        }
        if (inAppWidgetIds) {
          val match = regex.find(line)
          if (match != null) {
            activePackages.add(match.groupValues[1])
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting active widget packages", e)
    }
    return activePackages
  }

  fun loadBackgroundApps(callback: Consumer<List<AppModel>>?) {
    if (callback != null) {
      synchronized(pendingCallbacks) {
        pendingCallbacks.add(callback)
      }
    }
    if (isCurrentlyLoadingApps) {
      Log.d(TAG, "loadBackgroundApps skipped because another load is already in progress")
      return
    }
    isCurrentlyLoadingApps = true
    if (!executor.isShutdown) {
      executor.execute {
        val startTime = System.currentTimeMillis()
        Log.d(
          TAG,
          "loadBackgroundApps started showSystemApps=$showSystemApps, showPersistentApps=$showPersistentApps",
        )
        var result = mutableListOf<AppModel>()
        try {
          val hiddenApps = getHiddenApps()
          val protectedApps = ProtectionManager.getProtectedApps(context)

          if (shellManager.isShellCommandReady()) {
            // Discover running dotted process names via ps, then measure PSS (not RSS).
            // Summing RSS double-counts shared framework pages; PSS matches what kill frees.
            try {
              val pssMap = loadPackagePssKbMap()
              val discovery = shellManager.runShellCommandAndGetFullOutput(
                "${ShellManager.toyboxPath()} ps -A -o rss,name | grep '\\.'",
              )
              val entries = if (discovery != null) parsePsOutputToEntries(discovery) else emptyList()
              val pm = context.packageManager
              val appNames = linkedSetOf<String>()
              val serviceNames = linkedSetOf<String>()
              for (entry in entries) {
                try {
                  pm.getApplicationInfo(entry.packageName, 0)
                  appNames.add(entry.packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                  serviceNames.add(entry.packageName)
                }
              }
              // Fallback RSS (max per package) only when PSS probe failed for that name.
              val rssFallback = aggregateByPackageMaxRss(entries)
              fun ramFor(name: String): Long = pssMap[name] ?: rssFallback[name] ?: 0L

              val runningEntries = appNames.map { "$it:${ramFor(it)}" }.toSet()
              val serviceProcessEntries = serviceNames.map { "$it:${ramFor(it)}" }.toSet()
              val defaultIcon = try {
                pm.getDefaultActivityIcon()
              } catch (_: Exception) {
                context.packageManager.getApplicationIcon(context.packageName)
              }

              result = AppModelFilter.buildRunningAppModels(
                runningEntries = runningEntries,
                hiddenApps = hiddenApps,
                protectedApps = protectedApps,
                serviceProcessEntries = serviceProcessEntries,
                context = context,
                formatMemorySize = ::formatMemorySize,
                defaultIcon = defaultIcon,
              ).toMutableList()
            } catch (e: Exception) {
              Log.e(TAG, "Error getting running apps", e)
              handler.post {
                Toast.makeText(
                  context,
                  context.getString(R.string.error_getting_running_apps, e.message.orEmpty()),
                  Toast.LENGTH_SHORT,
                ).show()
              }
            }
          } else {
            Log.w(TAG, "Shell command backend is not ready while loading background apps")
          }

          result.sortWith(
            compareBy<AppModel> { it.isServiceProcess }
              .thenBy { it.isSystemApp }
              .thenBy { it.isPersistentApp }
              .thenBy { it.appName.lowercase(Locale.getDefault()) },
          )
        } catch (t: Throwable) {
          Log.e(TAG, "Fatal error in loadBackgroundApps background thread", t)
        } finally {
          isCurrentlyLoadingApps = false
          handler.post {
            Log.d(
              TAG,
              "loadBackgroundApps finished resultSize=${result.size}, durationMs=${System.currentTimeMillis() - startTime}",
            )
            currentAppsList.clear()
            currentAppsList.addAll(result)

            val callbacksToTrigger = synchronized(pendingCallbacks) {
              val list = ArrayList(pendingCallbacks)
              pendingCallbacks.clear()
              list
            }
            val immutableResult = ArrayList(result)
            for (cb in callbacksToTrigger) {
              cb.accept(immutableResult)
            }
          }
        }
      }
    }
  }

  fun loadAllApps(callback: Consumer<List<AppModel>>) {
    if (!executor.isShutdown) {
      executor.execute {
        val startTime = System.currentTimeMillis()
        val protectedApps = ProtectionManager.getProtectedApps(context)
        val allApps = AppModelFilter.buildAllAppsList(context, protectedApps)
        Log.d(TAG, "loadAllApps finished count=${allApps.size}, durationMs=${System.currentTimeMillis() - startTime}")
        handler.post { callback.accept(allApps) }
      }
    }
  }

  fun loadAppsRamUsage(packageNames: List<String>, callback: Consumer<Map<String, Long>>) {
    if (isCurrentlyLoadingRam) {
      Log.d(TAG, "loadAppsRamUsage skipped because another RAM load is in progress")
      return
    }
    isCurrentlyLoadingRam = true
    if (!executor.isShutdown) {
      executor.execute {
        val ramUsageByPackage = mutableMapOf<String, Long>()
        try {
          val requestedPackages = packageNames.toSet()
          if (requestedPackages.isNotEmpty() && shellManager.isShellCommandReady()) {
            try {
              val pssMap = loadPackagePssKbMap()
              for (pkg in requestedPackages) {
                val pss = pssMap[pkg]
                if (pss != null && pss > 0L) ramUsageByPackage[pkg] = pss
              }
              // Fill gaps with max-RSS so rows don't go blank if smaps is blocked for a pid.
              if (ramUsageByPackage.size < requestedPackages.size) {
                val discovery = shellManager.runShellCommandAndGetFullOutput(
                  "${ShellManager.toyboxPath()} ps -A -o rss,name | grep '\\.' | grep -v '[-@]'",
                )
                if (discovery != null) {
                  val rssMax = aggregateByPackageMaxRss(
                    parsePsOutputToEntries(discovery).filter { it.packageName in requestedPackages },
                  )
                  for ((pkg, rss) in rssMax) {
                    if (pkg !in ramUsageByPackage) ramUsageByPackage[pkg] = rss
                  }
                }
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error updating app RAM usage", e)
            }
          }
          handler.post { callback.accept(ramUsageByPackage) }
        } catch (t: Throwable) {
          Log.e(TAG, "Fatal error in loadAppsRamUsage background thread", t)
        } finally {
          isCurrentlyLoadingRam = false
        }
      }
    }
  }

  /**
   * Package TOTAL PSS via `dumpsys meminfo -s`.
   *
   * `/proc/<pid>/smaps_rollup` is almost always unreadable for other apps even under
   * Shizuku (Permission denied) — that path fell back to inflated RSS. ActivityManager's
   * dumpsys reports real TOTAL PSS (what Settings-style memory uses).
   */
  private fun loadPackagePssKbMap(): Map<String, Long> {
    if (!shellManager.isShellCommandReady()) return emptyMap()
    val now = System.currentTimeMillis()
    val cached = cachedPssMap
    if (cached.isNotEmpty() && now - cachedPssAtMs < PSS_CACHE_TTL_MS) {
      return cached
    }
    val output = shellManager.runShellCommandAndGetFullOutput("dumpsys meminfo -s")
      ?: return emptyMap()
    val map = parseDumpsysMeminfoPssByPackage(output)
    if (map.isEmpty()) {
      // Legacy one-line probe fallback (rarely works without root).
      val tb = ShellManager.toyboxPath()
      val probe =
        "$tb ps -A -o pid,name 2>/dev/null | while read -r pid name; do " +
          "case \"\$pid\" in ''|PID|pid) continue ;; esac; " +
          "case \"\$name\" in *.*) ;; *) continue ;; esac; " +
          "f=\"/proc/\$pid/smaps_rollup\"; " +
          "[ -r \"\$f\" ] || continue; " +
          "pss=\$(awk '/^Pss:/{print \$2; exit}' \"\$f\" 2>/dev/null); " +
          "if [ -n \"\$pss\" ]; then echo \"\$pss \$name\"; fi; " +
          "done"
      val probeOut = shellManager.runShellCommandAndGetFullOutput(probe)
      val fromProbe = if (probeOut != null) {
        aggregateByPackage(parsePssProbeOutput(probeOut))
      } else {
        emptyMap()
      }
      if (fromProbe.isEmpty()) {
        Log.w(TAG, "PSS map empty (dumpsys + smaps); RAM rows will use max-RSS fallback")
        return emptyMap()
      }
      cachedPssMap = fromProbe
      cachedPssAtMs = now
      Log.d(TAG, "PSS map from smaps size=${fromProbe.size}")
      return fromProbe
    }
    cachedPssMap = map
    cachedPssAtMs = now
    Log.d(TAG, "PSS map from dumpsys meminfo size=${map.size}")
    return map
  }

  fun loadAppDetailedInfo(app: AppModel, callback: Consumer<com.yassernull.shappky.data.models.AppDetailedInfo>) {
    if (!executor.isShutdown) {
      executor.execute {
        try {
          if (shellManager.isShellCommandReady()) {
            if (!com.yassernull.shappky.utils.PackageMatchUtils.isValidAndroidPackageName(app.packageName)) {
              handler.post {
                callback.accept(
                  com.yassernull.shappky.data.models.AppDetailedInfo(
                    app = app,
                    pid = "-",
                    user = "-",
                    isForeground = false,
                    cpuUsage = "0.0%",
                    threads = "0",
                    totalRamKb = 0L,
                    processes = emptyList(),
                  ),
                )
              }
              return@execute
            }
            val command = "${ShellManager.toyboxPath()} ps -A -o pid,user,rss,name | grep '\\.' | grep -v '[-@]' | grep '" + app.packageName + "'"
            val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
            var processes = mutableListOf<com.yassernull.shappky.data.models.ProcessInfo>()
            var mainPid = "-"
            var mainUser = "-"
            var totalCpu = 0.0
            var totalThreads = 0
            var totalRam = 0L
            var isForeground = false

            if (fullOutput != null) {
              processes = parsePsOutputToProcessInfos(fullOutput, app.packageName).toMutableList()

              var mainFound = false
              for (p in processes) {
                if (p.name == app.packageName) {
                  mainPid = p.pid
                  mainUser = "-"
                  mainFound = true
                }
                totalRam += p.ramKb
                try {
                  val statOutput = shellManager.runShellCommandAndGetFullOutput("cat /proc/${p.pid}/stat")
                  if (statOutput != null) {
                    totalThreads += parseStatForThreads(statOutput)
                  }
                } catch (_: Exception) {}
              }
              if (!mainFound && processes.isNotEmpty()) {
                mainPid = processes[0].pid
                mainUser = "N/A"
              }

              val entries = parsePsOutputToEntries(fullOutput)
              val byUser = mutableMapOf<String, String>()
              BufferedReader(java.io.StringReader(fullOutput)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                  val parts = line.trim().split(Regex("\\s+"))
                  if (parts.size >= 4 && !line.startsWith("ERROR:")) {
                    val pid = parts[0]
                    val user = parts[1]
                    val name = parts[3]
                    if (name == app.packageName || name.startsWith(app.packageName + ":")) {
                      if (name == app.packageName) mainUser = user
                    }
                  }
                  line = reader.readLine()
                }
              }

              try {
                val cpuOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys cpuinfo | grep " + app.packageName)
                if (cpuOutput != null && !cpuOutput.startsWith("ERROR")) {
                  totalCpu = parseCpuInfoOutput(cpuOutput)
                }
              } catch (_: Exception) {}
            }

            val dumpCommand = "dumpsys activity services " + app.packageName + " | grep isForeground=true"
            val dumpOutput = shellManager.runShellCommandAndGetFullOutput(dumpCommand)
            isForeground = !dumpOutput.isNullOrBlank() && !dumpOutput.startsWith("ERROR")

            if (mainPid == "-" && processes.isNotEmpty()) {
              mainPid = processes[0].pid
              mainUser = "N/A"
            }

            val result = com.yassernull.shappky.data.models.AppDetailedInfo(
              app = app,
              pid = mainPid,
              user = mainUser,
              isForeground = isForeground,
              cpuUsage = String.format(java.util.Locale.US, "%.1f%%", totalCpu),
              threads = totalThreads.toString(),
              totalRamKb = totalRam,
              processes = processes,
            )
            handler.post { callback.accept(result) }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error loading detailed info", e)
        }
      }
    }
  }

  fun getHiddenApps(): Set<String> = HashSet(sharedPreferences.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet())

  fun saveHiddenApps(hiddenApps: Set<String>) {
    sharedPreferences.edit().putStringSet(KEY_HIDDEN_APPS, HashSet(hiddenApps)).apply()
  }

  fun getAppsList(): List<AppModel> = ArrayList(currentAppsList)

  companion object {
    private const val TAG = "ShappkyApps"
    private const val PSS_CACHE_TTL_MS = 8_000L
    const val PREFERENCES_NAME = "AppPreferences"
    const val KEY_HIDDEN_APPS = "hidden_apps"
  }
}
