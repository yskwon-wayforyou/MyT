package com.myt.platform

/** Phone, SMS, and messenger intents exposed by the device OS. */
expect class DeviceCommunicationsPlatform(context: Any) {
    fun dialPhone(number: String): Result<Unit>
    fun sendSms(number: String, message: String): Result<Unit>
    fun shareKakaoTalk(message: String): Result<Unit>
    fun openMessagingApp(): Result<Unit>
}

expect class TextToSpeechPlatform(context: Any) {
    suspend fun speak(text: String, locale: String = "ko-KR"): Result<Unit>
    fun stop()
}
