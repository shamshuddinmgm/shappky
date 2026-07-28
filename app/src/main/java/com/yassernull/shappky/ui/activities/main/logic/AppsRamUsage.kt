package com.yassernull.shappky.ui.activities.main.logic

import androidx.compose.runtime.Composable
import com.yassernull.shappky.ui.activities.main.MainActivity
import com.yassernull.shappky.ui.components.RamUsageBar

@Composable
fun AppsRamUsage() {
  // Read here only — keeps system RAM ticks off the category pager composition path.
  RamUsageBar(AppsListLogic.ramState)
}

object AppsRamUsageLogic {
  private const val TAG = "AppsRamUsageLogic"

  fun refreshAppsRamUsage(activity: MainActivity) {
    android.util.Log.d(
      TAG,
      "refreshAppsRamUsage requested hasPermission=${AppsListLogic.hasPermission}, listSize=${AppsListLogic.appsDataList.size}",
    )
    if (!AppsListLogic.hasPermission || AppsListLogic.appsDataList.isEmpty()) {
      return
    }
    // Never thrash the list UI while the category pager is mid-swipe.
    if (AppsListLogic.pagerScrollInProgress) return

    AppsListLogic.appManager.loadAppsRamUsage(AppsListLogic.appsDataList.map { it.packageName }) { ramUsageByPackage ->
      if (ramUsageByPackage.isEmpty()) return@loadAppsRamUsage
      if (AppsListLogic.pagerScrollInProgress) return@loadAppsRamUsage

      var changed = false
      for (i in AppsListLogic.appsDataList.indices) {
        val app = AppsListLogic.appsDataList[i]
        val newRamKb = ramUsageByPackage[app.packageName] ?: continue
        if (newRamKb == app.ramKb) continue
        AppsListLogic.appsDataList[i] = app.copy(
          ramKb = newRamKb,
          appRam = if (newRamKb > 0) AppsListLogic.appManager.formatMemorySize(newRamKb) else app.appRam,
        )
        changed = true
      }
      if (!changed) return@loadAppsRamUsage

      val prefs = activity.getSharedPreferences(
        com.yassernull.shappky.core.preferences.PREFERENCES_NAME,
        android.content.Context.MODE_PRIVATE,
      )
      if (
        prefs.getString(
          com.yassernull.shappky.core.preferences.AppsListPreferences.KEY_SORT_MODE,
          com.yassernull.shappky.core.preferences.AppsListPreferences.SORT_BY_NAME,
        ) == com.yassernull.shappky.core.preferences.AppsListPreferences.SORT_BY_RAM
      ) {
        AppsListLogic.sortAppsDataList(activity)
      }
    }
  }
}
