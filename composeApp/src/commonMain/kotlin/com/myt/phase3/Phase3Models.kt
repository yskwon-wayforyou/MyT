package com.myt.phase3

import kotlinx.serialization.Serializable

data class BatteryHealthPoint(
    val atMs: Long,
    /** Estimated usable capacity vs nominal (100 = new pack). */
    val capacityPct: Float,
    val sourceLabel: String,
)

data class BatteryHealthReport(
    val points: List<BatteryHealthPoint>,
    /** Negative = degradation (% per year). */
    val trendPctPerYear: Float?,
)

data class Co2SavingsSummary(
    val totalDistanceKm: Float,
    val co2SavedKg: Float,
    val equivalentTrees: Float,
)

enum class CarbonBadgeTier(val label: String, val minCo2SavedKg: Float) {
    Seedling("새싹", 0f),
    Commuter("출퇴근", 10f),
    Explorer("탐험가", 50f),
    Champion("챔피언", 200f),
    Hero("히어로", 500f),
}

data class CarbonBadgeState(
    val tier: CarbonBadgeTier,
    val co2SavedKg: Float,
    val nextTier: CarbonBadgeTier?,
    val progressToNext: Float,
)

@Serializable
data class HaDiscoveryPayload(
    val name: String,
    val state_topic: String,
    val json_attributes_topic: String? = null,
    val unit_of_measurement: String? = null,
    val device_class: String? = null,
    val unique_id: String,
    val device: HaDeviceInfo,
)

@Serializable
data class HaDeviceInfo(
    val identifiers: List<String>,
    val name: String,
    val manufacturer: String = "MyT",
    val model: String = "Tesla Gauge",
)

data class LiveCameraStatus(
    val available: Boolean,
    val message: String,
)
