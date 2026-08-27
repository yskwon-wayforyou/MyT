package com.myt.debug

import com.myt.config.GitHubIssuesConfig
import com.myt.config.loadGitHubIssuesConfig
import com.myt.platform.AppInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class GitHubIssueClient(
    private val httpClient: HttpClient,
    private val configProvider: () -> GitHubIssuesConfig = { loadGitHubIssuesConfig() },
) {
    suspend fun createIssue(
        title: String,
        body: String,
        labels: List<String>,
    ): Result<CreatedIssue> = runCatching {
        val config = configProvider()
        require(config.canPostFromApp()) {
            "GitHub Issues token/repo not configured — pending file kept for agent triage"
        }
        val repo = config.repo.trim().removePrefix("https://github.com/").removeSuffix(".git")
        val response: GhIssueResponse = httpClient.post("https://api.github.com/repos/$repo/issues") {
            header("Authorization", "Bearer ${config.token}")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            contentType(ContentType.Application.Json)
            setBody(
                GhCreateIssueRequest(
                    title = title.take(240),
                    body = body,
                    labels = labels,
                ),
            )
        }.body()
        CreatedIssue(
            number = response.number,
            htmlUrl = response.htmlUrl,
        )
    }

    data class CreatedIssue(val number: Int, val htmlUrl: String)

    @Serializable
    private data class GhCreateIssueRequest(
        val title: String,
        val body: String,
        val labels: List<String> = emptyList(),
    )

    @Serializable
    private data class GhIssueResponse(
        val number: Int,
        @SerialName("html_url") val htmlUrl: String,
    )
}

object CrashIssueFormatter {
    fun title(kind: String, throwableType: String?, message: String?): String {
        val type = throwableType?.takeIf { it.isNotBlank() } ?: "Unknown"
        val msg = message?.lineSequence()?.firstOrNull()?.take(80).orEmpty()
        val suffix = if (msg.isBlank()) type else "$type: $msg"
        return "[MyT][$kind] $suffix"
    }

    fun body(
        kind: String,
        appInfo: AppInfo?,
        crashReport: String,
        logTail: String,
        pendingId: String,
    ): String = buildString {
        appendLine("## Auto-reported $kind")
        appendLine()
        appendLine("- pending_id: `$pendingId`")
        if (appInfo != null) {
            appendLine("- app: ${appInfo.appVersion} (${appInfo.buildLabel})")
            appendLine("- device: ${appInfo.deviceDescription}")
            appendLine("- os: ${appInfo.osDescription}")
            appendLine("- platform: ${appInfo.platformLabel}")
        }
        appendLine()
        appendLine("### Crash / exception")
        appendLine("```")
        appendLine(LogRedactor.redact(crashReport).take(12_000))
        appendLine("```")
        appendLine()
        appendLine("### Runtime log tail (`myt-runtime.log`)")
        appendLine("```")
        appendLine(LogRedactor.redact(logTail).take(12_000))
        appendLine("```")
        appendLine()
        appendLine("_Filed by MyT CrashIssueSync. Secrets are redacted._")
    }
}
