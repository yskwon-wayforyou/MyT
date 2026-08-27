package com.myt.platform

actual class DeviceCommunicationsPlatform actual constructor(context: Any) {
    actual fun dialPhone(number: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("iOS dial stub"))

    actual fun sendSms(number: String, message: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("iOS SMS stub"))

    actual fun shareKakaoTalk(message: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("iOS Kakao stub"))

    actual fun openMessagingApp(): Result<Unit> =
        Result.failure(UnsupportedOperationException("iOS messaging stub"))
}

actual class TextToSpeechPlatform actual constructor(context: Any) {
    actual suspend fun speak(text: String, locale: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("iOS TTS stub"))

    actual fun stop() = Unit
}
