package com.myt.platform

import kotlinx.coroutines.flow.Flow

enum class BtConnectionState {
    Disconnected,
    Connecting,
    Connected,
}

expect class SecureStoragePlatform(context: Any) {
    fun saveToken(key: String, value: String)
    fun getToken(key: String): String?
    fun deleteToken(key: String)
}

expect class BluetoothPlatform(context: Any) {
    val connectionState: Flow<BtConnectionState>
    fun startMonitoring()
    fun stopMonitoring()
}

expect class SpeechPlatform(context: Any) {
    suspend fun recognizeSpeech(locale: String): Result<String>
}

expect class AudioAlertPlatform(context: Any) {
    /** True when device volume allows beeps (library / silent mode). */
    fun isAudible(): Boolean
    /** Simulation·테스트 중 오디오 경보 금지 */
    fun setAlertsSuppressed(suppressed: Boolean)
    fun areAlertsSuppressed(): Boolean
    fun playBeep(frequencyHz: Int, durationMs: Int, count: Int)
}

expect class HapticPlatform(context: Any) {
    fun setHapticsSuppressed(suppressed: Boolean)
    fun areHapticsSuppressed(): Boolean
    fun vibrate(durationMs: Long)
}

expect class ScreenPlatform(context: Any) {
    fun keepScreenOn(enable: Boolean)
    fun currentWidthDp(): Int
    fun currentHeightDp(): Int
}

expect class DeviceLocationPlatform(context: Any) {
    val fixes: kotlinx.coroutines.flow.Flow<com.myt.domain.device.DeviceFix?>
    fun hasPermission(): Boolean
    fun startUpdates()
    fun stopUpdates()
}
