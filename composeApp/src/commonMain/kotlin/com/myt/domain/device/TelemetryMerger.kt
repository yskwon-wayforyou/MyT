package com.myt.domain.device

import com.myt.domain.model.GaugeState
import com.myt.domain.model.TelemetrySource
import kotlinx.datetime.Clock

object TelemetryMerger {
    const val FRESH_MS = 3_000L
    const val DEGRADED_MS = 5_000L
    private const val SPEED_EMA_ALPHA = 0.35f

    fun merge(
        fleet: GaugeState,
        deviceFix: DeviceFix?,
        bluetoothPresent: Boolean,
        preferDeviceSpeed: Boolean,
        previousSpeedKmh: Float = fleet.speedKmh,
        clock: Clock = Clock.System,
    ): GaugeState {
        val withBt = fleet.copy(bluetoothPresent = bluetoothPresent)
        val useDeviceSpeed = bluetoothPresent && preferDeviceSpeed && deviceFix != null
        val fleetHasLocation = fleet.latitude != null && fleet.longitude != null

        // Phone GPS may fill map coords when Fleet has none (e.g. charging, BT off).
        val locationFill = if (!fleetHasLocation && deviceFix != null) {
            withBt.copy(
                latitude = deviceFix.latitude,
                longitude = deviceFix.longitude,
                headingDegrees = deviceFix.headingDegrees ?: fleet.headingDegrees,
                locationSource = TelemetrySource.Device,
            )
        } else {
            withBt.copy(
                locationSource = when {
                    fleetHasLocation -> TelemetrySource.Fleet
                    else -> TelemetrySource.None
                },
            )
        }

        if (!useDeviceSpeed) {
            return locationFill.copy(
                speedSource = if (fleet.speedKmh > 0f || fleet.lastUpdated > 0L) {
                    TelemetrySource.Fleet
                } else {
                    TelemetrySource.None
                },
            )
        }

        val age = clock.now().toEpochMilliseconds() - deviceFix!!.timestampMs
        val source = when {
            age <= FRESH_MS -> TelemetrySource.Device
            age <= DEGRADED_MS -> TelemetrySource.Degraded
            else -> TelemetrySource.Degraded
        }
        if (age > DEGRADED_MS * 3) {
            return locationFill.copy(speedSource = TelemetrySource.Fleet)
        }

        val smoothed = previousSpeedKmh * (1f - SPEED_EMA_ALPHA) + deviceFix.speedKmh * SPEED_EMA_ALPHA
        return locationFill.copy(
            speedKmh = smoothed.coerceAtLeast(0f),
            latitude = deviceFix.latitude,
            longitude = deviceFix.longitude,
            headingDegrees = deviceFix.headingDegrees ?: fleet.headingDegrees,
            speedSource = source,
            locationSource = source,
            lastUpdated = maxOf(fleet.lastUpdated, deviceFix.timestampMs),
        )
    }
}
