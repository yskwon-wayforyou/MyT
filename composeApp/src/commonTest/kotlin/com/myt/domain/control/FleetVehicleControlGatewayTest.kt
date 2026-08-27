package com.myt.domain.control

import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.TokenRepository
import com.myt.phase2.PushNotifier
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FleetVehicleControlGatewayTest {
    @Test
    fun fleetGateway_mapsLockToDoorLock() = runBlocking {
        var seen = ""
        val fleet = object : FleetRepository by EmptyFleetRepository {
            override suspend fun sendVehicleCommand(
                vin: String,
                commandName: String,
                whichTrunk: String?,
                jsonBody: String?,
            ): Result<Unit> {
                seen = commandName
                return Result.success(Unit)
            }
        }
        val gateway = FleetVehicleControlGateway(fleet, NoopPush)
        val result = gateway.execute(ControlRequest(VehicleCommand.Lock, "VIN"))
        assertEquals(ControlResult.Accepted, result)
        assertEquals("door_lock", seen)
    }

    @Test
    fun fleetGateway_rejectsWithSigningHintOn403() = runBlocking {
        val fleet = object : FleetRepository by EmptyFleetRepository {
            override suspend fun sendVehicleCommand(
                vin: String,
                commandName: String,
                whichTrunk: String?,
                jsonBody: String?,
            ): Result<Unit> = Result.failure(IllegalStateException("Fleet door_lock failed: HTTP 403 unsigned"))
        }
        val gateway = FleetVehicleControlGateway(fleet, NoopPush)
        val result = gateway.execute(ControlRequest(VehicleCommand.Honk, "VIN"))
        assertTrue(result is ControlResult.Rejected)
        assertTrue((result as ControlResult.Rejected).reason.contains("Virtual Key"))
    }

    @Test
    fun fleetGateway_mapsSentryOnWithBody() = runBlocking {
        var seenName = ""
        var seenBody: String? = null
        val fleet = object : FleetRepository by EmptyFleetRepository {
            override suspend fun sendVehicleCommand(
                vin: String,
                commandName: String,
                whichTrunk: String?,
                jsonBody: String?,
            ): Result<Unit> {
                seenName = commandName
                seenBody = jsonBody
                return Result.success(Unit)
            }
        }
        val gateway = FleetVehicleControlGateway(fleet, NoopPush)
        val result = gateway.execute(ControlRequest(VehicleCommand.SentryOn, "VIN"))
        assertEquals(ControlResult.Accepted, result)
        assertEquals("set_sentry_mode", seenName)
        assertEquals("""{"on":true}""", seenBody)
    }
}

private object NoopPush : PushNotifier {
    override suspend fun notify(title: String, body: String): Result<Unit> = Result.success(Unit)
}

/** Minimal stub — only sendVehicleCommand overridden in tests. */
private object EmptyFleetRepository : FleetRepository {
    override fun observeVehicleState(vin: String) = throw UnsupportedOperationException()
    override suspend fun fetchVehicleState(vin: String) = throw UnsupportedOperationException()
    override suspend fun sendNavigationRequest(vin: String, destination: String) =
        throw UnsupportedOperationException()
    override suspend fun wakeVehicle(vin: String) = throw UnsupportedOperationException()
    override suspend fun sendVehicleCommand(
        vin: String,
        commandName: String,
        whichTrunk: String?,
        jsonBody: String?,
    ) = throw UnsupportedOperationException()
}
