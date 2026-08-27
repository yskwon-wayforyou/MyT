package com.myt.domain.usecase

import com.myt.debug.DebugLogExporter
import com.myt.debug.DebugLogger
import com.myt.debug.PendingIssueStore
import com.myt.debug.PersistentLogSink
import com.myt.platform.AppInfoPlatform
import com.myt.platform.CrashReporterPlatform
import com.myt.platform.LogExportPlatform

class DebugLogUseCase(
    private val logger: DebugLogger,
    private val appInfoPlatform: AppInfoPlatform,
    private val exportPlatform: LogExportPlatform,
    private val crashReporter: CrashReporterPlatform,
    private val fileSink: PersistentLogSink,
    private val pendingStore: PendingIssueStore? = null,
) {
    val entries = logger.entries

    var isEnabled: Boolean
        get() = logger.isEnabled
        set(value) {
            logger.isEnabled = value
        }

    fun runtimeLogPath(): String? = fileSink.currentLogFilePath()

    fun runtimeLogTail(maxChars: Int = 24_000): String = fileSink.readTail(maxChars)

    fun pendingIssueSummary(): String =
        pendingStore?.listPending()?.joinToString("\n") { p ->
            "${p.id} uploaded=${p.uploaded} ${p.title}"
        }.orEmpty()

    suspend fun clear() {
        logger.clear()
        logger.i("DebugLog", "Log buffer cleared")
    }

    suspend fun exportViaEmail(extraContext: Map<String, String> = emptyMap()): Result<Unit> {
        val entries = logger.snapshot()
        val appInfo = appInfoPlatform.collect()
        val crash = crashReporter.lastCrashReport()
        val mergedContext = buildMap {
            putAll(extraContext)
            if (!crash.isNullOrBlank()) put("last_crash_report", crash)
            fileSink.currentLogFilePath()?.let { put("runtime_log_path", it) }
            val pending = pendingIssueSummary()
            if (pending.isNotBlank()) put("pending_github_issues", pending)
            val tail = fileSink.readTail(8_000)
            if (tail.isNotBlank()) put("runtime_log_tail", tail)
        }
        val report = DebugLogExporter.buildReport(entries, appInfo, mergedContext)
        val fileName = DebugLogExporter.defaultFileName()
        logger.i("DebugLog", "Exporting ${entries.size} entries as $fileName")
        return exportPlatform.shareLogFile(report, fileName).also { result ->
            result.fold(
                onSuccess = { logger.i("DebugLog", "Share sheet opened") },
                onFailure = { logger.e("DebugLog", "Export failed: ${it.message}", it) },
            )
        }
    }
}
