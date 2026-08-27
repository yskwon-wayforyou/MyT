package com.myt.domain.control

/**
 * M29/M31 — vehicle remote commands with driving safety gate.
 */
enum class VehicleCommand {
    Lock,
    Unlock,
    ClimateOn,
    ClimateOff,
    Trunk,
    Frunk,
    Flash,
    Honk,
}

data class ControlRequest(
    val command: VehicleCommand,
    val vin: String,
)

sealed class ControlResult {
    data object Accepted : ControlResult()
    data class Rejected(val reason: String) : ControlResult()
}

interface VehicleControlGateway {
    suspend fun execute(request: ControlRequest): ControlResult
}

class SafetyGatedVehicleControl(
    private val gateway: VehicleControlGateway,
    private val isDriving: () -> Boolean,
) : VehicleControlGateway {
    override suspend fun execute(request: ControlRequest): ControlResult {
        val blockedWhileDriving = setOf(
            VehicleCommand.Unlock,
            VehicleCommand.Trunk,
            VehicleCommand.Frunk,
        )
        if (isDriving() && request.command in blockedWhileDriving) {
            return ControlResult.Rejected("주행 중에는 ${request.command} 명령을 보낼 수 없습니다")
        }
        return gateway.execute(request)
    }
}

/** Local scaffold until Phase 2 Fleet command proxy is live. */
class StubVehicleControlGateway : VehicleControlGateway {
    override suspend fun execute(request: ControlRequest): ControlResult =
        ControlResult.Accepted
}
