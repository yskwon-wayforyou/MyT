package com.myt.phase3

import com.myt.domain.history.TripHistoryItem
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Parses Tessie-style trip CSV exports into [TripHistoryItem].
 * Header columns are matched flexibly (case-insensitive).
 */
object TessieCsvParser {
    fun parseTrips(csv: String, defaultVin: String): List<TripHistoryItem> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        val headers = lines.first().split(',').map { it.trim().lowercase() }
        return lines.drop(1).mapNotNull { line ->
            parseRow(headers, line.split(','), defaultVin)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun parseRow(headers: List<String>, cells: List<String>, vin: String): TripHistoryItem? {
        if (cells.isEmpty()) return null
        val map = headers.mapIndexed { i, h -> h to cells.getOrNull(i)?.trim().orEmpty() }.toMap()
        val startedMs = parseEpochMs(map, listOf("start", "started_at", "start date", "start_date")) ?: return null
        val endedMs = parseEpochMs(map, listOf("end", "ended_at", "end date", "end_date"))
        val distance = map.pick(listOf("distance", "distance_km", "distance (km)", "odometer_delta"))
            ?.replace("km", "", ignoreCase = true)
            ?.trim()
            ?.toFloatOrNull() ?: 0f
        val id = map.pick(listOf("id", "trip_id"))?.ifBlank { null }
            ?: Uuid.random().toString()
        return TripHistoryItem(
            id = id,
            vin = map.pick(listOf("vin", "vehicle_vin"))?.ifBlank { null } ?: vin,
            startedAtMs = startedMs,
            endedAtMs = endedMs,
            distanceKm = distance,
            avgSpeedKmh = map.pick(listOf("avg_speed", "average_speed"))?.toFloatOrNull(),
            maxSpeedKmh = map.pick(listOf("max_speed"))?.toFloatOrNull(),
            startSoc = map.pick(listOf("start_soc", "start battery", "start_battery"))?.removeSuffix("%")?.toFloatOrNull(),
            endSoc = map.pick(listOf("end_soc", "end battery", "end_battery"))?.removeSuffix("%")?.toFloatOrNull(),
            polylineEncoded = map.pick(listOf("polyline", "route")),
            efficiencyKmPerKwh = map.pick(listOf("efficiency", "efficiency_km_per_kwh"))?.toFloatOrNull(),
        )
    }

    private fun parseEpochMs(map: Map<String, String>, keys: List<String>): Long? {
        val raw = map.pick(keys) ?: return null
        raw.toLongOrNull()?.let { return it }
        // ISO-ish fallback: yyyy-MM-dd or epoch seconds
        raw.toDoubleOrNull()?.let { d ->
            return if (d < 10_000_000_000.0) (d * 1000).toLong() else d.toLong()
        }
        return null
    }

    private fun Map<String, String>.pick(keys: List<String>): String? =
        keys.firstNotNullOfOrNull { key -> entries.firstOrNull { it.key.contains(key) }?.value?.ifBlank { null } }
}
