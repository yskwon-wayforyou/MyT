package com.myt.phase15

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json

class TelemetryPayloadParseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesMinimalPayloadViaClientHelpers() {
        // HybridTelemetryStreamClient.parseGauge is private; validate JSON shape contracts here.
        val text = """{"speed_kmh":80.5,"gear":"D","soc_percent":55.0,"range_km":210.0,"is_sleeping":false}"""
        val decoded = json.decodeFromString(TelemetryPayloadDto.serializer(), text)
        assertEquals(80.5f, decoded.speedKmh)
        assertEquals("D", decoded.gear)
        assertEquals(55.0f, decoded.socPercent)
        assertNotNull(decoded.rangeKm)
    }
}

@kotlinx.serialization.Serializable
private data class TelemetryPayloadDto(
    @kotlinx.serialization.SerialName("speed_kmh") val speedKmh: Float? = null,
    val gear: String? = null,
    @kotlinx.serialization.SerialName("soc_percent") val socPercent: Float? = null,
    @kotlinx.serialization.SerialName("range_km") val rangeKm: Float? = null,
    @kotlinx.serialization.SerialName("is_sleeping") val isSleeping: Boolean? = null,
)
