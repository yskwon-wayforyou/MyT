package com.myt.debug

object LogRedactor {
    private val bearerToken = Regex("""(?i)(Bearer\s+)[A-Za-z0-9._\-]+""")
    private val jwt = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
    private val secretKv = Regex(
        """(?i)(client[_-]?secret|access[_-]?token|refresh[_-]?token|password|authorization|github[._-]?issues[._-]?token|ghp_[A-Za-z0-9]+)\s*[:=]?\s*["']?[^"'\s,}\]]+""",
    )
    private val githubPat = Regex("""\bgh[pousr]_[A-Za-z0-9_]{20,}\b""")
    private val vin = Regex("""\b[A-HJ-NPR-Z0-9]{17}\b""")

    fun redact(input: String): String {
        var text = input
        text = bearerToken.replace(text) { "${it.groupValues[1]}[REDACTED]" }
        text = jwt.replace(text, "[JWT_REDACTED]")
        text = secretKv.replace(text) { match ->
            val key = match.value.substringBefore('=').substringBefore(':').trim()
            "$key=[REDACTED]"
        }
        text = githubPat.replace(text, "[GITHUB_TOKEN_REDACTED]")
        text = vin.replace(text) { match ->
            val v = match.value
            "VIN…${v.takeLast(4)}"
        }
        return text
    }
}
