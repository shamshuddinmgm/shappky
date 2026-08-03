package com.yassernull.shappky.core.managers

import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale

data class PsEntry(
  val packageName: String,
  val rssKb: Long,
)

fun parsePsOutputToEntries(output: String): List<PsEntry> {
  val entries = mutableListOf<PsEntry>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.size >= 2) {
        var rawPackageName = parts[1].trim()
        // Strip kernel bracket wrappers: [irq/foo.bar]
        if (rawPackageName.startsWith("[") && rawPackageName.endsWith("]")) {
          rawPackageName = rawPackageName.removeSurrounding("[", "]")
        }
        val packageName = if (rawPackageName.contains(":")) {
          rawPackageName.substringBefore(":")
        } else {
          rawPackageName
        }
        val rssKb = parts[0].trim().toLongOrNull() ?: 0L
        if (packageName.isNotEmpty() && packageName.contains(".") && !packageName.startsWith("ERROR:")) {
          entries.add(PsEntry(packageName, rssKb))
        }
      }
      line = reader.readLine()
    }
  }
  return entries
}

/** Sum PSS across processes of the same package (PSS is already proportional — summing is correct). */
fun aggregateByPackage(entries: List<PsEntry>): Map<String, Long> {
  val map = mutableMapOf<String, Long>()
  for (entry in entries) {
    map[entry.packageName] = (map[entry.packageName] ?: 0L) + entry.rssKb
  }
  return map
}

/**
 * RSS fallback only: take the **largest** process RSS per package.
 * Summing RSS double-counts shared zygote/framework pages and wildly inflates the list.
 */
fun aggregateByPackageMaxRss(entries: List<PsEntry>): Map<String, Long> {
  val map = mutableMapOf<String, Long>()
  for (entry in entries) {
    val prev = map[entry.packageName] ?: 0L
    if (entry.rssKb > prev) map[entry.packageName] = entry.rssKb
  }
  return map
}

/**
 * Parse `dumpsys meminfo -s` style / smaps probe lines: `"<pssKb> <processName>"`.
 * Process names like `pkg:service` collapse to `pkg`.
 */
fun parsePssProbeOutput(output: String): List<PsEntry> {
  val entries = mutableListOf<PsEntry>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val trimmed = line.trim()
      if (trimmed.isEmpty() || trimmed.startsWith("ERROR")) {
        line = reader.readLine()
        continue
      }
      val parts = trimmed.split(Regex("\\s+"), limit = 2)
      if (parts.size >= 2) {
        val pssKb = parts[0].toLongOrNull() ?: 0L
        var name = parts[1].trim()
        if (name.startsWith("[") && name.endsWith("]")) {
          name = name.removeSurrounding("[", "]")
        }
        val packageName = name.substringBefore(":")
        if (pssKb > 0L && packageName.contains('.') && !packageName.startsWith("ERROR:")) {
          entries.add(PsEntry(packageName, pssKb))
        }
      }
      line = reader.readLine()
    }
  }
  return entries
}

private val MEMINFO_PID_HEADER = Regex("""\*\*\s*MEMINFO in pid\s+\d+\s*\[([^\]]+)\]\s*\*\*""")
private val MEMINFO_TOTAL_PSS = Regex("""TOTAL PSS:\s+(\d+)""")

/**
 * Aggregate **TOTAL PSS** from `dumpsys meminfo -s` (or full package dumps) by package.
 * Process names like `pkg:service` collapse to `pkg`; PSS is summed across processes.
 */
fun parseDumpsysMeminfoPssByPackage(output: String): Map<String, Long> {
  val map = mutableMapOf<String, Long>()
  var currentPackage: String? = null
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val trimmed = line.trim()
      if (trimmed.startsWith("ERROR")) {
        line = reader.readLine()
        continue
      }
      val header = MEMINFO_PID_HEADER.find(trimmed)
      if (header != null) {
        var name = header.groupValues[1].trim()
        if (name.startsWith("[") && name.endsWith("]")) {
          name = name.removeSurrounding("[", "]")
        }
        val pkg = name.substringBefore(":")
        currentPackage = if (pkg.contains('.')) pkg else null
        line = reader.readLine()
        continue
      }
      val pssMatch = MEMINFO_TOTAL_PSS.find(trimmed)
      if (pssMatch != null) {
        val pssKb = pssMatch.groupValues[1].toLongOrNull() ?: 0L
        val pkg = currentPackage
        if (pkg != null && pssKb > 0L) {
          map[pkg] = (map[pkg] ?: 0L) + pssKb
        }
        // Only the App Summary TOTAL PSS line; ignore further TOTAL lines in the same block.
        currentPackage = null
      }
      line = reader.readLine()
    }
  }
  return map
}

fun parsePsOutputToProcessInfos(
  output: String,
  packageName: String,
): List<com.yassernull.shappky.data.models.ProcessInfo> {
  val processes = mutableListOf<com.yassernull.shappky.data.models.ProcessInfo>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.size >= 4 && !line.startsWith("ERROR:")) {
        val pid = parts[0]
        val rss = parts[2].toLongOrNull() ?: 0L
        val name = parts[3]
        if (name.startsWith(packageName)) {
          processes.add(com.yassernull.shappky.data.models.ProcessInfo(name, pid, rss))
        }
      }
      line = reader.readLine()
    }
  }
  return processes
}

fun parseCpuInfoOutput(cpuOutput: String): Double {
  var totalCpu = 0.0
  BufferedReader(StringReader(cpuOutput)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.isNotEmpty() && parts[0].endsWith("%")) {
        val percentStr = parts[0].removeSuffix("%")
        totalCpu += percentStr.toDoubleOrNull() ?: 0.0
      }
      line = reader.readLine()
    }
  }
  return totalCpu
}

fun parseStatForThreads(statOutput: String): Int {
  if (statOutput.startsWith("ERROR")) return 0
  val statParts = statOutput.trim().split(" ")
  return if (statParts.size >= 20) statParts[19].toIntOrNull() ?: 0 else 0
}

fun parseMemoryToKb(ram: String?): Long {
  if (ram.isNullOrEmpty() || ram == "-") return 0
  val normalizedRam = ram.trim().uppercase(Locale.getDefault())
  return try {
    when {
      normalizedRam.endsWith("KB") -> normalizedRam.replace("KB", "").trim().toFloat().toLong()
      normalizedRam.endsWith("MB") -> (normalizedRam.replace("MB", "").trim().toFloat() * 1024).toLong()
      normalizedRam.endsWith("GB") -> (normalizedRam.replace("GB", "").trim().toFloat() * 1024 * 1024).toLong()
      else -> 0
    }
  } catch (_: NumberFormatException) {
    0
  }
}
