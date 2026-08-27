package com.myt.phase3

import com.myt.domain.history.TripHistoryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Co2CalculatorTest {
    @Test
    fun summarizeTrips_computesPositiveSavings() {
        val trips = listOf(
            TripHistoryItem(
                id = "t1",
                vin = "VIN",
                startedAtMs = 1L,
                endedAtMs = 2L,
                distanceKm = 100f,
                avgSpeedKmh = 60f,
                maxSpeedKmh = 90f,
                startSoc = 80f,
                endSoc = 60f,
                efficiencyKmPerKwh = 6f,
            ),
        )
        val summary = Co2Calculator.summarizeTrips(trips)
        assertEquals(100f, summary.totalDistanceKm)
        assertTrue(summary.co2SavedKg > 0f)
    }
}

class TessieCsvParserTest {
    @Test
    fun parseTrips_readsFlexibleHeaders() {
        val csv = """
            id,start,distance_km,start_soc,end_soc
            trip-1,1700000000000,12.5,80,72
        """.trimIndent()
        val trips = TessieCsvParser.parseTrips(csv, "DEFAULTVIN")
        assertEquals(1, trips.size)
        assertEquals("trip-1", trips.first().id)
        assertEquals(12.5f, trips.first().distanceKm)
        assertEquals("DEFAULTVIN", trips.first().vin)
    }
}

class HaDiscoveryBuilderTest {
    @Test
    fun sensorDiscovery_containsUniqueId() {
        val json = HaDiscoveryBuilder.sensorDiscovery("myt", "abc123", "soc", "SOC", "%", "battery")
        assertTrue(json.contains("myt_abc123_soc"))
        assertTrue(json.contains("state_topic"))
    }
}
