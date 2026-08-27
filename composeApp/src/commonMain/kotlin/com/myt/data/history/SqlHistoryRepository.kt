package com.myt.data.history

import com.myt.data.local.MyTDatabase
import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.history.HistorySortOrder
import com.myt.domain.history.HistoryTab
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.history.VehicleSnapshotPayload
import com.myt.domain.model.ChargeInfo
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.TelemetrySource
import com.myt.domain.model.TirePressures
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlHistoryRepository(
    private val db: MyTDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: Clock = Clock.System,
) : HistoryRepository {

    override suspend fun recordTrip(item: TripHistoryItem) = withContext(Dispatchers.IO) {
        db.myTDatabaseQueries.insertTrip(
            id = item.id,
            vin = item.vin,
            started_at_ms = item.startedAtMs,
            ended_at_ms = item.endedAtMs,
            distance_km = item.distanceKm.toDouble(),
            avg_speed_kmh = item.avgSpeedKmh?.toDouble(),
            max_speed_kmh = item.maxSpeedKmh?.toDouble(),
            start_soc = item.startSoc?.toDouble(),
            end_soc = item.endSoc?.toDouble(),
            polyline_encoded = item.polylineEncoded,
            efficiency_km_per_kwh = item.efficiencyKmPerKwh?.toDouble(),
        )
    }

    override suspend fun updateTripEnd(item: TripHistoryItem) = withContext(Dispatchers.IO) {
        db.myTDatabaseQueries.updateTripEnd(
            ended_at_ms = item.endedAtMs,
            distance_km = item.distanceKm.toDouble(),
            avg_speed_kmh = item.avgSpeedKmh?.toDouble(),
            max_speed_kmh = item.maxSpeedKmh?.toDouble(),
            end_soc = item.endSoc?.toDouble(),
            polyline_encoded = item.polylineEncoded,
            efficiency_km_per_kwh = item.efficiencyKmPerKwh?.toDouble(),
            id = item.id,
        )
    }

    override suspend fun recordCharge(item: ChargeHistoryItem) = withContext(Dispatchers.IO) {
        db.myTDatabaseQueries.insertChargeSession(
            id = item.id,
            vin = item.vin,
            started_at_ms = item.startedAtMs,
            ended_at_ms = item.endedAtMs,
            start_soc = item.startSoc.toDouble(),
            end_soc = item.endSoc?.toDouble(),
            energy_kwh = item.energyKwh?.toDouble(),
            peak_kw = item.peakKw?.toDouble(),
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun recordFleetEvent(category: FleetCallCategory, ok: Boolean, detail: String?) =
        withContext(Dispatchers.IO) {
            db.myTDatabaseQueries.insertFleetEvent(
                id = Uuid.random().toString(),
                at_ms = clock.now().toEpochMilliseconds(),
                category = category.name,
                ok = if (ok) 1L else 0L,
                detail = detail,
            )
        }

    override suspend fun saveVehicleSnapshot(vin: String, state: GaugeState) = withContext(Dispatchers.IO) {
        val payload = state.toPayload()
        db.myTDatabaseQueries.upsertVehicleSnapshot(
            vin = vin,
            payload_json = json.encodeToString(payload),
            updated_at_ms = payload.lastUpdated,
            from_cache = 0L,
        )
    }

    override suspend fun loadVehicleSnapshot(vin: String): GaugeState? = withContext(Dispatchers.IO) {
        db.myTDatabaseQueries.selectVehicleSnapshot(vin)
            .executeAsOneOrNull()
            ?.payload_json
            ?.let { runCatching { json.decodeFromString<VehicleSnapshotPayload>(it).toGaugeState() }.getOrNull() }
    }

    override suspend fun trips(filter: HistoryFilterState): List<TripHistoryItem> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = when (filter.period) {
                HistoryPeriodFilter.All -> db.myTDatabaseQueries.selectAllTrips().executeAsList()
                else -> db.myTDatabaseQueries.selectTripsSince(sinceMs(filter.period)).executeAsList()
            }
            rows.map { it.toTripItem() }.let { sortTrips(it, filter.sort) }
        }.getOrElse { emptyList() }
    }

    override suspend fun chargeSessions(filter: HistoryFilterState): List<ChargeHistoryItem> =
        withContext(Dispatchers.IO) {
            val rows = when (filter.period) {
                HistoryPeriodFilter.All -> db.myTDatabaseQueries.selectAllChargeSessions().executeAsList()
                else -> db.myTDatabaseQueries.selectChargeSince(sinceMs(filter.period)).executeAsList()
            }
            rows.map { it.toChargeItem() }.let { sortCharge(it, filter.sort) }
        }

    override suspend fun fleetEvents(filter: HistoryFilterState): List<FleetApiHistoryItem> =
        withContext(Dispatchers.IO) {
            val rows = when {
                filter.fleetCategory != null ->
                    db.myTDatabaseQueries.selectFleetEventsByCategory(filter.fleetCategory).executeAsList()
                filter.period == HistoryPeriodFilter.All ->
                    db.myTDatabaseQueries.selectAllFleetEvents().executeAsList()
                else ->
                    db.myTDatabaseQueries.selectFleetEventsSince(sinceMs(filter.period)).executeAsList()
            }
            rows.map { it.toFleetItem() }
                .filter { !filter.onlyFailures || !it.ok }
                .let { sortFleet(it, filter.sort) }
        }

    override suspend fun tripById(id: String): TripHistoryItem? = withContext(Dispatchers.IO) {
        db.myTDatabaseQueries.selectTripById(id).executeAsOneOrNull()?.toTripItem()
    }

    private fun sinceMs(period: HistoryPeriodFilter): Long {
        val now = clock.now().toEpochMilliseconds()
        return when (period) {
            HistoryPeriodFilter.Days7 -> now - 7L * 24 * 60 * 60 * 1000
            HistoryPeriodFilter.Days30 -> now - 30L * 24 * 60 * 60 * 1000
            HistoryPeriodFilter.All -> 0L
        }
    }
}

private fun sortTrips(items: List<TripHistoryItem>, sort: HistorySortOrder) = when (sort) {
    HistorySortOrder.Newest -> items.sortedByDescending { it.startedAtMs }
    HistorySortOrder.Oldest -> items.sortedBy { it.startedAtMs }
    HistorySortOrder.DistanceDesc -> items.sortedByDescending { it.distanceKm }
    else -> items.sortedByDescending { it.startedAtMs }
}

private fun sortCharge(items: List<ChargeHistoryItem>, sort: HistorySortOrder) = when (sort) {
    HistorySortOrder.EnergyDesc -> items.sortedByDescending { it.energyKwh ?: 0f }
    HistorySortOrder.Oldest -> items.sortedBy { it.startedAtMs }
    else -> items.sortedByDescending { it.startedAtMs }
}

private fun sortFleet(items: List<FleetApiHistoryItem>, sort: HistorySortOrder) = when (sort) {
    HistorySortOrder.Category -> items.sortedWith(compareBy({ it.category }, { -it.atMs }))
    HistorySortOrder.Oldest -> items.sortedBy { it.atMs }
    else -> items.sortedByDescending { it.atMs }
}

private fun GaugeState.toPayload() = VehicleSnapshotPayload(
    speedKmh = speedKmh,
    gear = gear.name,
    socPercent = socPercent,
    rangeKm = rangeKm,
    insideTempC = insideTempC,
    outsideTempC = outsideTempC,
    powerKw = powerKw,
    isSleeping = isSleeping,
    locked = locked,
    odometerKm = odometerKm,
    isCharging = charging?.isCharging == true,
    connection = connection.name,
    lastUpdated = lastUpdated,
    latitude = latitude,
    longitude = longitude,
    headingDegrees = headingDegrees,
    tireFlBar = tires?.frontLeftBar,
    tireFrBar = tires?.frontRightBar,
    tireRlBar = tires?.rearLeftBar,
    tireRrBar = tires?.rearRightBar,
)

private fun VehicleSnapshotPayload.toGaugeState(): GaugeState {
    val tirePressures = if (
        tireFlBar != null && tireFrBar != null && tireRlBar != null && tireRrBar != null
    ) {
        TirePressures(tireFlBar, tireFrBar, tireRlBar, tireRrBar)
    } else {
        null
    }
    return GaugeState(
        speedKmh = speedKmh,
        gear = runCatching { Gear.valueOf(gear) }.getOrDefault(Gear.PARK),
        socPercent = socPercent,
        rangeKm = rangeKm,
        insideTempC = insideTempC,
        outsideTempC = outsideTempC,
        powerKw = powerKw,
        tires = tirePressures,
        isSleeping = isSleeping,
        locked = locked,
        odometerKm = odometerKm,
        charging = ChargeInfo(isCharging = isCharging),
        connection = runCatching { ConnectionStatus.valueOf(connection) }.getOrDefault(ConnectionStatus.FleetConnected),
        lastUpdated = lastUpdated,
        latitude = latitude,
        longitude = longitude,
        headingDegrees = headingDegrees,
        locationSource = if (latitude != null && longitude != null) {
            TelemetrySource.Cache
        } else {
            TelemetrySource.None
        },
    )
}

private fun com.myt.data.local.Trip_record.toTripItem() = TripHistoryItem(
    id = id,
    vin = vin,
    startedAtMs = started_at_ms,
    endedAtMs = ended_at_ms,
    distanceKm = distance_km.toFloat(),
    avgSpeedKmh = avg_speed_kmh?.toFloat(),
    maxSpeedKmh = max_speed_kmh?.toFloat(),
    startSoc = start_soc?.toFloat(),
    endSoc = end_soc?.toFloat(),
    polylineEncoded = polyline_encoded,
    efficiencyKmPerKwh = efficiency_km_per_kwh?.toFloat(),
)

private fun com.myt.data.local.Charge_session.toChargeItem() = ChargeHistoryItem(
    id = id,
    vin = vin,
    startedAtMs = started_at_ms,
    endedAtMs = ended_at_ms,
    startSoc = start_soc.toFloat(),
    endSoc = end_soc?.toFloat(),
    energyKwh = energy_kwh?.toFloat(),
    peakKw = peak_kw?.toFloat(),
)

private fun com.myt.data.local.Fleet_api_event.toFleetItem() = FleetApiHistoryItem(
    id = id,
    atMs = at_ms,
    category = category,
    ok = ok == 1L,
    detail = detail,
)
