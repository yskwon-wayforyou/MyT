package com.myt.phase3

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Builds Home Assistant MQTT discovery payloads (M39). */
object HaDiscoveryBuilder {
    private val json = Json { encodeDefaults = true }

    fun sensorDiscovery(
        topicPrefix: String,
        vinSuffix: String,
        sensorKey: String,
        name: String,
        unit: String?,
        deviceClass: String?,
    ): String {
        val unique = listOf("myt", vinSuffix.ifBlank { "vehicle" }, sensorKey).joinToString("_")
        val payload = HaDiscoveryPayload(
            name = name,
            state_topic = "$topicPrefix/$vinSuffix/state/$sensorKey",
            unit_of_measurement = unit,
            device_class = deviceClass,
            unique_id = unique,
            device = HaDeviceInfo(
                identifiers = listOf("myt_$vinSuffix"),
                name = "MyT Tesla ($vinSuffix)",
            ),
        )
        return json.encodeToString(payload)
    }

    fun discoveryTopic(component: String, nodeId: String, objectId: String): String =
        "homeassistant/$component/$nodeId/$objectId/config"
}

@Serializable
data class HaStateUpdate(
    val state: String,
    val attributes: Map<String, String> = emptyMap(),
)

object HaEntityMapper {
    fun socState(soc: Float): HaStateUpdate =
        HaStateUpdate(state = soc.toInt().toString(), attributes = mapOf("unit" to "%"))

    fun speedState(speedKmh: Float): HaStateUpdate =
        HaStateUpdate(state = speedKmh.toInt().toString(), attributes = mapOf("unit" to "km/h"))

    fun rangeState(rangeKm: Float): HaStateUpdate =
        HaStateUpdate(state = rangeKm.toInt().toString(), attributes = mapOf("unit" to "km"))
}
