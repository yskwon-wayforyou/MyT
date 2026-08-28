package com.myt.platform

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.myt.domain.bluetooth.TeslaBlePresence

/**
 * Shared BT ACL/state + bonded/GATT Tesla presence for [BluetoothPlatform].
 * Does NOT treat "adapter enabled" alone as vehicle present.
 */
object BtConnectionHub {
    private val listeners = mutableSetOf<(Boolean) -> Unit>()
    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var lastPresent: Boolean = false

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    fun register(listener: (Boolean) -> Unit) {
        listeners += listener
        emitCurrent()
    }

    fun unregister(listener: (Boolean) -> Unit) {
        listeners -= listener
    }

    fun onAclEvent(connected: Boolean, deviceName: String? = null) {
        if (connected) {
            if (deviceName == null || TeslaBlePresence.matchesAdvertisementName(deviceName)) {
                notifyPresent(true)
            }
        } else {
            emitCurrent()
        }
    }

    fun onAclEvent(connected: Boolean) = onAclEvent(connected, null)

    fun emitCurrentForService() = emitCurrent()

    fun emitCurrent() {
        val present = detectTeslaPresent()
        notifyPresent(present)
    }

    private fun notifyPresent(present: Boolean) {
        val wasPresent = lastPresent
        lastPresent = present
        if (present && !wasPresent) {
            appContext?.let { ctx ->
                com.myt.service.VehiclePresenceLauncher.onVehiclePresent(ctx)
            }
        }
        listeners.forEach { it(present) }
    }

    @Suppress("MissingPermission")
    fun detectTeslaPresent(): Boolean {
        val ctx = appContext ?: return false
        val manager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return false
        val adapter = manager.adapter ?: return false
        if (!adapter.isEnabled) return false

        // Connected GATT peripherals (Phone Key often stays connected without advertising).
        runCatching {
            val gattDevices = manager.getConnectedDevices(BluetoothProfile.GATT)
            if (gattDevices.any { isLikelyTesla(it) }) return true
        }

        // Bonded devices named like Tesla / Phone Key pattern.
        runCatching {
            @Suppress("DEPRECATION")
            val bonded = adapter.bondedDevices.orEmpty()
            if (bonded.any { isLikelyTesla(it) }) {
                // Bonded alone is weak; require also connected GATT or recent ACL.
                // Still useful when getConnectedDevices is empty on some OEMs — check connection state.
                bonded.forEach { device ->
                    if (isLikelyTesla(device) && isDeviceConnected(device)) return true
                }
            }
        }
        return false
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return runCatching {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as? Boolean == true
        }.getOrDefault(false)
    }

    @Suppress("MissingPermission")
    private fun isLikelyTesla(device: BluetoothDevice): Boolean {
        val name = runCatching { device.name }.getOrNull()
        return TeslaBlePresence.matchesAdvertisementName(name)
    }
}
