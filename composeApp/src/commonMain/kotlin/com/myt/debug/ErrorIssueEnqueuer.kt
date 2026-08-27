package com.myt.debug

import com.myt.platform.AppInfoPlatform
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Enqueues non-fatal ERROR(+throwable) as pending GitHub issues (uploaded on next sync).
 */
class ErrorIssueEnqueuer(
    private val pendingStore: PendingIssueStore,
    private val fileSink: PersistentLogSink,
    private val appInfoPlatform: AppInfoPlatform,
    private val clock: Clock = Clock.System,
) {
    @Volatile
    private var lastFingerprint: String? = null
    @Volatile
    private var lastAtMs: Long = 0L

    fun enqueue(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) return
        // Expected / soft fleet failures — keep in runtime log only
        val softName = throwable::class.simpleName.orEmpty()
        if (softName.contains("VehicleDataUnavailable") || softName.contains("QuotaExceeded")) return
        val msg = message.lowercase()
        if ("empty vehicle_data" in msg || "quota" in msg) return
        if ("음성 인식" in message || "speech" in msg || "stt" in msg) return
        // Android SpeechRecognizer error codes often surface as "오류 (6|7|8|9)"
        if (Regex("""오류\s*\([6-9]\)""").containsMatchIn(message)) return
        val fingerprint = "${throwable::class.simpleName}:${message.take(120)}"
        val now = clock.now().toEpochMilliseconds()
        // Deduplicate identical errors within 60s
        if (fingerprint == lastFingerprint && now - lastAtMs < 60_000) return
        lastFingerprint = fingerprint
        lastAtMs = now

        val id = "error-$now-${Random.nextInt(1000, 9999)}"
        val appInfo = runCatching { appInfoPlatform.collect() }.getOrNull()
        val crashBlock = buildString {
            appendLine("Tag: $tag")
            appendLine("Message: $message")
            appendLine("Exception: ${throwable::class.qualifiedName}")
            appendLine(throwable.stackTraceToString())
        }
        pendingStore.enqueue(
            PendingGitHubIssue(
                id = id,
                createdAtMs = now,
                kind = "error",
                title = CrashIssueFormatter.title("error", throwable::class.simpleName, message),
                body = CrashIssueFormatter.body("error", appInfo, crashBlock, fileSink.readTail(), id),
                labels = listOf("bug", "auto-reported", "runtime-error"),
            ),
        )
        fileSink.appendRaw("pending_error_issue id=$id tag=$tag")
    }
}
