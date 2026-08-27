package com.myt.domain.voice

/** Maps platform STT errors to actionable Korean copy (W2). */
object SpeechErrorMessages {
    fun humanize(raw: String?): String {
        val msg = raw.orEmpty()
        val lower = msg.lowercase()
        return when {
            Regex("""\b7\b""").containsMatchIn(msg) || "no.?match" in lower || "인식" in msg && "오류" in msg ->
                "음성을 인식하지 못했습니다. 조용한 곳에서 「강남역」처럼 짧게 다시 말씀해 주세요."
            Regex("""\b6\b""").containsMatchIn(msg) || "timeout" in lower ->
                "응답 시간이 초과되었습니다. 다시 시도해 주세요."
            Regex("""\b9\b""").containsMatchIn(msg) || "permission" in lower || "권한" in msg ->
                "마이크 권한이 필요합니다. 설정에서 마이크를 허용해 주세요."
            Regex("""\b5\b""").containsMatchIn(msg) || "client" in lower ->
                "음성 인식 클라이언트가 바쁩니다. 잠시 후 다시 시도해 주세요."
            msg.isBlank() -> "음성 인식에 실패했습니다. 다시 시도해 주세요."
            else -> msg
        }
    }
}
