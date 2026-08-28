package com.myt.domain.usecase

import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.platform.SpeechRecognizer

sealed interface VoiceNavResult {
    data class Recognized(val destination: String) : VoiceNavResult
    data object Sent : VoiceNavResult
    data class Failed(val message: String) : VoiceNavResult
}

class VoiceNavUseCase(
    private val fleetRepository: FleetRepository,
    private val settingsRepository: SettingsRepository,
    private val speech: SpeechRecognizer,
) {
    suspend fun recognizeDestination(locale: String = "ko-KR"): VoiceNavResult {
        return speech.recognizeSpeech(locale).fold(
            onSuccess = { VoiceNavResult.Recognized(it) },
            onFailure = {
                VoiceNavResult.Failed(com.myt.domain.voice.SpeechErrorMessages.humanize(it.message))
            },
        )
    }

    suspend fun recognizeAndSend(locale: String = "ko-KR"): VoiceNavResult {
        return when (val recognized = recognizeDestination(locale)) {
            is VoiceNavResult.Recognized -> sendDestination(recognized.destination)
            else -> recognized
        }
    }

    suspend fun sendDestination(destination: String): VoiceNavResult {
        val vin = settingsRepository.getVin()
            ?: return VoiceNavResult.Failed("VIN이 설정되지 않았습니다. 설정에서 차량을 선택해 주세요.")
        val cleaned = destination.trim()
        if (cleaned.length < 2) {
            return VoiceNavResult.Failed("목적지를 다시 말씀해 주세요. 예) 「수원역으로 안내해줘」")
        }
        return fleetRepository.sendNavigationRequest(vin, cleaned).fold(
            onSuccess = { VoiceNavResult.Sent },
            onFailure = { VoiceNavResult.Failed(it.message ?: "내비게이션 요청에 실패했습니다") },
        )
    }
}
