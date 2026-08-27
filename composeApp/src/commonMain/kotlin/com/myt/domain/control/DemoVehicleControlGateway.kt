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
        val label = when (request.command) {
            VehicleCommand.Lock -> "차량 잠금"
            VehicleCommand.Unlock -> "차량 해제"
            VehicleCommand.ClimateOn -> "공조 ON"
            VehicleCommand.ClimateOff -> "공조 OFF"
            VehicleCommand.Trunk -> "트렁크 명령"
            VehicleCommand.Frunk -> "프렁크 명령"
            VehicleCommand.Flash -> "라이트 점멸"
            VehicleCommand.Honk -> "경적"
        }
        pushNotifier.notify("MyT 제어", "$label 적용됨 (데모)")
        return ControlResult.Accepted
    }
}
