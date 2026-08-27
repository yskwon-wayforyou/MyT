package com.myt.domain.control

import com.myt.domain.usecase.TelemetryUseCase
import com.myt.phase2.PushNotifier

/**
 * In-app / simulation vehicle control — mutates [TelemetryUseCase] gauge state
 * until Phase 2 Fleet command proxy is wired.
 */
class DemoVehicleControlGateway(
    private val telemetryUseCase: TelemetryUseCase,
    private val pushNotifier: PushNotifier,
) : VehicleControlGateway {
    override suspend fun execute(request: ControlRequest): ControlResult {
        telemetryUseCase.applyDemoControl(request.command)
        pushNotifier.notify("MyT 제어", "${VehicleCommandLabels.ko(request.command)} 적용됨 (데모)")
        return ControlResult.Accepted
    }
}
