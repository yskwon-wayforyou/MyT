package com.myt.phase3

import com.myt.config.HaIntegrationConfig
import com.myt.domain.model.GaugeState
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * M39 — publishes gauge state to Home Assistant REST API.
 * MQTT discovery JSON is generated via [HaDiscoveryBuilder] for manual broker setup.
 */
class HaRestStateBridge(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : HomeAssistantBridge {
    private var lastPublishMs: Long = 0L

    suspend fun publishGaugeState(config: HaIntegrationConfig, vin: String, state: GaugeState): Result<Unit> {
        if (!config.enabled || config.baseUrl.isBlank() || config.accessToken.isBlank()) {
            return Result.success(Unit)
        }
        val suffix = config.vinSuffix.ifBlank { vin.takeLast(6) }
        val base = config.baseUrl.trimEnd('/')
        val token = config.accessToken
        return runCatching {
            publishEntity(base, token, "sensor.myt_${suffix}_soc", HaEntityMapper.socState(state.socPercent))
            publishEntity(base, token, "sensor.myt_${suffix}_speed", HaEntityMapper.speedState(state.speedKmh))
            publishEntity(
                base,
                token,
                "sensor.myt_${suffix}_range",
                HaEntityMapper.rangeState(state.rangeKm ?: 0f),
            )
            lastPublishMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }

    fun discoveryMessages(config: HaIntegrationConfig, vin: String): Map<String, String> {
        val suffix = config.vinSuffix.ifBlank { vin.takeLast(6) }
        val prefix = config.topicPrefix
        return mapOf(
            HaDiscoveryBuilder.discoveryTopic("sensor", "myt_$suffix", "soc") to
                HaDiscoveryBuilder.sensorDiscovery(prefix, suffix, "soc", "MyT SOC", "%", "battery"),
            HaDiscoveryBuilder.discoveryTopic("sensor", "myt_$suffix", "speed") to
                HaDiscoveryBuilder.sensorDiscovery(prefix, suffix, "speed", "MyT Speed", "km/h", null),
            HaDiscoveryBuilder.discoveryTopic("sensor", "myt_$suffix", "range") to
                HaDiscoveryBuilder.sensorDiscovery(prefix, suffix, "range", "MyT Range", "km", "distance"),
        )
    }

    override suspend fun publishState(topic: String, payload: String) {
        // Generic MQTT-style hook — REST bridge ignores topic when using HA API directly.
    }

    private suspend fun publishEntity(baseUrl: String, token: String, entityId: String, update: HaStateUpdate) {
        val response = httpClient.post("$baseUrl/api/states/$entityId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(update))
        }
        if (!response.status.isSuccess()) {
            error("HA state update failed: ${response.status}")
        }
    }
}

class StubHomeAssistantBridge : HomeAssistantBridge {
    override suspend fun publishState(topic: String, payload: String) = Unit
}
