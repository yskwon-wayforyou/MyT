package com.myt.domain.control

import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.TokenRepository
import com.myt.domain.usecase.TelemetryUseCase
import com.myt.phase2.PushNotifier

/**
 * W1 — real Tesla Fleet vehicle commands.
 * Clear errors when Virtual Key / signed proxy is required (HTTP 403).
 */
class FleetVehicleControlGateway(
    private val fleetRepository: FleetRepository,
    private val pushNotifier: PushNotifier,
) : VehicleControlGateway {
    override suspend fun execute(request: ControlRequest): ControlResult {
        val (path, trunk) = mapCommand(request.command)
        val result = fleetRepository.sendVehicleCommand(request.vin, path, trunk)
        return result.fold(
            onSuccess = {
                pushNotifier.notify("MyT 제어", "${label(request.command)} 전송됨")
                ControlResult.Accepted
            },
            onFailure = { error ->
                val reason = humanize(error)
                pushNotifier.notify("MyT 제어 실패", reason)
                ControlResult.Rejected(reason)
            },
        )
    }

    private fun mapCommand(command: VehicleCommand): Pair<String, String?> = when (command) {
        VehicleCommand.Lock -> "door_lock" to null
        VehicleCommand.Unlock -> "door_unlock" to null
        VehicleCommand.ClimateOn -> "auto_conditioning_start" to null
        VehicleCommand.ClimateOff -> "auto_conditioning_stop" to null
        VehicleCommand.Trunk -> "actuate_trunk" to "rear"
        VehicleCommand.Frunk -> "actuate_trunk" to "front"
        VehicleCommand.Flash -> "flash_lights" to null
        VehicleCommand.Honk -> "honk_horn" to null
    }

    private fun label(command: VehicleCommand): String = when (command) {
        VehicleCommand.Lock -> "잠금"
        VehicleCommand.Unlock -> "잠금 해제"
        VehicleCommand.ClimateOn -> "공조 ON"
        VehicleCommand.ClimateOff -> "공조 OFF"
        VehicleCommand.Trunk -> "트렁크"
        VehicleCommand.Frunk -> "프렁크"
        VehicleCommand.Flash -> "라이트"
        VehicleCommand.Honk -> "경적"
    }

    private fun humanize(error: Throwable): String {
        val msg = error.message.orEmpty()
        val lower = msg.lowercase()
        return when {
            "403" in lower || "sign" in lower || "virtual" in lower ->
                "명령 서명/Virtual Key가 필요합니다. Auth 테스트(W1)에서 키를 등록해 주세요. ($msg)"
            "oauth" in lower || "token" in lower || "log in" in lower ->
                "Tesla 로그인이 필요합니다"
            "한도" in msg || "quota" in lower ->
                msg
            else -> msg.ifBlank { "Fleet 명령 실패" }
        }
    }
}

/**
 * Uses Demo while simulating or logged out; otherwise Fleet.
 */
class SelectingVehicleControlGateway(
    private val fleet: FleetVehicleControlGateway,
    private val demo: DemoVehicleControlGateway,
    private val telemetryUseCase: TelemetryUseCase,
    private val tokenRepository: TokenRepository,
) : VehicleControlGateway {
    override suspend fun execute(request: ControlRequest): ControlResult {
        val useDemo = telemetryUseCase.isSimulating || !tokenRepository.isAuthenticated()
        return if (useDemo) demo.execute(request) else fleet.execute(request)
    }
}
