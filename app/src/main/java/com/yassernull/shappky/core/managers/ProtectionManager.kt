package com.yassernull.shappky.core.managers

import android.content.Context
import android.util.Log

object ProtectionManager {
  private const val TAG = "ProtectionManager"
  private const val KEY_PROTECTED_APPS = "protectedApps"
  private const val KEY_PROTECTED_REGEX = "protectedRegex"
  private const val PREFERENCES_NAME = "AppPreferences"

  /**
   * User-chosen protected packages only.
   * Fresh install = empty. No auto-seeded launcher/keyboard/system/Google lists.
   */
  fun getProtectedApps(context: Context): Set<String> {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    if (!sharedPrefs.contains(KEY_PROTECTED_APPS)) {
      return emptySet()
    }
    return HashSet(sharedPrefs.getStringSet(KEY_PROTECTED_APPS, emptySet()) ?: emptySet())
  }

  fun saveProtectedApps(context: Context, apps: Set<String>) {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putStringSet(KEY_PROTECTED_APPS, HashSet(apps)).apply()
    Log.d(TAG, "Saved protected apps count=${apps.size}")
  }

  /**
   * User-chosen regex only. Fresh install = blank (no OEM auto patterns).
   */
  fun getProtectedRegex(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_PROTECTED_REGEX, "") ?: ""
  }

  fun saveProtectedRegex(context: Context, regex: String) {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_PROTECTED_REGEX, regex).apply()
  }

  /**
   * Only Shappky itself is hard-protected (cannot kill the app running the kill).
   * Everything else is solely what the user put in the protected list / regex.
   */
  fun isProtected(context: Context, packageName: String): Boolean {
    if (packageName.isEmpty()) return true
    if (packageName == context.packageName) return true
    if (getProtectedApps(context).contains(packageName)) return true
    return isAppProtectedByRegex(context, packageName)
  }

  fun isAppProtectedByRegex(context: Context, packageName: String): Boolean {
    val regexStr = getProtectedRegex(context)
    if (regexStr.isBlank()) return false

    val patterns = regexStr.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    for (pattern in patterns) {
      try {
        val regex = pattern.replace(".", "\\.").replace("*", ".*").toRegex()
        if (regex.matches(packageName)) return true
      } catch (_: Exception) {
        if (pattern.endsWith(".*") && packageName.startsWith(pattern.removeSuffix(".*"))) {
          return true
        } else if (packageName == pattern) {
          return true
        }
      }
    }
    return false
  }
}
