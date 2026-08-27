package com.myt.config

/**
 * Optional in-app GitHub Issues reporter.
 * Token is never logged; leave blank to rely on agent `gh` triage after adb pull.
 */
data class GitHubIssuesConfig(
    val enabled: Boolean = true,
    val repo: String = "yskwon-wayforyou/MyT",
    val token: String = "",
) {
    fun canPostFromApp(): Boolean = enabled && repo.isNotBlank() && token.isNotBlank()
}

expect fun loadGitHubIssuesConfig(): GitHubIssuesConfig
