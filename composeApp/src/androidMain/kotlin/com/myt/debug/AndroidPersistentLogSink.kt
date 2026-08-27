package com.myt.debug

import android.content.Context
import java.io.File

class AndroidPersistentLogSink(
    context: Context,
    private val maxBytes: Long = 1_500_000L,
) : PersistentLogSink {
    private val dir: File = File(context.filesDir, "debug_logs").also { it.mkdirs() }
    private val runtimeLog: File = File(dir, "myt-runtime.log")
    private val lock = Any()

    override fun append(entry: LogEntry) {
        if (entry.level.ordinal < LogLevel.Warn.ordinal) return
        appendRaw(entry.formattedLine())
    }

    override fun appendRaw(line: String) {
        synchronized(lock) {
            runCatching {
                rotateIfNeeded()
                runtimeLog.appendText(line.trimEnd() + "\n", Charsets.UTF_8)
            }
        }
    }

    override fun flush() {
        // FileChannel sync would be nicer; appendText is durable enough for crash triage.
    }

    override fun currentLogFilePath(): String = runtimeLog.absolutePath

    override fun readTail(maxChars: Int): String = synchronized(lock) {
        runCatching {
            if (!runtimeLog.exists()) return ""
            val text = runtimeLog.readText(Charsets.UTF_8)
            if (text.length <= maxChars) text else text.takeLast(maxChars)
        }.getOrDefault("")
    }

    override fun listLogFiles(): List<String> = synchronized(lock) {
        dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.absolutePath }
            .orEmpty()
    }

    private fun rotateIfNeeded() {
        if (!runtimeLog.exists() || runtimeLog.length() < maxBytes) return
        val rotated = File(dir, "myt-runtime-${System.currentTimeMillis()}.log")
        runtimeLog.renameTo(rotated)
        // Keep last 5 rotated files
        dir.listFiles()
            ?.filter { it.name.startsWith("myt-runtime-") && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(5)
            ?.forEach { it.delete() }
    }
}

class AndroidPendingIssueStore(
    context: Context,
) : PendingIssueStore {
    private val dir: File = File(context.filesDir, "pending_github_issues").also { it.mkdirs() }
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override fun enqueue(issue: PendingGitHubIssue) {
        val file = File(dir, "${issue.id}.json")
        file.writeText(json.encodeToString(PendingGitHubIssue.serializer(), issue), Charsets.UTF_8)
        // Human-readable sibling for adb / agent
        File(dir, "${issue.id}.md").writeText(
            buildString {
                appendLine("# ${issue.title}")
                appendLine()
                appendLine(issue.body)
            },
            Charsets.UTF_8,
        )
    }

    override fun listPending(): List<PendingGitHubIssue> =
        dir.listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedBy { it.name }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString(PendingGitHubIssue.serializer(), file.readText(Charsets.UTF_8))
                }.getOrNull()
            }
            .orEmpty()

    override fun markUploaded(id: String, url: String, number: Int?) {
        update(id) { it.copy(uploaded = true, githubIssueUrl = url, githubIssueNumber = number, uploadError = null) }
    }

    override fun markUploadFailed(id: String, error: String) {
        update(id) { it.copy(uploadError = error.take(500)) }
    }

    override fun delete(id: String) {
        File(dir, "$id.json").delete()
        File(dir, "$id.md").delete()
    }

    private fun update(id: String, transform: (PendingGitHubIssue) -> PendingGitHubIssue) {
        val file = File(dir, "$id.json")
        if (!file.exists()) return
        val current = runCatching {
            json.decodeFromString(PendingGitHubIssue.serializer(), file.readText(Charsets.UTF_8))
        }.getOrNull() ?: return
        enqueue(transform(current))
    }
}
