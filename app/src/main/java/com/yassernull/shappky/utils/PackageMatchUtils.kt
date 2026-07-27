package com.yassernull.shappky.utils

/** Exact package-token match helpers for dumpsys / process name filtering. */
object PackageMatchUtils {
  /** True if [dump] contains [packageName] as a whole package token (not a prefix of another). */
  fun dumpContainsPackage(dump: String, packageName: String): Boolean {
    if (packageName.isEmpty() || dump.isEmpty()) return false
    var idx = dump.indexOf(packageName)
    while (idx >= 0) {
      val beforeOk = idx == 0 || !isPackageChar(dump[idx - 1])
      val after = idx + packageName.length
      val afterOk = after >= dump.length || !isPackageChar(dump[after])
      if (beforeOk && afterOk) return true
      idx = dump.indexOf(packageName, idx + 1)
    }
    return false
  }

  private fun isPackageChar(c: Char): Boolean = c.isLetterOrDigit() || c == '.' || c == '_'
}
