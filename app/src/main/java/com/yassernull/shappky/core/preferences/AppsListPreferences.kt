package com.yassernull.shappky.core.preferences

object AppsListPreferences {
  const val KEY_SHOW_USER_APPS = "showUserApps"
  const val KEY_SHOW_SYSTEM_APPS = "showSystemApps"
  const val KEY_SHOW_PERSISTENT_APPS = "showPersistentApps"
  const val KEY_SHOW_PROTECTED_APPS = "showProtectedApps"
  const val KEY_SHOW_SERVICE_PROCESSES = "showServiceProcesses"
  const val KEY_SHOW_APP_TYPE_ICONS = "showAppTypeIcons"

  /** Category pager screens (Settings → Main screens). */
  const val KEY_SCREEN_ALL = "showScreenAll"
  const val KEY_SCREEN_USER = "showScreenUser"
  const val KEY_SCREEN_SYSTEM = "showScreenSystem"
  const val KEY_SCREEN_PERSISTENT = "showScreenPersistent"
  const val KEY_SCREEN_PROTECTED = "showScreenProtected"
  const val KEY_SCREEN_SERVICES = "showScreenServices"

  /** Comma-separated [com.yassernull.shappky.ui.activities.main.AppsCategory] names. */
  const val KEY_SCREEN_ORDER = "screenOrder"
  const val DEFAULT_SCREEN_ORDER = "ALL,USER,SYSTEM,PERSISTENT,PROTECTED,SERVICES"

  const val KEY_SORT_MODE = "sortMode"
  const val KEY_SORT_DESCENDING = "sortDescending"

  const val SORT_BY_NAME = "name"
  const val SORT_BY_RAM = "ram"
  const val SORT_BY_TYPE = "type"

  const val KEY_APPS_AUTO_REFRESH = "appsAutoRefresh"
  const val KEY_APPS_RAM_USAGE_AUTO_REFRESH = "appsRamUsageAutoRefresh"
  const val KEY_APPS_AUTO_REFRESH_INTERVAL_MS = "appsAutoRefreshIntervalMs"
  const val KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = "appsRamUsageRefreshIntervalMs"

  /** Explicit user opt-in for the background killer foreground service. Default OFF. */
  const val KEY_SERVICE_ENABLED = "service_enabled"

  const val DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS = 1000L
  const val DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = 1000L
}
