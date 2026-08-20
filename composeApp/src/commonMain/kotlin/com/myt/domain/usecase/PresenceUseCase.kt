package com.myt.domain.usecase

import com.myt.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

class PresenceUseCase(
    private val bluetoothRepository: BluetoothRepository,
) {
    val isVehiclePresent: Flow<Boolean> = bluetoothRepository.isConnected

    fun startMonitoring() = bluetoothRepository.startMonitoring()

    fun stopMonitoring() = bluetoothRepository.stopMonitoring()
}
