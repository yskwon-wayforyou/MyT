package com.myt.phase15

import com.myt.debug.DebugLogger
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.repository.FleetRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** M20 — Phase 1.5: Fleet Telemetry stream contract. */
interface TelemetryStreamClient {
    val gaugeStream: Flow<GaugeState>
    suspend fun connect(vin: String)
    suspend fun disconnect()
}

/**
 * Prefer configurable WebSocket endpoint (`tesla.telemetry.wss.url`).
 * Falls back to Fleet REST polling when WSS URL is blank or connection fails.
 */
class HybridTelemetryStreamClient(
    private val httpClient: HttpClient,
    private val fleetRepository: FleetRepository,
    private val scope: CoroutineScope,
    private val wssUrlProvider: () -> String,
    private val debugLogger: DebugLogger,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TelemetryStreamClient {
    private val _stream = MutableStateFlow(GaugeState())
    private var job: Job? = null

    override val gaugeStream: Flow<GaugeState> = _stream.asStateFlow()

    override suspend fun connect(vin: String) {
        job?.cancel()
        job = scope.launch {
            val wss = wssUrlProvider().trim()
            if (wss.isNotBlank()) {
                debugLogger.i("TelemetryWss", "Connecting WSS…")
                runCatching { connectWss(wss, vin) }
                    .onFailure { debugLogger.w("TelemetryWss", "WSS failed: ${it.message}; fallback polling") }
            }
            // Always keep polling as durable fallback / primary when no WSS.
            fleetRepository.observeVehicleState(vin).collect { state ->
                _stream.value = state
            }
        }
    }

    private suspend fun connectWss(url: String, vin: String) {
        val client = HttpClient {
            install(WebSockets)
        }
        try {
            client.webSocket(urlString = url) {
                send(Frame.Text("""{"type":"subscribe","vin":"$vin"}"""))
                for (frame in incoming) {
                    if (!isActive) break
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            parseGauge(text)?.let { _stream.value = it }
                        }
                        else -> Unit
                    }
                }
            }
        } finally {
            client.close()
        }
    }

    private fun parseGauge(text: String): GaugeState? = runCatching {
        val payload = json.decodeFromString(TelemetryPayload.serializer(), text)
        GaugeState(
            speedKmh = payload.speedKmh ?: 0f,
            gear = payload.gear?.let { runCatching { Gear.valueOf(it) }.getOrNull() } ?: Gear.PARK,
            socPercent = payload.socPercent ?: 0f,
            rangeKm = payload.rangeKm ?: 0f,
            insideTempC = payload.insideTempC,
            outsideTempC = payload.outsideTempC,
            powerKw = payload.powerKw,
            isSleeping = payload.isSleeping ?: false,
            connection = ConnectionStatus.FleetConnected,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
        )
    }.getOrNull()

    override suspend fun disconnect() {
        job?.cancel()
        job = null
    }
}

@Serializable
private data class TelemetryPayload(
    @SerialName("speed_kmh") val speedKmh: Float? = null,
    val gear: String? = null,
    @SerialName("soc_percent") val socPercent: Float? = null,
    @SerialName("range_km") val rangeKm: Float? = null,
    @SerialName("inside_temp_c") val insideTempC: Float? = null,
    @SerialName("outside_temp_c") val outsideTempC: Float? = null,
    @SerialName("power_kw") val powerKw: Float? = null,
    @SerialName("is_sleeping") val isSleeping: Boolean? = null,
)

/** @deprecated Prefer [HybridTelemetryStreamClient]. */
class PollingTelemetryStreamClient(
    private val fleetRepository: FleetRepository,
    private val scope: CoroutineScope,
) : TelemetryStreamClient {
    private val _stream = MutableStateFlow(GaugeState())
    private var job: Job? = null

    override val gaugeStream: Flow<GaugeState> = _stream.asStateFlow()

    override suspend fun connect(vin: String) {
        job?.cancel()
        job = scope.launch {
            fleetRepository.observeVehicleState(vin).collect { state ->
                _stream.value = state
            }
        }
    }

    override suspend fun disconnect() {
        job?.cancel()
        job = null
    }
}

class StubTelemetryStreamClient : TelemetryStreamClient {
    override val gaugeStream: Flow<GaugeState> = kotlinx.coroutines.flow.emptyFlow()
    override suspend fun connect(vin: String) = Unit
    override suspend fun disconnect() = Unit
}
