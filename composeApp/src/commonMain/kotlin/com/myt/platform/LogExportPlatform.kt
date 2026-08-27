package com.myt.platform

/** Writes a log file and opens the system share / email sheet (Gmail when available). */
expect class LogExportPlatform(context: Any) {
    fun shareLogFile(content: String, fileName: String): Result<Unit>
}
