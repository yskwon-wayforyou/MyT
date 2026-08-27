package com.myt.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.juul.kable.Scanner
import com.myt.domain.bluetooth.TeslaBlePresence
import com.myt.service.PresenceService
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

/**
 * Android presence: Tesla Phone Key BLE ads + GATT connected devices + filtered ACL.
 */
actual class BluetoothPlatform actual constructor(context: Any) {
    private val appContext = (context as Context).applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _connectionState = MutableStateFlow(BtConnectionState.Disconnected)
    private var scanJob: Job? = null
    private var pollJob: Job? = null
    private var lastBleSeenMs: Long = 0L

    private val listener: (Boolean) -> Unit = { connected ->
        if (connected || recentBleSighting()) {
            _connectionState.value = BtConnectionState.Connected
        } else {
            _connectionState.value = BtConnectionState.Disconnected
        }
    }

    actual val connectionState: Flow<BtConnectionState> = _connectionState.asStateFlow()

    actual fun startMonitoring() {
        BtConnectionHub.attach(appContext)
        BtConnectionHub.register(listener)
        if (hasBlePermission()) {
            val intent = Intent(appContext, PresenceService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            }
        }
        startBleScan()
        startPresencePoll()
        refreshPresence()
    }

    actual fun stopMonitoring() {
        scanJob?.cancel()
        pollJob?.cancel()
        scanJob = null
        pollJob = null
        appContext.stopService(Intent(appContext, PresenceService::class.java))
        BtConnectionHub.unregister(listener)
        _connectionState.value = BtConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (!hasBlePermission()) return
        scanJob?.cancel()
        scanJob = scope.launch {
            _connectionState.value = BtConnectionState.Connecting
            while (true) {
                runCatching {
                    Scanner().advertisements
                        .catch { /* keep scanning */ }
                        .collect { advertisement ->
                            val name = advertisement.name
                            val match = TeslaBlePresence.matchesAdvertisementName(name)
                            if (match) {
                                lastBleSeenMs = System.currentTimeMillis()
                                _connectionState.value = BtConnectionState.Connected
                            }
                        }
                }
                delay(2_000)
            }
        }
    }

    private fun startPresencePoll() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                refreshPresence()
                delay(3_000)
            }
        }
    }

    private fun refreshPresence() {
        if (!hasBlePermission()) return
        if (recentBleSighting() || BtConnectionHub.detectTeslaPresent()) {
            _connectionState.value = BtConnectionState.Connected
        } else if (_connectionState.value == BtConnectionState.Connected && !recentBleSighting()) {
            _connectionState.value = BtConnectionState.Disconnected
        }
    }

    private fun recentBleSighting(): Boolean =
        System.currentTimeMillis() - lastBleSeenMs < 20_000

    private fun hasBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
