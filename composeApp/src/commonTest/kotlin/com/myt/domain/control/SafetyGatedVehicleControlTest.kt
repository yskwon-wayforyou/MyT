package com.myt.domain.control

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SafetyGatedVehicleControlTest {
    @Test
    fun rejectsUnlockWhileDriving() = runBlocking {
        val gated = SafetyGatedVehicleControl(
            gateway = StubVehicleControlGateway(),
            isDriving = { true },
        )
        val result = gated.execute(ControlRequest(VehicleCommand.Unlock, "VIN"))
        assertTrue(result is ControlResult.Rejected)
    }

    @Test
    fun allowsLockWhileDriving() = runBlocking {
        val gated = SafetyGatedVehicleControl(
            gateway = StubVehicleControlGateway(),
            isDriving = { true },
        )
        val result = gated.execute(ControlRequest(VehicleCommand.Lock, "VIN"))
        assertTrue(result is ControlResult.Accepted)
    }
}
