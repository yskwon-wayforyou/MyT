package com.myt.domain

data class VehicleConfig(
    val vin: String,
    val displayName: String = "Model 3",
    val pollingIntervalDrivingMs: Long = 2_000L,
    val pollingIntervalParkedMs: Long = 30_000L,
    val pollingIntervalChargingMs: Long = 5_000L,
    val pollingIntervalSleepMs: Long = 60_000L,
)
