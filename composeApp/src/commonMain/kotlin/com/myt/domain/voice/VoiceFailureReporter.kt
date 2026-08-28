package com.myt.domain.voice

import com.myt.debug.CrashIssueFormatter
import com.myt.debug.PendingGitHubIssue
import com.myt.debug.PendingIssueStore
import com.myt.domain.usecase.VoiceCommandResult
import com.myt.platform.AppInfoPlatform
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Records failed voice commands for GitHub issue auto-sync (text + outcome; audio path when available).
 */
class VoiceFailureReporter(
    private val pendingStore: PendingIssueStore,
    private val appInfoPlatform: AppInfoPlatform,
    private val clock: Clock = Clock.System,
) {
    fun report(rawText: String, result: VoiceCommandResult, audioPath: String? = null) {
        if (result !is VoiceCommandResult.Failed) return
        val now = clock.now().toEpochMilliseconds()
        val id = "voice-$now-${Random.nextInt(1000, 9999)}"
        val appInfo = runCatching { appInfoPlatform.collect() }.getOrNull()
        val body = buildString {
            appendLine("## Voice command failure")
            appendLine("- Raw STT: `$rawText`")
            appendLine("- Result: ${result.message}")
            audioPath?.let { appendLine("- Audio: `$it`") }
            appendLine()
            appInfo?.let {
                appendLine("App: ${it.appVersion} (${it.buildLabel})")
            }
        }
        pendingStore.enqueue(
            PendingGitHubIssue(
                id = id,
                createdAtMs = now,
                kind = "voice",
                title = CrashIssueFormatter.title("voice", "VoiceNav", result.message.take(80)),
                body = body,
                labels = listOf("bug", "auto-reported", "voice-nav"),
            ),
        )
    }
}
