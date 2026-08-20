package com.myt.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val _connectionState = MutableStateFlow(BtConnectionState.Disconnected)
    actual val connectionState: Flow<BtConnectionState> = _connectionState.asStateFlow()

    actual fun startMonitoring() {
        _connectionState.value = BtConnectionState.Connected
    }

    actual fun stopMonitoring() {
        _connectionState.value = BtConnectionState.Disconnected
    }
}

actual class SpeechPlatform actual constructor(context: Any) {
    actual suspend fun recognizeSpeech(locale: String): Result<String> =
        Result.success("강남역")
}

actual class AudioAlertPlatform actual constructor(context: Any) {
    actual fun playBeep(frequencyHz: Int, durationMs: Int, count: Int) = Unit
}

actual class HapticPlatform actual constructor(context: Any) {
    actual fun vibrate(durationMs: Long) = Unit
}
