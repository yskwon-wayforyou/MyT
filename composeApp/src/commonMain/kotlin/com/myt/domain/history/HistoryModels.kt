package com.myt.domain.history

import kotlinx.serialization.Serializable

enum class HistoryTab {
    Driving,
    Charging,
    FleetApi,
}

enum class HistoryPeriodFilter {
    Days7,
    Days30,
    All,
}

enum class HistorySortOrder {
    Newest,
    Oldest,
    DistanceDesc,
    EnergyDesc,
    Category,
}

data class TripHistoryItem(
    val id: String,
    val vin: String,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val distanceKm: Float,
    val avgSpeedKmh: Float?,
    val maxSpeedKmh: Float?,
    val startSoc: Float?,
    val endSoc: Float?,
    val polylineEncoded: String? = null,
    /** Approx: km per kWh estimated from SOC delta. */
    val efficiencyKmPerKwh: Float? = null,
)

data class ChargeHistoryItem(
    val id: String,
    val vin: String,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val startSoc: Float,
    val endSoc: Float?,
    val energyKwh: Float?,
    val peakKw: Float?,
)

data class FleetApiHistoryItem(
    val id: String,
    val atMs: Long,
    val category: String,
    val ok: Boolean,
    val detail: String?,
)

data class HistoryFilterState(
    val tab: HistoryTab = HistoryTab.Driving,
    val period: HistoryPeriodFilter = HistoryPeriodFilter.Days30,
    val sort: HistorySortOrder = HistorySortOrder.Newest,
    val fleetCategory: String? = null,
    val onlyFailures: Boolean = false,
)

@Serializable
data class VehicleSnapshotPayload(
    val speedKmh: Float = 0f,
    val gear: String = "PARK",
    val socPercent: Float = 0f,
    val rangeKm: Float = 0f,
    val insideTempC: Float? = null,
    val outsideTempC: Float? = null,
    val powerKw: Float? = null,
    val isSleeping: Boolean = false,
    val locked: Boolean? = null,
    val odometerKm: Float? = null,
    val isCharging: Boolean = false,
    val chargingState: String? = null,
    val chargeLimitPercent: Int? = null,
    val chargeRateKw: Float? = null,
    val connection: String = "Disconnected",
    val lastUpdated: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val headingDegrees: Float? = null,
    val tireFlBar: Float? = null,
    val tireFrBar: Float? = null,
    val tireRlBar: Float? = null,
    val tireRrBar: Float? = null,
)

data class DailyAggregate(
    val dayLabel: String,
    val value: Float,
)
