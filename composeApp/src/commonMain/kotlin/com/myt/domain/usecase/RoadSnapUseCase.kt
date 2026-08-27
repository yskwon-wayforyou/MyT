package com.myt.domain.usecase

import com.myt.domain.repository.PoiRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Snaps raw GPS to the nearest drivable road via OSRM (map matching lite).
 * Falls back to projecting onto nearby speed-cam road anchors, then raw GPS.
 */
class RoadSnapUseCase(
    private val httpClient: HttpClient,
    private val poiRepository: PoiRepository? = null,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val cache = object : LinkedHashMap<String, Pair<Double, Double>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Double, Double>>?): Boolean =
            size > MAX_CACHE
    }

    suspend fun snap(lat: Double, lng: Double): Pair<Double, Double> = withContext(Dispatchers.Default) {
        val key = "${"%.4f".format(lat)}:${"%.4f".format(lng)}"
        cache[key]?.let { return@withContext it }
        val snapped = runCatching {
            val url = "https://router.project-osrm.org/nearest/v1/driving/$lng,$lat?number=1"
            val body = httpClient.get(url).bodyAsText()
            val response = json.decodeFromString<OsrmNearestResponse>(body)
            val location = response.waypoints.firstOrNull()?.location
            if (location != null && location.size >= 2) {
                location[1] to location[0]
            } else {
                null
            }
        }.getOrNull() ?: localSnap(lat, lng) ?: (lat to lng)
        cache[key] = snapped
        snapped
    }

    /** Project onto nearest pair of nearby cameras as a crude road segment. */
    private fun localSnap(lat: Double, lng: Double): Pair<Double, Double>? {
        val cams = poiRepository?.findNearbyCameras(lat, lng, LOCAL_SNAP_RADIUS_M).orEmpty()
        if (cams.size < 2) {
            return cams.firstOrNull()?.let { it.latitude to it.longitude }
        }
        val a = cams[0]
        val b = cams[1]
        return projectOntoSegment(lat, lng, a.latitude, a.longitude, b.latitude, b.longitude)
    }

    private fun projectOntoSegment(
        lat: Double,
        lng: Double,
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Pair<Double, Double> {
        val dx = lng2 - lng1
        val dy = lat2 - lat1
        val len2 = dx * dx + dy * dy
        if (len2 < 1e-12) return lat1 to lng1
        val t = (((lng - lng1) * dx + (lat - lat1) * dy) / len2).coerceIn(0.0, 1.0)
        return (lat1 + t * dy) to (lng1 + t * dx)
    }

    @Serializable
    private data class OsrmNearestResponse(
        val waypoints: List<OsrmWaypoint> = emptyList(),
    )

    @Serializable
    private data class OsrmWaypoint(
        val location: List<Double> = emptyList(),
        @SerialName("distance") val distanceM: Double? = null,
    )

    companion object {
        private const val MAX_CACHE = 64
        private const val LOCAL_SNAP_RADIUS_M = 400
    }
}
