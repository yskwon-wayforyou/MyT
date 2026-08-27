package com.myt.debug

enum class LogLevel(val label: String) {
    Debug("DEBUG"),
    Info("INFO"),
    Warn("WARN"),
    Error("ERROR"),
}

data class LogEntry(
    val id: Long,
    val epochMs: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableType: String? = null,
    val throwableMessage: String? = null,
) {
    fun formattedLine(): String {
        val ts = formatEpoch(epochMs)
        val base = "$ts [${level.label}] $tag: $message"
        return if (throwableType != null) {
            "$base | ${throwableType}${throwableMessage?.let { ": $it" }.orEmpty()}"
        } else {
            base
        }
    }
}

private fun formatEpoch(epochMs: Long): String {
    return kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toString()
        .replace('T', ' ')
        .take(23)
}
