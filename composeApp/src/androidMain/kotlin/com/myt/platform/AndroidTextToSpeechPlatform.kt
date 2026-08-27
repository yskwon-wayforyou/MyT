package com.myt.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

actual class TextToSpeechPlatform actual constructor(context: Any) {
    private val ctx = context as Context
    private var tts: TextToSpeech? = null

    actual suspend fun speak(text: String, locale: String): Result<Unit> =
        speakInternal(text, locale, waitUntilDone = false)

    actual suspend fun speakAndWait(text: String, locale: String): Result<Unit> =
        speakInternal(text, locale, waitUntilDone = true)

    private suspend fun speakInternal(
        text: String,
        locale: String,
        waitUntilDone: Boolean,
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val finished = AtomicBoolean(false)
        fun complete(result: Result<Unit>) {
            if (finished.compareAndSet(false, true)) {
                cont.resume(result)
            }
        }
        tts?.stop()
        tts = TextToSpeech(ctx) { status ->
            if (status != TextToSpeech.SUCCESS) {
                complete(Result.failure(IllegalStateException("TTS init failed")))
                return@TextToSpeech
            }
            val engine = tts ?: run {
                complete(Result.failure(IllegalStateException("TTS null")))
                return@TextToSpeech
            }
            engine.language = Locale.forLanguageTag(locale)
            if (waitUntilDone) {
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        complete(Result.success(Unit))
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        complete(Result.failure(IllegalStateException("TTS utterance error")))
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        complete(Result.failure(IllegalStateException("TTS error $errorCode")))
                    }
                })
            }
            val code = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "myt-tts")
            if (code != TextToSpeech.SUCCESS) {
                complete(Result.failure(IllegalStateException("TTS speak failed: $code")))
                return@TextToSpeech
            }
            if (!waitUntilDone) {
                complete(Result.success(Unit))
            }
        }
        cont.invokeOnCancellation {
            tts?.stop()
            finished.set(true)
        }
    }

    actual fun stop() {
        tts?.stop()
    }
}
