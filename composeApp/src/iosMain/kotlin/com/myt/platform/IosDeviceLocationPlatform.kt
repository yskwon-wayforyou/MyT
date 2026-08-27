package com.myt.platform

import com.myt.domain.device.DeviceFix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS CoreLocation wiring is deferred; stub keeps BT-gate logic compilable.
 * Speed/location fall back to Fleet until CLLocation actual is added.
 */
actual class DeviceLocationPlatform actual constructor(context: Any) {
    private val _fixes = MutableStateFlow<DeviceFix?>(null)
    actual val fixes: Flow<DeviceFix?> = _fixes.asStateFlow()

    actual fun hasPermission(): Boolean = false

    actual fun startUpdates() {
        _fixes.value = null
    }

    actual fun stopUpdates() {
        _fixes.value = null
    }
}
