package com.myt.backend.state

import kotlinx.serialization.Serializable

/** Shared mock vehicle state for /dash and API until live telemetry is wired. */
object DashboardStateStore {
    @Volatile
    var state: DashboardVehicleState = DashboardVehicleState()

    fun update(block: (DashboardVehicleState) -> DashboardVehicleState) {
        state = block(state)
    }
}

@Serializable
data class DashboardVehicleState(
    val vin: String = "5YJ3E1EA0KF000001",
    val displayName: String = "MyT Demo",
    val soc: Int = 72,
    val speedKmh: Int = 0,
    val rangeKm: Int = 280,
    val locked: Boolean = true,
    val climateOn: Boolean = false,
    val isCharging: Boolean = false,
)
