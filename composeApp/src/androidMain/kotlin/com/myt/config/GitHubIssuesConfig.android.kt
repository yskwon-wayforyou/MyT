package com.myt.config

import com.myt.MyTApplication
import java.util.Properties

actual fun loadGitHubIssuesConfig(): GitHubIssuesConfig {
    val props = Properties()
    runCatching {
        if (MyTApplication.isInitialized) {
            val writable = java.io.File(MyTApplication.instance.filesDir, "tesla.local.properties")
            if (writable.exists()) {
                writable.inputStream().use { props.load(it) }
            } else {
                MyTApplication.instance.assets.open("tesla.local.properties").use { props.load(it) }
            }
        }
    }
    fun prop(key: String, fallback: String = ""): String =
        props.getProperty(key)?.trim().orEmpty().ifBlank { fallback }

    val enabled = prop("github.issues.enabled", "true").equals("true", ignoreCase = true)
    return GitHubIssuesConfig(
        enabled = enabled,
        repo = prop("github.issues.repo", "yskwon-wayforyou/MyT"),
        token = prop("github.issues.token"),
    )
}
