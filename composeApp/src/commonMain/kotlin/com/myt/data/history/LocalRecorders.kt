package com.myt.data.history

import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.geo.PolylineCodec
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.repository.ChargeSessionRecorder
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.repository.TripRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.math.max
import kotlin.math.pow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalTripRecorder(
    private val historyRepository: HistoryRepository,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) : TripRecorder {
    override var isRecording: Boolean = false
        private set

    private var active: ActiveTrip? = null

    override fun onGaugeUpdate(state: GaugeState, vin: String) {
        if (vin.isBlank()) return
        val driving = state.gear != Gear.PARK && state.speedKmh > 2f
        when {
            driving && active == null -> startTrip(state, vin)
            driving -> updateTrip(state)
            state.gear == Gear.PARK && active != null -> scope.launch { flushCurrentTrip() }
        }
    }

    override suspend fun flushCurrentTrip() {
        val trip = active ?: return
        val polylineEncoded = trip.points.let { points ->
            PolylineCodec.encode(points)
                .takeIf { it.isNotBlank() }
        }

        val efficiency = estimateEfficiencyKmPerKwh(
            distanceKm = trip.distanceKm,
            startSoc = trip.item.startSoc,
            endSoc = trip.lastSoc,
        )

        val ended = trip.item.copy(
            endedAtMs = clock.now().toEpochMilliseconds(),
            endSoc = trip.lastSoc,
            distanceKm = trip.distanceKm,
            avgSpeedKmh = if (trip.speedSamples.isEmpty()) null else trip.speedSamples.average().toFloat(),
            maxSpeedKmh = trip.speedSamples.maxOrNull(),
            polylineEncoded = polylineEncoded,
            efficiencyKmPerKwh = efficiency,
        )
        historyRepository.updateTripEnd(ended)
        active = null
        isRecording = false
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun startTrip(state: GaugeState, vin: String) {
        val id = Uuid.random().toString()
        val nowMs = clock.now().toEpochMilliseconds()
        val firstPoint = state.latitude?.let { lat ->
            state.longitude?.let { lng ->
                PolylineCodec.LatLng(lat, lng)
            }
        }

        val item = TripHistoryItem(
            id = id,
            vin = vin,
            startedAtMs = clock.now().toEpochMilliseconds(),
            endedAtMs = null,
            distanceKm = 0f,
            avgSpeedKmh = null,
            maxSpeedKmh = state.speedKmh,
            startSoc = state.socPercent,
            endSoc = null,
        )
        active = ActiveTrip(
            item = item,
            lastOdometer = state.odometerKm,
            lastSoc = state.socPercent,
            speedSamples = mutableListOf(state.speedKmh),
            points = (firstPoint?.let { mutableListOf(it) } ?: mutableListOf()),
            lastPointAtMs = nowMs,
        )
        isRecording = true
        scope.launch { historyRepository.recordTrip(item) }
    }

    private fun updateTrip(state: GaugeState) {
        val current = active ?: return
        val odometerDelta = if (current.lastOdometer != null && state.odometerKm != null) {
            max(0f, state.odometerKm - current.lastOdometer)
        } else {
            0f
        }
        current.speedSamples.add(state.speedKmh)

        var nextLastPointAtMs = current.lastPointAtMs
        val nowMs = clock.now().toEpochMilliseconds()

        val lat = state.latitude
        val lng = state.longitude
        if (lat != null && lng != null) {
            val candidate = PolylineCodec.LatLng(lat, lng)
            val last = current.points.lastOrNull()
            val shouldAdd = if (last == null) {
                true
            } else {
                val distanceM = distanceMeters(last.lat, last.lng, candidate.lat, candidate.lng)
                distanceM >= MIN_POINT_DISTANCE_M || (nowMs - current.lastPointAtMs) >= MIN_POINT_TIME_MS
            }
            if (shouldAdd) {
                current.points.add(candidate)
                nextLastPointAtMs = nowMs
            }
        }

        active = current.copy(
            lastOdometer = state.odometerKm ?: current.lastOdometer,
            lastSoc = state.socPercent,
            distanceKm = current.distanceKm + odometerDelta,
            lastPointAtMs = nextLastPointAtMs,
            item = current.item.copy(
                maxSpeedKmh = max(current.item.maxSpeedKmh ?: 0f, state.speedKmh),
            ),
        )
    }

    private data class ActiveTrip(
        val item: TripHistoryItem,
        val lastOdometer: Float?,
        val lastSoc: Float,
        val speedSamples: MutableList<Float>,
        val distanceKm: Float = 0f,
        val points: MutableList<PolylineCodec.LatLng> = mutableListOf(),
        val lastPointAtMs: Long = 0L,
    )

    private fun estimateEfficiencyKmPerKwh(
        distanceKm: Float,
        startSoc: Float?,
        endSoc: Float,
    ): Float? {
        if (distanceKm <= 0f) return null
        val start = startSoc ?: return null
        val deltaSoc = (start - endSoc).coerceAtLeast(0f) / 100f
        if (deltaSoc <= 0f) return null
        val energyKwh = deltaSoc * BATTERY_KWH
        if (energyKwh <= 0f) return null
        return distanceKm / energyKwh
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        // Lightweight approximation for short GPS segments.
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2).pow(2) +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).pow(2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    private companion object {
        private const val BATTERY_KWH = 75f
        private const val MIN_POINT_DISTANCE_M = 30.0 // avoid too dense polyline
        private const val MIN_POINT_TIME_MS = 3_000L
        private const val EARTH_RADIUS_M = 6_371_000.0
    }
}

class LocalChargeSessionRecorder(
    private val historyRepository: HistoryRepository,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) : ChargeSessionRecorder {
    private var activeId: String? = null
    private var startSoc: Float = 0f
    private var startMs: Long = 0L
    private var peakKw: Float = 0f
    private var vin: String = ""
    private var lastProgressAtMs: Long = 0L

    override fun onGaugeUpdate(state: GaugeState, vin: String) {
        if (vin.isBlank()) return
        val charging = state.charging?.isCharging == true
        when {
            charging && activeId == null -> startSession(state, vin)
            charging -> {
                peakKw = max(peakKw, state.charging?.chargeRateKw ?: state.powerKw ?: 0f)
                val nowMs = clock.now().toEpochMilliseconds()
                if (nowMs - lastProgressAtMs >= PROGRESS_INTERVAL_MS) {
                    lastProgressAtMs = nowMs
                    val endSoc = state.socPercent
                    val energy = ((endSoc - startSoc) / 100f * BATTERY_KWH).coerceAtLeast(0f)
                    scope.launch {
                        historyRepository.recordCharge(
                            ChargeHistoryItem(
                                id = activeId!!,
                                vin = this@LocalChargeSessionRecorder.vin,
                                startedAtMs = startMs,
                                endedAtMs = null,
                                startSoc = startSoc,
                                endSoc = endSoc,
                                energyKwh = energy,
                                peakKw = peakKw,
                            ),
                        )
                    }
                }
            }
            !charging && activeId != null -> scope.launch { endSession(state) }
        }
    }

    override suspend fun activeSessionId(): String? = activeId

    @OptIn(ExperimentalUuidApi::class)
    private fun startSession(state: GaugeState, vin: String) {
        activeId = Uuid.random().toString()
        startSoc = state.socPercent
        startMs = clock.now().toEpochMilliseconds()
        peakKw = state.charging?.chargeRateKw ?: 0f
        this.vin = vin
        val item = ChargeHistoryItem(
            id = activeId!!,
            vin = vin,
            startedAtMs = startMs,
            endedAtMs = null,
            startSoc = startSoc,
            endSoc = null,
            energyKwh = null,
            peakKw = peakKw,
        )
        scope.launch { historyRepository.recordCharge(item) }
    }

    private suspend fun endSession(state: GaugeState) {
        val id = activeId ?: return
        val endSoc = state.socPercent
        val energy = ((endSoc - startSoc) / 100f * BATTERY_KWH).coerceAtLeast(0f)
        historyRepository.recordCharge(
            ChargeHistoryItem(
                id = id,
                vin = vin,
                startedAtMs = startMs,
                endedAtMs = clock.now().toEpochMilliseconds(),
                startSoc = startSoc,
                endSoc = endSoc,
                energyKwh = energy,
                peakKw = peakKw,
            ),
        )
        activeId = null
        peakKw = 0f
        lastProgressAtMs = 0L
    }

    companion object {
        private const val BATTERY_KWH = 75f
        private const val PROGRESS_INTERVAL_MS = 15_000L
    }
}
