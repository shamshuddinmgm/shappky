package com.yassernull.shappky.core.managers

import android.os.Handler
import android.os.SystemClock
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

data class RamState(
  val usedKb: Int = 0,
  val totalKb: Int = 0,
) {
  val progress: Float
    get() = if (totalKb > 0) usedKb.toFloat() / totalKb else 0f
}

class RamMonitorManager(
  private val handler: Handler,
  private var refreshIntervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS,
  private val onUpdate: (RamState) -> Unit,
) {
  private var isMonitoring = false
  private var ramUsageRunnable: Runnable? = null

  /** After a kill, keep displayed used ≤ this until /proc/meminfo catches up. */
  @Volatile
  private var holdUsedAtMostKb: Int? = null

  @Volatile
  private var holdUntilElapsedMs: Long = 0L

  fun startMonitoring() {
    if (isMonitoring) return
    isMonitoring = true
    ramUsageRunnable = object : Runnable {
      override fun run() {
        if (!isMonitoring) return
        readRamState()?.let { publish(it) }
        handler.postDelayed(this, refreshIntervalMs)
      }
    }
    handler.post(requireNotNull(ramUsageRunnable))
  }

  fun setRefreshIntervalMs(intervalMs: Long) {
    refreshIntervalMs = intervalMs.coerceAtLeast(1L)
  }

  fun refreshNow() {
    handler.post {
      readRamState()?.let { publish(it) }
    }
  }

  /**
   * Show freed memory immediately using the killed app's **PSS** figure, and hold that
   * lower used-value until MemAvailable reflects it (or the hold window expires).
   */
  fun applyOptimisticFree(freedKb: Long, current: RamState, holdMs: Long = 3500L) {
    if (freedKb <= 0L || current.totalKb <= 0) {
      refreshNow()
      return
    }
    val targetUsed = (current.usedKb - freedKb.toInt()).coerceIn(0, current.totalKb)
    holdUsedAtMostKb = targetUsed
    holdUntilElapsedMs = SystemClock.elapsedRealtime() + holdMs.coerceAtLeast(500L)
    handler.post {
      onUpdate(RamState(usedKb = targetUsed, totalKb = current.totalKb))
      // Rechecks: publish() keeps the optimistic floor until meminfo drops.
      handler.postDelayed({ readRamState()?.let { publish(it) } }, 500L)
      handler.postDelayed({ readRamState()?.let { publish(it) } }, 1500L)
      handler.postDelayed({ readRamState()?.let { publish(it) } }, holdMs)
    }
  }

  fun applyFreedMemory(freedKb: Long, recheckDelayMs: Long = 400L) {
    // Back-compat: just schedule meminfo re-reads (optimistic already applied separately).
    handler.post {
      readRamState()?.let { publish(it) }
      handler.postDelayed({ readRamState()?.let { publish(it) } }, recheckDelayMs.coerceAtLeast(0L))
      handler.postDelayed({ readRamState()?.let { publish(it) } }, (recheckDelayMs + 1200L).coerceAtLeast(1200L))
    }
  }

  fun stopMonitoring() {
    isMonitoring = false
    ramUsageRunnable?.let { handler.removeCallbacks(it) }
    ramUsageRunnable = null
    holdUsedAtMostKb = null
  }

  private fun publish(real: RamState) {
    val hold = holdUsedAtMostKb
    val now = SystemClock.elapsedRealtime()
    if (hold != null && now < holdUntilElapsedMs && real.usedKb > hold) {
      // System still reports "used" higher than post-kill PSS free — keep optimistic.
      onUpdate(real.copy(usedKb = hold))
      return
    }
    if (hold != null && (now >= holdUntilElapsedMs || real.usedKb <= hold + HOLD_SLACK_KB)) {
      holdUsedAtMostKb = null
    }
    onUpdate(real)
  }

  private fun readRamState(): RamState? = try {
    val process = Runtime.getRuntime().exec("cat /proc/meminfo")
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
      var memMax = 0
      var memFree = 0
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        val current = line ?: break
        when {
          current.startsWith("MemTotal:") ->
            memMax = current.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull() ?: memMax
          current.startsWith("MemAvailable:") ->
            memFree = current.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull() ?: memFree
        }
        if (memMax > 0 && memFree > 0) break
      }
      process.waitFor()
      if (memMax > 0 && memFree >= 0) RamState(memMax - memFree, memMax) else null
    }
  } catch (e: IOException) {
    e.printStackTrace()
    null
  } catch (e: NumberFormatException) {
    e.printStackTrace()
    null
  } catch (e: InterruptedException) {
    Thread.currentThread().interrupt()
    e.printStackTrace()
    null
  }

  private companion object {
    const val DEFAULT_REFRESH_INTERVAL_MS = 1000L
    const val HOLD_SLACK_KB = 8 * 1024 // 8 MB — treat as caught up
  }
}
