package com.yassernull.shappky.utils

import com.yassernull.shappky.core.preferences.AppsListPreferences.SORT_BY_RAM
import com.yassernull.shappky.core.preferences.AppsListPreferences.SORT_BY_TYPE
import com.yassernull.shappky.data.models.AppModel
import java.util.Locale

object AppSortUtils {

  fun sortApps(
    appsList: List<AppModel>,
    sortMode: String?,
    descending: Boolean,
  ): List<AppModel> {
    val comparator = when (sortMode) {
      SORT_BY_RAM -> {
        // Pure RAM order — do NOT group by system/persistent first (that caused
        // high-RAM system apps to appear below low-RAM user apps).
        val ramComparator = if (descending) {
          compareByDescending<AppModel> { it.ramKb }
        } else {
          compareBy<AppModel> { it.ramKb }
        }
        ramComparator.thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
      }
      SORT_BY_TYPE -> {
        val typeComparator = if (descending) {
          compareByDescending<AppModel> { getTypePriority(it) }
        } else {
          compareBy<AppModel> { getTypePriority(it) }
        }
        typeComparator.thenBy { it.appName.lowercase(Locale.getDefault()) }
      }
      else -> {
        // Name: keep light type grouping so user apps cluster, then A–Z / Z–A.
        val appTypeComparator = compareBy<AppModel> { it.isSystemApp }.thenBy { it.isPersistentApp }
        val nameComparator = if (descending) {
          compareByDescending<AppModel> { it.appName.lowercase(Locale.getDefault()) }
        } else {
          compareBy { it.appName.lowercase(Locale.getDefault()) }
        }
        appTypeComparator.then(nameComparator)
      }
    }

    return appsList.sortedWith(comparator)
  }

  private fun getTypePriority(app: AppModel): Int = when {
    app.isProtected -> 4
    app.isPersistentApp -> 3
    app.isSystemApp -> 2
    else -> 1 // User app
  }
}
