package com.myt.platform

import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual class CrashReporterPlatform actual constructor(context: Any) {
    private val crashPath: String =
        NSHomeDirectory() + "/Documents/myt-last-crash.txt"

    private var installed: Boolean = false

    actual fun install() {
        if (installed) return
        installed = true
        setUnhandledExceptionHook { throwable ->
            val report = buildString {
                appendLine("MyT Crash Report (iOS local)")
                appendLine("Exception: ${throwable::class.qualifiedName}")
                appendLine("Message: ${throwable.message}")
                appendLine()
                appendLine(throwable.stackTraceToString())
            }
            runCatching {
                NSString.create(string = report).writeToFile(
                    path = crashPath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null,
                )
            }
            throw throwable
        }
    }

    actual fun lastCrashReport(): String? = runCatching {
        if (!NSFileManager.defaultManager.fileExistsAtPath(crashPath)) return null
        NSString.stringWithContentsOfFile(
            path = crashPath,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }.getOrNull()

    actual fun clearLastCrashReport() {
        runCatching {
            NSFileManager.defaultManager.removeItemAtPath(crashPath, error = null)
        }
    }
}
