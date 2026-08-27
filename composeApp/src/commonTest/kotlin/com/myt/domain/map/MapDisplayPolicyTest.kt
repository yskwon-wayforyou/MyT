package com.myt.domain.map

import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapDisplayPolicyTest {
    @Test
    fun parked_noSnap() {
        assertFalse(
            MapDisplayPolicy.shouldApplyRoadSnap(
                GaugeState(gear = Gear.PARK, speedKmh = 0f),
            ),
        )
    }

    @Test
    fun drivingFast_snaps() {
        assertTrue(
            MapDisplayPolicy.shouldApplyRoadSnap(
                GaugeState(gear = Gear.DRIVE, speedKmh = 40f),
            ),
        )
    }

    @Test
    fun indoorCrawl_noSnap() {
        assertFalse(
            MapDisplayPolicy.shouldApplyRoadSnap(
                GaugeState(gear = Gear.DRIVE, speedKmh = 3f),
            ),
        )
    }

    @Test
    fun simulation_noSnap() {
        assertFalse(
            MapDisplayPolicy.shouldApplyRoadSnap(
                GaugeState(gear = Gear.DRIVE, speedKmh = 80f, isSimulated = true),
            ),
        )
    }
}
