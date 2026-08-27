package com.myt.domain.usecase

import com.myt.debug.DebugLogger
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.platform.DeviceCommunicationsPlatform
import com.myt.platform.SpeechPlatform
import com.myt.platform.TextToSpeechPlatform

sealed interface VoiceCommandResult {
    data class Navigate(val route: String) : VoiceCommandResult
    data class Speak(val message: String) : VoiceCommandResult
    data object Sent : VoiceCommandResult
    data class Failed(val message: String) : VoiceCommandResult
}

class VoiceCommandUseCase(
    private val speechPlatform: SpeechPlatform,
    private val communications: DeviceCommunicationsPlatform,
    private val tts: TextToSpeechPlatform,
    private val voiceNavUseCase: VoiceNavUseCase,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val debugLogger: DebugLogger,
) {
    suspend fun listenAndExecute(locale: String = "ko-KR"): VoiceCommandResult {
        val raw = speechPlatform.recognizeSpeech(locale).getOrElse {
            debugLogger.w("Voice", "STT failed: ${it.message}")
            return VoiceCommandResult.Failed(
                com.myt.domain.voice.SpeechErrorMessages.humanize(it.message),
            )
        }
        debugLogger.i("Voice", "Recognized: $raw")
        return execute(raw)
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

            lower.startsWith("전화") || lower.contains("전화 걸") || lower.contains("call ") -> {
                val number = extractPhone(text) ?: return VoiceCommandResult.Failed("전화번호를 말씀해 주세요")
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

            lower.contains("내비") || lower.contains("길 안내") || lower.contains("목적지") -> {
                when (val nav = voiceNavUseCase.sendDestination(extractDestination(text))) {
                    is VoiceNavResult.Sent -> VoiceCommandResult.Sent
                    is VoiceNavResult.Failed -> VoiceCommandResult.Failed(nav.message)
                    is VoiceNavResult.Recognized -> VoiceCommandResult.Sent
                }
            }

            else -> {
                when (val nav = voiceNavUseCase.sendDestination(text)) {
                    is VoiceNavResult.Sent -> VoiceCommandResult.Sent
                    is VoiceNavResult.Failed -> VoiceCommandResult.Failed("명령을 이해하지 못했습니다. 내비·전화·문자·카카오·히스토리를 말씀해 주세요.")
                    is VoiceNavResult.Recognized -> VoiceCommandResult.Sent
                }
            }
        }
        debugLogger.i("Voice", "Command result=${result::class.simpleName}")
        return result
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
        val keywords = listOf("내비", "길 안내", "목적지", "로", "까지", "에")
        var cleaned = text
        keywords.forEach { key ->
            cleaned = cleaned.replace(key, " ", ignoreCase = true)
        }
        return cleaned.trim().ifBlank { text }
    }

    private fun extractPhone(text: String): String? {
        val digits = Regex("""[\d+\-() ]+""").findAll(text).map { it.value }.maxByOrNull { it.count { c -> c.isDigit() } }
        return digits?.filter { it.isDigit() || it == '+' }?.takeIf { it.length >= 8 }
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String? {
        keywords.forEach { key ->
            val idx = text.indexOf(key, ignoreCase = true)
            if (idx >= 0) {
                return text.substring(idx + key.length).trim(' ', ':', '에', '을', '를', '줘', '.').takeIf { it.isNotBlank() }
            }
        }
        return null
    }
}
