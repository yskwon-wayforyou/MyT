package com.myt.domain.usecase

import com.myt.debug.DebugLogger
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.domain.voice.VoiceCommandExample
import com.myt.domain.voice.VoiceCommandExamples
import com.myt.domain.voice.VoiceFailureReporter
import com.myt.platform.DeviceCommunications
import com.myt.platform.SpeechRecognizer
import com.myt.platform.TextToSpeech

sealed interface VoiceCommandResult {
    data class Navigate(val route: String) : VoiceCommandResult
    data class Speak(val message: String) : VoiceCommandResult
    data object Sent : VoiceCommandResult
    data class Failed(val message: String) : VoiceCommandResult
}

class VoiceCommandUseCase(
    private val speech: SpeechRecognizer,
    private val communications: DeviceCommunications,
    private val tts: TextToSpeech,
    private val voiceNavUseCase: VoiceNavUseCase,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val debugLogger: DebugLogger,
    private val voiceFailureReporter: VoiceFailureReporter? = null,
) {
    suspend fun listenAndExecute(locale: String = "ko-KR"): VoiceCommandResult {
        val raw = speech.recognizeSpeech(locale).getOrElse {
            debugLogger.w("Voice", "STT failed: ${it.message}")
            return VoiceCommandResult.Failed(
                com.myt.domain.voice.SpeechErrorMessages.humanize(it.message),
            )
        }
        debugLogger.i("Voice", "Recognized: $raw")
        return execute(raw)
    }

    /**
     * TTS plays [example] then runs [execute] with the same text (STT substitute).
     * Validates end-to-end command wiring without relying on the microphone.
     */
    suspend fun playExampleAndExecute(
        example: VoiceCommandExample,
        locale: String = "ko-KR",
        speakFirst: Boolean = true,
    ): VoiceCommandResult {
        if (speakFirst) {
            tts.speakAndWait(example.spokenText, locale).onFailure {
                debugLogger.w("Voice", "TTS example failed: ${it.message}")
                return VoiceCommandResult.Failed("TTS 재생 실패: ${it.message}")
            }
        }
        debugLogger.i("Voice", "Example inject id=${example.id} text=${example.spokenText}")
        return execute(example.spokenText)
    }

    suspend fun runAllExamplesAsTtsInject(
        speakFirst: Boolean = false,
    ): List<Pair<VoiceCommandExample, VoiceCommandResult>> =
        VoiceCommandExamples.all.map { example ->
            example to playExampleAndExecute(example, speakFirst = speakFirst)
        }

    suspend fun execute(raw: String): VoiceCommandResult {
        val text = raw.trim()
        val lower = text.lowercase()
        val result = when {
            lower.contains("히스토리") || lower.contains("기록") || lower.contains("내역") ->
                VoiceCommandResult.Navigate("history")

            lower.contains("설정") ->
                VoiceCommandResult.Navigate("settings")

            lower.contains("계기") || lower.contains("게이지") || lower.contains("홈") ->
                VoiceCommandResult.Navigate("gauge")

            isYouTubeMusicCommand(lower) -> {
                val query = extractMusicQuery(text)
                if (query.isBlank()) {
                    return VoiceCommandResult.Failed("재생할 가수·앨범·곡을 말씀해 주세요")
                }
                communications.openYouTubeMusicSearch(query).fold(
                    onSuccess = { VoiceCommandResult.Sent },
                    onFailure = { VoiceCommandResult.Failed(it.message ?: "유튜브 뮤직 열기 실패") },
                )
            }

            lower.contains("전화") || lower.contains("call ") -> {
                val number = extractPhone(text) ?: return VoiceCommandResult.Failed(
                    "전화번호를 말씀해 주세요. 예) 「전화 01012345678」",
                )
                communications.dialPhone(number).fold(
                    onSuccess = { VoiceCommandResult.Sent },
                    onFailure = { VoiceCommandResult.Failed(it.message ?: "전화 연결 실패") },
                )
            }

            lower.contains("문자") || lower.contains("sms") -> {
                val number = extractPhone(text)
                val body = extractAfterKeyword(text, listOf("보내", "전송", "문자")) ?: text
                if (number.isNullOrBlank()) {
                    communications.openMessagingApp().fold(
                        onSuccess = { VoiceCommandResult.Sent },
                        onFailure = { VoiceCommandResult.Failed(it.message ?: "문자 앱 열기 실패") },
                    )
                } else {
                    communications.sendSms(number, body).fold(
                        onSuccess = { VoiceCommandResult.Sent },
                        onFailure = { VoiceCommandResult.Failed(it.message ?: "문자 보내기 실패") },
                    )
                }
            }

            lower.contains("카카오") || lower.contains("kakao") -> {
                val body = extractAfterKeyword(text, listOf("보내", "전송", "카카오")) ?: text
                communications.shareKakaoTalk(body).fold(
                    onSuccess = { VoiceCommandResult.Sent },
                    onFailure = { VoiceCommandResult.Failed(it.message ?: "카카오톡 공유 실패") },
                )
            }

            lower.contains("읽어") || lower.contains("읽어줘") -> {
                val message = lastReadableMessage()
                tts.speak(message).fold(
                    onSuccess = { VoiceCommandResult.Speak(message) },
                    onFailure = { VoiceCommandResult.Failed(it.message ?: "읽기 실패") },
                )
            }

            lower.contains("내비") || lower.contains("길 안내") || lower.contains("목적지") ||
                lower.contains("안내") -> {
                when (val nav = voiceNavUseCase.sendDestination(extractDestination(text))) {
                    is VoiceNavResult.Sent -> VoiceCommandResult.Sent
                    is VoiceNavResult.Failed -> VoiceCommandResult.Failed(nav.message)
                    is VoiceNavResult.Recognized -> VoiceCommandResult.Sent
                }
            }

            else -> {
                when (val nav = voiceNavUseCase.sendDestination(text)) {
                    is VoiceNavResult.Sent -> VoiceCommandResult.Sent
                    is VoiceNavResult.Failed -> VoiceCommandResult.Failed(
                        "명령을 이해하지 못했습니다. 내비·전화·문자·카카오·유튜브 뮤직·히스토리를 말씀해 주세요.",
                    )
                    is VoiceNavResult.Recognized -> VoiceCommandResult.Sent
                }
            }
        }
        debugLogger.i("Voice", "Command result=${result::class.simpleName}")
        if (result is VoiceCommandResult.Failed) {
            voiceFailureReporter?.report(text, result)
        }
        return result
    }

    private fun isYouTubeMusicCommand(lower: String): Boolean {
        val musicHint = listOf(
            "유튜브 뮤직",
            "유튜브뮤직",
            "youtube music",
            "youtubemusic",
            "yt music",
            "음악 틀",
            "노래 틀",
            "음악 플레이",
            "노래 플레이",
            "음악 재생",
            "노래 재생",
        )
        return musicHint.any { it in lower } ||
            ((lower.contains("유튜브") || lower.contains("youtube")) &&
                (lower.contains("음악") || lower.contains("뮤직") || lower.contains("노래") || lower.contains("플레이")))
    }

    private fun extractMusicQuery(text: String): String {
        var cleaned = text
        listOf(
            "유튜브 뮤직에서",
            "유튜브뮤직에서",
            "유튜브 뮤직",
            "유튜브뮤직",
            "youtube music에서",
            "youtube music",
            "youtubemusic",
            "음악을 무작위로 플레이해줘",
            "음악을 무작위로 플레이",
            "음악 무작위로 플레이해줘",
            "무작위로 플레이해줘",
            "무작위로 플레이",
            "플레이해줘",
            "틀어줘",
            "재생해줘",
            "재생",
            "플레이",
            "음악",
            "노래",
        ).forEach { token ->
            cleaned = cleaned.replace(token, " ", ignoreCase = true)
        }
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    private suspend fun lastReadableMessage(): String {
        val events = historyRepository.fleetEvents(
            com.myt.domain.history.HistoryFilterState(
                tab = com.myt.domain.history.HistoryTab.FleetApi,
                period = com.myt.domain.history.HistoryPeriodFilter.Days7,
            ),
        )
        val latest = events.firstOrNull()
        return latest?.detail ?: "최근 알릴 메시지가 없습니다."
    }

    private fun extractDestination(text: String): String {
        val keywords = listOf(
            "내비게이션", "내비", "길 안내", "길안내", "목적지", "안내해줘", "안내해 줘", "안내",
            "으로 가", "로 가", "까지 가", "에 가",
        )
        var cleaned = text
        keywords.forEach { key ->
            cleaned = cleaned.replace(key, " ", ignoreCase = true)
        }
        return cleaned.replace(Regex("\\s+"), " ").trim().ifBlank { text.trim() }
    }

    private fun extractPhone(text: String): String? {
        val digits = Regex("""[\d+\-() ]+""").findAll(text)
            .map { it.value }
            .maxByOrNull { it.count { c -> c.isDigit() } }
        return digits?.filter { it.isDigit() || it == '+' }?.takeIf { it.length >= 8 }
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String? {
        keywords.forEach { key ->
            val idx = text.indexOf(key, ignoreCase = true)
            if (idx >= 0) {
                return text.substring(idx + key.length)
                    .trim(' ', ':', '에', '을', '를', '줘', '.')
                    .takeIf { it.isNotBlank() }
            }
        }
        return null
    }
}
