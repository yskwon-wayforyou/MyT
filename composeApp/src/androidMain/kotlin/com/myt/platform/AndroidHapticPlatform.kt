package com.myt.platform

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

actual class HapticPlatform actual constructor(context: Any) {
    private val vibrator: Vibrator? = run {
        val ctx = (context as Context).applicationContext
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ctx.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    private var hapticsSuppressed: Boolean = false

    actual fun setHapticsSuppressed(suppressed: Boolean) {
        hapticsSuppressed = suppressed
    }

    actual fun areHapticsSuppressed(): Boolean = hapticsSuppressed

    actual fun vibrate(durationMs: Long) {
        if (hapticsSuppressed) return
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
