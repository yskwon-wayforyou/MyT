package com.myt.platform

import com.juul.kable.Scanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import platform.UIKit.UIDevice
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual class SecureStoragePlatform actual constructor(context: Any) {
    private val store = mutableMapOf<String, String>()

    actual fun saveToken(key: String, value: String) {
        store[key] = value
    }

    actual fun getToken(key: String): String? = store[key]

    actual fun deleteToken(key: String) {
        store.remove(key)
    }
}

actual class BluetoothPlatform actual constructor(context: Any) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _connectionState = MutableStateFlow(BtConnectionState.Disconnected)
    private var scanJob: Job? = null
    private var lastBleSeenMs: Long = 0L

    actual val connectionState: Flow<BtConnectionState> = _connectionState.asStateFlow()

    actual fun startMonitoring() {
        scanJob?.cancel()
        scanJob = scope.launch {
            _connectionState.value = BtConnectionState.Connecting
            while (true) {
                runCatching {
                    Scanner().advertisements
                        .catch { /* keep scanning */ }
                        .collect { advertisement ->
                            val name = advertisement.name.orEmpty()
                            val isTesla = name.contains("Tesla", ignoreCase = true) ||
                                name.contains("Sentry", ignoreCase = true)
                            if (isTesla) {
                                lastBleSeenMs = Clock.System.now().toEpochMilliseconds()
                                _connectionState.value = BtConnectionState.Connected
                            }
                        }
                }
                delay(2_000)
                val now = Clock.System.now().toEpochMilliseconds()
                if (now - lastBleSeenMs > 20_000) {
                    _connectionState.value = BtConnectionState.Disconnected
                }
            }
        }
    }

    actual fun stopMonitoring() {
        scanJob?.cancel()
        scanJob = null
        _connectionState.value = BtConnectionState.Disconnected
    }
}

/**
 * iOS STT via Speech framework requires Xcode Speech entitlements + runtime permission.
 * Uses SFSpeechRecognizer when available; otherwise returns a clear failure
 * (no hardcoded destination stub).
 */
actual class SpeechPlatform actual constructor(context: Any) {
    actual suspend fun recognizeSpeech(locale: String): Result<String> {
        return runCatching {
            IosSpeechBridge.recognize(locale)
        }.fold(
            onSuccess = { text ->
                if (text.isBlank()) Result.failure(IllegalStateException("인식된 음성이 없습니다"))
                else Result.success(text)
            },
            onFailure = { Result.failure(it) },
        )
    }
}

actual class AudioAlertPlatform actual constructor(context: Any) {
    private var alertsSuppressed: Boolean = false

    actual fun isAudible(): Boolean = !alertsSuppressed

    actual fun setAlertsSuppressed(suppressed: Boolean) {
        alertsSuppressed = suppressed
    }

    actual fun areAlertsSuppressed(): Boolean = alertsSuppressed

    actual fun playBeep(frequencyHz: Int, durationMs: Int, count: Int) {
        if (!isAudible()) return
        repeat(count.coerceAtLeast(1)) {
            UIDevice.currentDevice.playInputClick()
        }
    }
}

actual class HapticPlatform actual constructor(context: Any) {
    private var hapticsSuppressed: Boolean = false

    actual fun setHapticsSuppressed(suppressed: Boolean) {
        hapticsSuppressed = suppressed
    }

    actual fun areHapticsSuppressed(): Boolean = hapticsSuppressed

    actual fun vibrate(durationMs: Long) {
        if (hapticsSuppressed) return
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium).impactOccurred()
    }
}
