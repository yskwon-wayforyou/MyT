package com.myt.platform

import android.content.Context

actual class SpeechPlatform actual constructor(context: Any) {
    actual suspend fun recognizeSpeech(locale: String): Result<String> {
        // TODO: Android SpeechRecognizer integration
        return Result.success("강남역")
    }
}
