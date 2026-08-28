package com.myt.domain

import com.myt.domain.model.SpeedCamera
import com.myt.domain.model.CameraType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeedCamMatcherTest {
    private val vehicleLat = 37.4985
    private val vehicleLng = 127.0280

    @Test
    fun aheadCamera_sameDirection_isAccepted() {
        // Camera ~200m east, heading east (90°), road direction east
        val cam = camera(lat = vehicleLat, lng = vehicleLng + 0.0018, roadDir = 90f)
        assertTrue(
            SpeedCamMatcher.isAheadOnRoute(vehicleLat, vehicleLng, heading = 90f, cam, distanceM = 200.0),
        )
    }

    @Test
    fun oppositeDirectionCamera_isRejected() {
        // Camera east but road direction west (opposite lane flow)
        val cam = camera(lat = vehicleLat, lng = vehicleLng + 0.0018, roadDir = 270f)
        assertFalse(
            SpeedCamMatcher.isAheadOnRoute(vehicleLat, vehicleLng, heading = 90f, cam, distanceM = 200.0),
        )
    }

    @Test
    fun behindCamera_isRejected() {
        // Camera west while heading east
        val cam = camera(lat = vehicleLat, lng = vehicleLng - 0.0018, roadDir = 270f)
        assertFalse(
            SpeedCamMatcher.isAheadOnRoute(vehicleLat, vehicleLng, heading = 90f, cam, distanceM = 200.0),
        )
    }

    @Test
    fun noRoadDirection_usesForwardConeOnly() {
        val cam = camera(lat = vehicleLat, lng = vehicleLng + 0.0018, roadDir = null)
        assertTrue(
            SpeedCamMatcher.isAheadOnRoute(vehicleLat, vehicleLng, heading = 92f, cam, distanceM = 180.0),
        )
        assertFalse(
            SpeedCamMatcher.isAheadOnRoute(vehicleLat, vehicleLng, heading = 270f, cam, distanceM = 180.0),
        )
    }

    @Test
    fun underLimit_stillReturnsL1_inEngine() {
        val engine = SpeedCamEngine(com.myt.data.poi.MockPoiRepository())
        val level = engine.calculateAlertLevel(
            camera = camera(lat = vehicleLat, lng = vehicleLng + 0.0005, roadDir = 90f),
            distanceM = 250.0,
            speedKmh = 50f,
        )
        kotlin.test.assertEquals(com.myt.domain.model.AlertLevel.L1, level)
    }

    private fun camera(lat: Double, lng: Double, roadDir: Float?) = SpeedCamera(
        id = "test",
        latitude = lat,
        longitude = lng,
        speedLimitKmh = 80,
        roadDirection = roadDir,
        cameraType = CameraType.FIXED,
    )
}
