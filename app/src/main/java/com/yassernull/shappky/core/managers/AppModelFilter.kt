package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.yassernull.shappky.data.models.AppModel

object AppModelFilter {

  /**
   * Builds the full running set (all app types + service processes).
   * More-menu checklist filters are applied later on the All screen only.
   */
  fun buildRunningAppModels(
    runningEntries: Set<String>,
    hiddenApps: Set<String>,
    protectedApps: Set<String>,
    serviceProcessEntries: Set<String>,
    context: Context,
    formatMemorySize: (Long) -> String,
    defaultIcon: Drawable,
  ): List<AppModel> {
    val pm = context.packageManager
    val result = mutableListOf<AppModel>()
    val seenPackages = HashSet<String>()

    for (packageEntry in runningEntries) {
      val parts = packageEntry.split(":")
      val packageName = parts[0]
      val ramUsage = parts.getOrNull(1)?.toLongOrNull() ?: 0L

      try {
        if (hiddenApps.contains(packageName)) continue

        val isProtected = ProtectionManager.isProtected(context, packageName)

        val appInfo = pm.getApplicationInfo(packageName, 0)
        val isPersistentApp = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
        val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val label = pm.getApplicationLabel(appInfo).toString()

        result.add(
          AppModel(
            appName = label,
            packageName = packageName,
            appRam = formatMemorySize(ramUsage),
            ramKb = ramUsage,
            appIcon = pm.getApplicationIcon(appInfo),
            isSystemApp = isSystemApp,
            isPersistentApp = isPersistentApp,
            isProtected = isProtected,
            isServiceProcess = false,
          ),
        )
        seenPackages.add(packageName)
      } catch (_: PackageManager.NameNotFoundException) {
      } catch (_: Exception) {
      }
    }

    for (entry in serviceProcessEntries) {
      val parts = entry.split(":")
      val processName = parts[0]
      val ramUsage = parts.getOrNull(1)?.toLongOrNull() ?: 0L
      if (processName.isEmpty() || hiddenApps.contains(processName)) continue
      if (processName in seenPackages) continue

      result.add(
        AppModel(
          appName = displayNameForProcess(processName),
          packageName = processName,
          appRam = formatMemorySize(ramUsage),
          ramKb = ramUsage,
          appIcon = defaultIcon,
          isSystemApp = true,
          isPersistentApp = false,
          isProtected = true,
          isServiceProcess = true,
        ),
      )
    }

    return result
  }

  /** Short label for HAL/vendor process names; avoids bare "0" from "...@4.0". */
  internal fun displayNameForProcess(processName: String): String {
    val leaf = processName.substringAfterLast('/').ifEmpty { processName }
    val noAt = leaf.substringBefore('@').ifEmpty { leaf }
    val segments = noAt.split('.').filter { it.isNotEmpty() }
    val simple = segments.lastOrNull().orEmpty()
    return when {
      simple.isEmpty() -> leaf
      simple.matches(Regex("\\d+")) || simple.length <= 1 ->
        segments.lastOrNull { !it.matches(Regex("\\d+")) && it.length > 1 } ?: noAt
      else -> simple
    }
  }

  fun buildAllAppsList(
    context: Context,
    protectedApps: Set<String>,
  ): List<AppModel> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val allApps = mutableListOf<AppModel>()
    for (appInfo in packages) {
      val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
      val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
      val label = pm.getApplicationLabel(appInfo).toString()
      val pkg = appInfo.packageName
      val isProtected = ProtectionManager.isProtected(context, pkg)

      allApps.add(
        AppModel(
          appName = label,
          packageName = pkg,
          appRam = "-",
          ramKb = 0L,
          appIcon = pm.getApplicationIcon(appInfo),
          isSystemApp = isSystem,
          isPersistentApp = isPersistent,
          isProtected = isProtected,
          isServiceProcess = false,
        ),
      )
    }
    allApps.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    return allApps
  }
}
