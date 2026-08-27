package com.myt.debug

import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

private fun docsDir(): String = NSHomeDirectory() + "/Documents"

class IosPersistentLogSink : PersistentLogSink {
    private val dir = docsDir() + "/debug_logs"
    private val runtimePath = "$dir/myt-runtime.log"

    init {
        NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    }

    override fun append(entry: LogEntry) {
        if (entry.level.ordinal < LogLevel.Warn.ordinal) return
        appendRaw(entry.formattedLine())
    }

    override fun appendRaw(line: String) {
        runCatching {
            val existing = NSString.stringWithContentsOfFile(
                path = runtimePath,
                encoding = NSUTF8StringEncoding,
                error = null,
            ).orEmpty()
            val next = existing + line.trimEnd() + "\n"
            NSString.create(string = next).writeToFile(
                path = runtimePath,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
        }
    }

    override fun flush() = Unit

    override fun currentLogFilePath(): String = runtimePath

    override fun readTail(maxChars: Int): String = runCatching {
        val text = NSString.stringWithContentsOfFile(
            path = runtimePath,
            encoding = NSUTF8StringEncoding,
            error = null,
        ).orEmpty()
        if (text.length <= maxChars) text else text.takeLast(maxChars)
    }.getOrDefault("")

    override fun listLogFiles(): List<String> = listOf(runtimePath)
}

class IosPendingIssueStore : PendingIssueStore {
    private val dir = docsDir() + "/pending_github_issues"
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    }

    override fun enqueue(issue: PendingGitHubIssue) {
        val path = "$dir/${issue.id}.json"
        val payload = json.encodeToString(PendingGitHubIssue.serializer(), issue)
        NSString.create(string = payload).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        val md = "# ${issue.title}\n\n${issue.body}"
        NSString.create(string = md).writeToFile(
            path = "$dir/${issue.id}.md",
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }

    override fun listPending(): List<PendingGitHubIssue> {
        // Lightweight: try known recent pattern by reading directory listing as string dump is fragile on K/N;
        // for Phase 1.5 iOS we keep JSON files and best-effort single-file read of fixed names is enough.
        // Prefer Android for auto GitHub filing; iOS still persists crash text via CrashReporterPlatform.
        return emptyList()
    }

    override fun markUploaded(id: String, url: String, number: Int?) = Unit

    override fun markUploadFailed(id: String, error: String) = Unit

    override fun delete(id: String) {
        val fm = NSFileManager.defaultManager
        fm.removeItemAtPath("$dir/$id.json", null)
        fm.removeItemAtPath("$dir/$id.md", null)
    }
}
