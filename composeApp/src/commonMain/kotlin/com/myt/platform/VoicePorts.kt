package com.myt.platform

/** Common ports so voice command logic can be unit-tested without expect/actual. */
interface SpeechRecognizer {
    suspend fun recognizeSpeech(locale: String = "ko-KR"): Result<String>
}

interface TextToSpeech {
    suspend fun speak(text: String, locale: String = "ko-KR"): Result<Unit>
    suspend fun speakAndWait(text: String, locale: String = "ko-KR"): Result<Unit>
    fun stop()
}

interface DeviceCommunications {
    fun dialPhone(number: String): Result<Unit>
    fun sendSms(number: String, message: String): Result<Unit>
    fun shareKakaoTalk(message: String): Result<Unit>
    fun openMessagingApp(): Result<Unit>
    fun openYouTubeMusicSearch(query: String): Result<Unit>
}

class SpeechPlatformRecognizer(
    private val platform: SpeechPlatform,
) : SpeechRecognizer {
    override suspend fun recognizeSpeech(locale: String): Result<String> =
        platform.recognizeSpeech(locale)
}

class PlatformTextToSpeech(
    private val platform: TextToSpeechPlatform,
) : TextToSpeech {
    override suspend fun speak(text: String, locale: String): Result<Unit> =
        platform.speak(text, locale)

    override suspend fun speakAndWait(text: String, locale: String): Result<Unit> =
        platform.speakAndWait(text, locale)

    override fun stop() = platform.stop()
}

class PlatformDeviceCommunications(
    private val platform: DeviceCommunicationsPlatform,
) : DeviceCommunications {
    override fun dialPhone(number: String): Result<Unit> = platform.dialPhone(number)
    override fun sendSms(number: String, message: String): Result<Unit> = platform.sendSms(number, message)
    override fun shareKakaoTalk(message: String): Result<Unit> = platform.shareKakaoTalk(message)
    override fun openMessagingApp(): Result<Unit> = platform.openMessagingApp()
    override fun openYouTubeMusicSearch(query: String): Result<Unit> =
        platform.openYouTubeMusicSearch(query)
}
