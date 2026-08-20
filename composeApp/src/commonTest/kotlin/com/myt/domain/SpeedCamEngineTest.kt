package com.myt.domain

import com.myt.domain.model.AlertLevel
import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamera
import com.myt.data.poi.MockPoiRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeedCamEngineTest {
    private val engine = SpeedCamEngine(MockPoiRepository())

    @Test
    fun calculateAlertLevel_returnsNullBeyond500m() {
        val camera = demoCamera()
        assertNull(engine.calculateAlertLevel(camera, 501.0, 90f))
    }

    @Test
    fun calculateAlertLevel_returnsL1Between300And500m() {
        val camera = demoCamera()
        assertEquals(AlertLevel.L1, engine.calculateAlertLevel(camera, 400.0, 90f))
    }

    @Test
    fun calculateAlertLevel_returnsL2Between100And300m() {
        val camera = demoCamera()
        assertEquals(AlertLevel.L2, engine.calculateAlertLevel(camera, 200.0, 90f))
    }

    @Test
    fun calculateAlertLevel_returnsL3WhenSpeedingUnder100m() {
        val camera = demoCamera(limit = 80)
        assertEquals(AlertLevel.L3, engine.calculateAlertLevel(camera, 50.0, 95f))
    }

    @Test
    fun calculateAlertLevel_returnsL2WhenNotSpeedingUnder100m() {
        val camera = demoCamera(limit = 80)
        assertEquals(AlertLevel.L2, engine.calculateAlertLevel(camera, 50.0, 70f))
    }

    @Test
    fun filterByDirection_keepsCamerasWithinTolerance() {
        val cameras = listOf(
            demoCamera(direction = 90f),
            demoCamera(id = "other", direction = 270f),
        )
        val filtered = engine.filterByDirection(cameras, heading = 95f)
        assertEquals(1, filtered.size)
        assertEquals("cam-test", filtered.first().id)
    }

    private fun demoCamera(
        id: String = "cam-test",
        limit: Int = 80,
        direction: Float = 90f,
    ) = SpeedCamera(
        id = id,
        latitude = 37.4985,
        longitude = 127.0280,
        speedLimitKmh = limit,
        roadDirection = direction,
        cameraType = CameraType.FIXED,
    )
}
