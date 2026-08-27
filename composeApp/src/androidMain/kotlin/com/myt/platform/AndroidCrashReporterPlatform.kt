package com.myt.platform

import android.content.Context
import com.myt.debug.AndroidPersistentLogSink
import java.io.File
import kotlin.system.exitProcess

actual class CrashReporterPlatform actual constructor(context: Any) {
    private val appContext = context as Context
    private val crashFile: File = File(appContext.filesDir, "crash_reports/myt-last-crash.txt")
    private val legacyCrashFile: File = File(appContext.cacheDir, "myt-last-crash.txt")

    @Volatile
    private var installed: Boolean = false

    @Volatile
    private var logSink: AndroidPersistentLogSink? = null

    fun attachLogSink(sink: AndroidPersistentLogSink) {
        logSink = sink
    }

    actual fun install() {
        if (installed) return
        installed = true

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val report = buildString {
                appendLine("MyT Crash Report (local)")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable::class.qualifiedName}")
                appendLine("Message: ${throwable.message}")
                appendLine()
                appendLine(throwable.stackTraceToString())
            }

            runCatching {
                crashFile.parentFile?.mkdirs()
                crashFile.writeText(report)
                // Keep legacy path for older tooling
                legacyCrashFile.parentFile?.mkdirs()
                legacyCrashFile.writeText(report)
            }
            runCatching {
                logSink?.appendRaw("--- FATAL ${throwable::class.simpleName}: ${throwable.message} ---")
                logSink?.appendRaw(throwable.stackTraceToString().lineSequence().take(80).joinToString("\n"))
                logSink?.flush()
            }

            previous?.uncaughtException(thread, throwable) ?: exitProcess(10)
        }
    }

    actual fun lastCrashReport(): String? = runCatching {
        when {
            crashFile.exists() -> crashFile.readText()
            legacyCrashFile.exists() -> legacyCrashFile.readText()
            else -> null
        }
    }.getOrNull()

    actual fun clearLastCrashReport() {
        runCatching { crashFile.delete() }
        runCatching { legacyCrashFile.delete() }
    }
}
