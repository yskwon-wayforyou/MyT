package com.myt.domain.device

/**
 * Single GPS/fused location sample from the handset.
 * Only consumed when Bluetooth vehicle presence is true (see TelemetryMerger).
 */
data class DeviceFix(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val headingDegrees: Float?,
    val accuracyMeters: Float?,
    val timestampMs: Long,
)
