package com.myt.domain

data class VehicleConfig(
    val vin: String,
    val displayName: String = "Model 3",
    /** Hybrid default: longer when Device GPS covers speed (BT ON). */
    val pollingIntervalDrivingMs: Long = 105_000L,
    val pollingIntervalDrivingNoDeviceMs: Long = 75_000L,
    val pollingIntervalParkedMs: Long = 300_000L,
    val pollingIntervalChargingMs: Long = 150_000L,
    /** Near charge limit — refresh faster so Complete is not stuck behind 150s cache. */
    val pollingIntervalChargingNearLimitMs: Long = 45_000L,
    val pollingIntervalSleepMs: Long = 900_000L,
)
