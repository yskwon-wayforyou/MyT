package com.myt.platform

/** Phone, SMS, and messenger intents exposed by the device OS. */
expect class DeviceCommunicationsPlatform(context: Any) {
    fun dialPhone(number: String): Result<Unit>
    fun sendSms(number: String, message: String): Result<Unit>
    fun shareKakaoTalk(message: String): Result<Unit>
    fun openMessagingApp(): Result<Unit>
    /** Opens YouTube Music (or browser fallback) search for [query]. */
    fun openYouTubeMusicSearch(query: String): Result<Unit>
}

expect class TextToSpeechPlatform(context: Any) {
    suspend fun speak(text: String, locale: String = "ko-KR"): Result<Unit>
    /**
     * Speaks [text] and waits until utterance finishes (best-effort).
     * Used by TTS→command smoke tests so STT-substitute runs after audio.
     */
    suspend fun speakAndWait(text: String, locale: String = "ko-KR"): Result<Unit>
    fun stop()
}
