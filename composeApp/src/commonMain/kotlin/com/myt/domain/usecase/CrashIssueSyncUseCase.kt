package com.myt.domain.usecase

import com.myt.debug.CrashIssueFormatter
import com.myt.debug.DebugLogger
import com.myt.debug.GitHubIssueClient
import com.myt.debug.PendingGitHubIssue
import com.myt.debug.PendingIssueStore
import com.myt.debug.PersistentLogSink
import com.myt.platform.AppInfoPlatform
import com.myt.platform.CrashReporterPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * On cold start: upload any crash/error pending issues to GitHub (if token present).
 * Always keeps local pending files for agent `crash-log-triage` when offline / no token.
 */
class CrashIssueSyncUseCase(
    private val crashReporter: CrashReporterPlatform,
    private val pendingStore: PendingIssueStore,
    private val fileSink: PersistentLogSink,
    private val github: GitHubIssueClient,
    private val appInfoPlatform: AppInfoPlatform,
    private val debugLogger: DebugLogger,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {
    fun start() {
        scope.launch {
            runCatching { bootstrapFromLastCrash() }
            runCatching { flushPending() }
                .onFailure { debugLogger.e("CrashSync", "flush failed: ${it.message}", it) }
        }
    }

    /** Turn last local crash file into a pending GitHub issue (idempotent via crash clear). */
    fun bootstrapFromLastCrash() {
        val report = crashReporter.lastCrashReport()?.takeIf { it.isNotBlank() } ?: return
        val id = "crash-${clock.now().toEpochMilliseconds()}-${Random.nextInt(1000, 9999)}"
        val appInfo = runCatching { appInfoPlatform.collect() }.getOrNull()
        val logTail = fileSink.readTail()
        val title = CrashIssueFormatter.title(
            kind = "crash",
            throwableType = report.lineSequence().firstOrNull { it.startsWith("Exception:") }
                ?.removePrefix("Exception:")?.trim(),
            message = report.lineSequence().firstOrNull { it.startsWith("Message:") }
                ?.removePrefix("Message:")?.trim(),
        )
        val body = CrashIssueFormatter.body("crash", appInfo, report, logTail, id)
        pendingStore.enqueue(
            PendingGitHubIssue(
                id = id,
                createdAtMs = clock.now().toEpochMilliseconds(),
                kind = "crash",
                title = title,
                body = body,
                labels = listOf(
                    "crash",
                    "auto-reported",
                    appInfo?.platformLabel?.lowercase() ?: "android",
                ),
            ),
        )
        fileSink.appendRaw("--- crash bootstrap pending_id=$id ---")
        crashReporter.clearLastCrashReport()
        debugLogger.w("CrashSync", "Queued crash issue pending_id=$id")
    }

    fun enqueueError(tag: String, message: String, throwable: Throwable?) {
        // Delegated to ErrorIssueEnqueuer via DebugLogger; kept for API symmetry / tests.
    }

    suspend fun flushPending(): Int {
        val pending = pendingStore.listPending().filter { !it.uploaded }
        if (pending.isEmpty()) return 0
        var uploaded = 0
        var deferred = 0
        var lastDeferReason: String? = null
        for (issue in pending) {
            val result = github.createIssue(issue.title, issue.body, issue.labels)
            result.fold(
                onSuccess = {
                    pendingStore.markUploaded(issue.id, it.htmlUrl, it.number)
                    debugLogger.i("CrashSync", "GitHub issue #${it.number} ${it.htmlUrl}")
                    fileSink.appendRaw("github_issue_uploaded id=${issue.id} url=${it.htmlUrl}")
                    uploaded++
                },
                onFailure = {
                    pendingStore.markUploadFailed(issue.id, it.message ?: "upload failed")
                    deferred++
                    lastDeferReason = it.message
                },
            )
        }
        if (deferred > 0) {
            debugLogger.w(
                "CrashSync",
                "Issue upload deferred x$deferred: ${lastDeferReason ?: "unknown"} — pending kept for agent triage",
            )
        }
        return uploaded
    }

    fun pendingCount(): Int = pendingStore.listPending().count { !it.uploaded }

    fun describePending(): String =
        pendingStore.listPending().joinToString("\n") { p ->
            "${p.id} uploaded=${p.uploaded} ${p.title} ${p.githubIssueUrl.orEmpty()}"
        }
}
