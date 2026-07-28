package com.yassernull.shappky.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.activities.main.AppsCategory

object MainScreensPrefs {

  fun visibilityKey(category: AppsCategory): String = when (category) {
    AppsCategory.ALL -> AppsListPreferences.KEY_SCREEN_ALL
    AppsCategory.USER -> AppsListPreferences.KEY_SCREEN_USER
    AppsCategory.SYSTEM -> AppsListPreferences.KEY_SCREEN_SYSTEM
    AppsCategory.PERSISTENT -> AppsListPreferences.KEY_SCREEN_PERSISTENT
    AppsCategory.PROTECTED -> AppsListPreferences.KEY_SCREEN_PROTECTED
    AppsCategory.SERVICES -> AppsListPreferences.KEY_SCREEN_SERVICES
  }

  fun defaultVisible(category: AppsCategory): Boolean = when (category) {
    AppsCategory.SERVICES -> false
    else -> true
  }

  fun isVisible(prefs: SharedPreferences, category: AppsCategory): Boolean = prefs.getBoolean(visibilityKey(category), defaultVisible(category))

  fun loadOrder(prefs: SharedPreferences): List<AppsCategory> {
    val raw = prefs.getString(
      AppsListPreferences.KEY_SCREEN_ORDER,
      AppsListPreferences.DEFAULT_SCREEN_ORDER,
    ) ?: AppsListPreferences.DEFAULT_SCREEN_ORDER
    val parsed = raw.split(',')
      .mapNotNull { token -> runCatching { AppsCategory.valueOf(token.trim()) }.getOrNull() }
      .distinct()
    val missing = AppsCategory.entries.filter { it !in parsed }
    return parsed + missing
  }

  fun saveOrder(prefs: SharedPreferences, order: List<AppsCategory>) {
    prefs.edit()
      .putString(AppsListPreferences.KEY_SCREEN_ORDER, order.joinToString(",") { it.name })
      .apply()
  }

  fun titleRes(category: AppsCategory): Int = when (category) {
    AppsCategory.ALL -> R.string.screen_all
    AppsCategory.USER -> R.string.screen_user
    AppsCategory.SYSTEM -> R.string.screen_system
    AppsCategory.PERSISTENT -> R.string.screen_persistent
    AppsCategory.PROTECTED -> R.string.screen_protected
    AppsCategory.SERVICES -> R.string.screen_services
  }

  fun loadVisibleScreens(prefs: SharedPreferences, context: Context): List<Pair<AppsCategory, String>> {
    val visible = loadOrder(prefs)
      .filter { isVisible(prefs, it) }
      .map { it to context.getString(titleRes(it)) }
    return visible.ifEmpty {
      listOf(AppsCategory.ALL to context.getString(R.string.screen_all))
    }
  }
}
