package com.myt.domain.device

import com.myt.domain.model.GaugeState
import com.myt.domain.model.TelemetrySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelemetryMergerTest {
    @Test
    fun btOff_ignoresDeviceSpeed_keepsFleetLocation() {
        val fleet = GaugeState(speedKmh = 40f, latitude = 1.0, longitude = 2.0)
        val fix = DeviceFix(37.0, 127.0, 88f, 90f, 5f, System.currentTimeMillis())
        val merged = TelemetryMerger.merge(
            fleet = fleet,
            deviceFix = fix,
            bluetoothPresent = false,
            preferDeviceSpeed = true,
        )
        assertEquals(40f, merged.speedKmh)
        assertEquals(TelemetrySource.Fleet, merged.speedSource)
        assertEquals(1.0, merged.latitude)
        assertEquals(TelemetrySource.Fleet, merged.locationSource)
    }

    @Test
    fun btOff_fillsLocationFromDeviceWhenFleetMissing() {
        val fleet = GaugeState(speedKmh = 0f)
        val fix = DeviceFix(37.0, 127.0, 0f, 90f, 5f, System.currentTimeMillis())
        val merged = TelemetryMerger.merge(
            fleet = fleet,
            deviceFix = fix,
            bluetoothPresent = false,
            preferDeviceSpeed = true,
        )
        assertEquals(37.0, merged.latitude)
        assertEquals(127.0, merged.longitude)
        assertEquals(TelemetrySource.Device, merged.locationSource)
        assertEquals(0f, merged.speedKmh)
    }

    @Test
    fun btOn_appliesDeviceSpeed() {
        val fleet = GaugeState(speedKmh = 40f, latitude = 1.0, longitude = 2.0)
        val now = System.currentTimeMillis()
        val fix = DeviceFix(37.0, 127.0, 80f, 90f, 5f, now)
        val merged = TelemetryMerger.merge(
            fleet = fleet,
            deviceFix = fix,
            bluetoothPresent = true,
            preferDeviceSpeed = true,
            previousSpeedKmh = 80f,
        )
        assertTrue(merged.speedKmh > 70f)
        assertEquals(TelemetrySource.Device, merged.speedSource)
        assertEquals(37.0, merged.latitude)
    }
}
