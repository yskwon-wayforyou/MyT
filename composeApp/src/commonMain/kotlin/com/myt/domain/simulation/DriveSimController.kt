package com.myt.domain.simulation

import com.myt.domain.usecase.TelemetryUseCase
import com.myt.platform.AudioAlertPlatform
import com.myt.platform.HapticPlatform
import kotlinx.coroutines.CoroutineScope

class DriveSimController(
    private val telemetryUseCase: TelemetryUseCase,
    private val appScope: CoroutineScope,
    private val audioAlertPlatform: AudioAlertPlatform,
    private val hapticPlatform: HapticPlatform,
) {
    fun start(scenarioId: DrivingSimulationId) {
        audioAlertPlatform.setAlertsSuppressed(true)
        hapticPlatform.setHapticsSuppressed(true)
        telemetryUseCase.startDrivingSimulation(appScope, scenarioId)
    }

    fun stop() {
        telemetryUseCase.stopDrivingSimulation()
        audioAlertPlatform.setAlertsSuppressed(false)
        hapticPlatform.setHapticsSuppressed(false)
    }

    val isRunning: Boolean get() = telemetryUseCase.isSimulating
}
