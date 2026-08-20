package com.myt.platform

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

actual class AudioAlertPlatform actual constructor(context: Any) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)

    actual fun playBeep(frequencyHz: Int, durationMs: Int, count: Int) {
        repeat(count) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
        }
    }
}
