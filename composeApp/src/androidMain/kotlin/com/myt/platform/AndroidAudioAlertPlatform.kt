package com.myt.platform

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

actual class AudioAlertPlatform actual constructor(context: Any) {
    private val appContext = (context as Context).applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var toneGenerator: ToneGenerator? = null
    private var alertsSuppressed: Boolean = false

    actual fun isAudible(): Boolean {
        if (alertsSuppressed) return false
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> return false
            AudioManager.RINGER_MODE_VIBRATE -> return false
        }
        val alarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val music = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val notification = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        return alarm > 0 || music > 0 || notification > 0
    }

    actual fun setAlertsSuppressed(suppressed: Boolean) {
        alertsSuppressed = suppressed
        if (suppressed) {
            runCatching { toneGenerator?.release() }
            toneGenerator = null
        }
    }

    actual fun areAlertsSuppressed(): Boolean = alertsSuppressed

    actual fun playBeep(frequencyHz: Int, durationMs: Int, count: Int) {
        if (!isAudible()) return
        val gen = toneGenerator ?: ToneGenerator(
            AudioManager.STREAM_ALARM,
            streamVolumePercent(AudioManager.STREAM_ALARM),
        ).also { toneGenerator = it }
        repeat(count) {
            gen.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
        }
    }

    private fun streamVolumePercent(stream: Int): Int {
        val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(stream)
        return ((current.toFloat() / max) * 100f).toInt().coerceIn(0, 100)
    }
}
