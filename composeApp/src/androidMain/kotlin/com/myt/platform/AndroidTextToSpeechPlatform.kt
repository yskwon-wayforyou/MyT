package com.myt.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

actual class TextToSpeechPlatform actual constructor(context: Any) {
    private val ctx = context as Context
    private var tts: TextToSpeech? = null

    actual suspend fun speak(text: String, locale: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        tts?.stop()
        tts = TextToSpeech(ctx) { status ->
            if (status != TextToSpeech.SUCCESS) {
                cont.resume(Result.failure(IllegalStateException("TTS init failed")))
                return@TextToSpeech
            }
            tts?.language = Locale.forLanguageTag(locale)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "myt-tts")
            cont.resume(Result.success(Unit))
        }
    }

    actual fun stop() {
        tts?.stop()
    }
}
