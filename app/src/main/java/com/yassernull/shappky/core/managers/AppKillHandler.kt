package com.yassernull.shappky.core.managers

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.utils.PackageMatchUtils

class AppKillHandler(
  private val context: Context,
  private val handler: Handler,
  private val shellManager: ShellManager,
) {
  fun killPackages(
    packageNames: List<String>?,
    onComplete: Runnable?,
    showToast: Boolean = true,
    getAppRamKb: ((String) -> Long)? = null,
    formatMemorySize: (Long) -> String,
  ) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      onComplete?.let { handler.post(it) }
      return
    }

    if (packageNames.isNullOrEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    val safePackageNames = packageNames
      .filter { PackageMatchUtils.isValidAndroidPackageName(it) }
      .filter { !ProtectionManager.isProtected(context, it) }

    if (safePackageNames.isEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    var totalKb = 0L
    for (pkg in safePackageNames) {
      totalKb += getAppRamKb?.invoke(pkg) ?: 0L
    }

    val command = buildSmartKillCommand(safePackageNames)
    if (command.isEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }
    shellManager.runShellCommand(command, onComplete)
    if (showToast) {
      val message = context.getString(R.string.free_up_memory, formatMemorySize(totalKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  fun killApp(
    packageName: String?,
    onComplete: Runnable?,
    forceKill: Boolean = false,
    getAppRamKb: ((String) -> Long)? = null,
    formatMemorySize: (Long) -> String,
  ) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      onComplete?.let { handler.post(it) }
      return
    }
    if (!PackageMatchUtils.isValidAndroidPackageName(packageName)) {
      Log.w(TAG, "Rejected invalid package name for kill")
      onComplete?.let { handler.post(it) }
      return
    }
    val pkg = packageName!!

    if (!forceKill && ProtectionManager.isProtected(context, pkg)) {
      onComplete?.let { handler.post(it) }
      return
    }

    val command = buildSmartKillCommand(listOf(pkg))
    if (command.isEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }
    shellManager.runShellCommand(command, onComplete)
    val ramKb = getAppRamKb?.invoke(pkg) ?: 0L
    if (ramKb > 0) {
      val message = context.getString(R.string.free_up_memory, formatMemorySize(ramKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  companion object {
    private const val TAG = "AppKillHandler"

    fun buildSmartKillCommand(packageNames: List<String>): String {
      val safe = packageNames.filter { PackageMatchUtils.isValidAndroidPackageName(it) }
      if (safe.isEmpty()) return ""
      val toybox = ShellManager.toyboxPath()
      val killCommands = safe.joinToString("; ") { "am kill $it" }
      val forceStopCommands = safe.joinToString("; ") {
        "if pidof $it > /dev/null; then am force-stop $it; fi"
      }
      // Exact process name or pkg:service — never substring-match sibling packages.
      // Package names are pre-validated; awk -v avoids embedding untrusted shell text.
      val kill9Commands = safe.joinToString("; ") { pkg ->
        "pids=\$($toybox ps -A -o pid,name | awk -v p='$pkg' '\$2==p || index(\$2, p \":\")==1 {print \$1}'); " +
          "if [ ! -z \"\$pids\" ]; then kill -9 \$pids 2>/dev/null; fi"
      }
      return "$killCommands; sleep 0.2; $forceStopCommands; sleep 0.2; $kill9Commands"
    }
  }
}
