package com.myt.phase15

import com.myt.domain.model.GaugeState
import kotlinx.coroutines.flow.Flow

/** M20 — Phase 1.5: Fleet Telemetry WebSocket client (stub). */
interface TelemetryStreamClient {
    val gaugeStream: Flow<GaugeState>
    suspend fun connect(vin: String)
    suspend fun disconnect()
}

class StubTelemetryStreamClient : TelemetryStreamClient {
    override val gaugeStream: Flow<GaugeState> = kotlinx.coroutines.flow.emptyFlow()
    override suspend fun connect(vin: String) = Unit
    override suspend fun disconnect() = Unit
}
