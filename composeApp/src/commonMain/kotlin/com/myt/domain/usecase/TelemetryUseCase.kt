package com.myt.domain.usecase

import com.myt.domain.VehicleConfig
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.repository.BluetoothRepository
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryUseCase(
    private val fleetRepository: FleetRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val _gaugeState = MutableStateFlow(GaugeState())
    val gaugeState: StateFlow<GaugeState> = _gaugeState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling(scope: CoroutineScope, config: VehicleConfig) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                val btConnected = bluetoothRepository.isConnected.first()
                if (!btConnected) {
                    _gaugeState.value = _gaugeState.value.copy(
                        connection = ConnectionStatus.Disconnected,
                    )
                    delay(5_000)
                    continue
                }

                val result = fleetRepository.fetchVehicleState(config.vin)
                result.onSuccess { state ->
                    _gaugeState.value = state.copy(
                        connection = when {
                            state.isSleeping -> ConnectionStatus.Sleeping
                            else -> ConnectionStatus.FleetConnected
                        },
                    )
                }.onFailure {
                    _gaugeState.value = _gaugeState.value.copy(
                        connection = ConnectionStatus.Error,
                    )
                }

                val interval = pollingInterval(_gaugeState.value, config)
                delay(interval)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun pollingInterval(state: GaugeState, config: VehicleConfig): Long = when {
        state.isSleeping -> config.pollingIntervalSleepMs
        state.charging?.isCharging == true -> config.pollingIntervalChargingMs
        state.gear == Gear.PARK -> config.pollingIntervalParkedMs
        else -> config.pollingIntervalDrivingMs
    }
}
