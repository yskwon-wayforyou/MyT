package com.myt.debug

import com.myt.platform.AppInfo
import kotlinx.datetime.Clock

object DebugLogExporter {
    fun buildReport(
        entries: List<LogEntry>,
        appInfo: AppInfo,
        extraContext: Map<String, String> = emptyMap(),
        clock: Clock = Clock.System,
    ): String = buildString {
        val now = clock.now().toEpochMilliseconds()
        appendLine("MyT Debug Report")
        appendLine("Generated: ${formatEpoch(now)}")
        appendLine("App: ${appInfo.appVersion} (${appInfo.buildLabel})")
        appendLine("OS: ${appInfo.osDescription}")
        appendLine("Device: ${appInfo.deviceDescription}")
        appendLine("Platform: ${appInfo.platformLabel}")
        extraContext.forEach { (k, v) -> appendLine("$k: ${LogRedactor.redact(v)}") }
        appendLine("Entry count: ${entries.size}")
        appendLine()
        appendLine("--- LOG ---")
        entries.forEach { appendLine(it.formattedLine()) }
    }

    fun defaultFileName(clock: Clock = Clock.System): String {
        val stamp = formatEpoch(clock.now().toEpochMilliseconds())
            .replace(' ', '_')
            .replace(':', '-')
            .take(19)
        return "myt-debug-$stamp.txt"
    }

    private fun formatEpoch(epochMs: Long): String =
        kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toString()
}
