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
        val mapped = mapCommand(request.command)
        val result = fleetRepository.sendVehicleCommand(
            vin = request.vin,
            commandName = mapped.path,
            whichTrunk = mapped.whichTrunk,
            jsonBody = mapped.jsonBody,
        )
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

    private data class MappedCommand(
        val path: String,
        val whichTrunk: String? = null,
        val jsonBody: String? = null,
    )

    private fun mapCommand(command: VehicleCommand): MappedCommand = when (command) {
        VehicleCommand.Lock -> MappedCommand("door_lock")
        VehicleCommand.Unlock -> MappedCommand("door_unlock")
        VehicleCommand.ClimateOn -> MappedCommand("auto_conditioning_start")
        VehicleCommand.ClimateOff -> MappedCommand("auto_conditioning_stop")
        VehicleCommand.Trunk -> MappedCommand("actuate_trunk", whichTrunk = "rear")
        VehicleCommand.Frunk -> MappedCommand("actuate_trunk", whichTrunk = "front")
        VehicleCommand.Flash -> MappedCommand("flash_lights")
        VehicleCommand.Honk -> MappedCommand("honk_horn")
        VehicleCommand.SentryOn -> MappedCommand("set_sentry_mode", jsonBody = """{"on":true}""")
        VehicleCommand.SentryOff -> MappedCommand("set_sentry_mode", jsonBody = """{"on":false}""")
        // climate_keeper_mode: 0 Off · 1 Keep · 2 Dog · 3 Camp
        VehicleCommand.DogMode -> MappedCommand(
            "set_climate_keeper_mode",
            jsonBody = """{"climate_keeper_mode":2}""",
        )
        VehicleCommand.CampMode -> MappedCommand(
            "set_climate_keeper_mode",
            jsonBody = """{"climate_keeper_mode":3}""",
        )
        VehicleCommand.WindowVent -> MappedCommand(
            "window_control",
            jsonBody = """{"command":"vent","lat":0,"lon":0}""",
        )
        VehicleCommand.ChargePortOpen -> MappedCommand("charge_port_door_open")
        VehicleCommand.ChargePortClose -> MappedCommand("charge_port_door_close")
    }

    private fun label(command: VehicleCommand): String = VehicleCommandLabels.ko(command)

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
