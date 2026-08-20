package com.myt.data.bluetooth

import com.myt.domain.repository.BluetoothRepository
import com.myt.platform.BluetoothPlatform
import com.myt.platform.BtConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BluetoothRepositoryImpl(
    private val platform: BluetoothPlatform,
) : BluetoothRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isConnected: Flow<Boolean> = platform.connectionState.map {
        it == BtConnectionState.Connected
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun startMonitoring() = platform.startMonitoring()

    override fun stopMonitoring() = platform.stopMonitoring()
}
