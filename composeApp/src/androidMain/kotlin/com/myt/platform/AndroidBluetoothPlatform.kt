package com.myt.platform

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class BluetoothPlatform actual constructor(context: Any) {
    private val _connectionState = MutableStateFlow(BtConnectionState.Disconnected)

    actual val connectionState: Flow<BtConnectionState> = _connectionState.asStateFlow()

    actual fun startMonitoring() {
        // TODO: Integrate Kable BLE monitoring for Tesla Phone Key
        _connectionState.value = BtConnectionState.Connected
    }

    actual fun stopMonitoring() {
        _connectionState.value = BtConnectionState.Disconnected
    }
}
