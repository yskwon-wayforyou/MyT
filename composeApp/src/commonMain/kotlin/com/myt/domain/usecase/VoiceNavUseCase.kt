package com.myt.domain.usecase

import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.platform.SpeechPlatform

sealed interface VoiceNavResult {
    data class Recognized(val destination: String) : VoiceNavResult
    data object Sent : VoiceNavResult
    data class Failed(val message: String) : VoiceNavResult
}

class VoiceNavUseCase(
    private val fleetRepository: FleetRepository,
    private val settingsRepository: SettingsRepository,
    private val speechPlatform: SpeechPlatform,
) {
    suspend fun recognizeDestination(locale: String = "ko-KR"): VoiceNavResult {
        return speechPlatform.recognizeSpeech(locale).fold(
            onSuccess = { VoiceNavResult.Recognized(it) },
            onFailure = { VoiceNavResult.Failed(it.message ?: "Speech recognition failed") },
        )
    }

    suspend fun sendDestination(destination: String): VoiceNavResult {
        val vin = settingsRepository.getVin()
            ?: return VoiceNavResult.Failed("VIN not configured")
        return fleetRepository.sendNavigationRequest(vin, destination).fold(
            onSuccess = { VoiceNavResult.Sent },
            onFailure = { VoiceNavResult.Failed(it.message ?: "Navigation request failed") },
        )
    }
}
