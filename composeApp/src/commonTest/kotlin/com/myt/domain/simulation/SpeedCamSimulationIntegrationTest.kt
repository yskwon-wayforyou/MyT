package com.myt.domain.simulation

import com.myt.domain.SpeedCamEngine
import com.myt.domain.model.AlertLevel
import com.myt.domain.model.SpeedCamera
import com.myt.domain.model.CameraType
import com.myt.domain.repository.PoiRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedCamSimulationIntegrationTest {
    private val camera = SpeedCamera(
        id = "cam-su-003",
        latitude = 37.2851,
        longitude = 127.0532,
        speedLimitKmh = 60,
        roadName = "광교중앙로",
        roadDirection = 180f,
        cameraType = CameraType.FIXED,
    )

    private val poi = object : PoiRepository {
        override fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int) =
            if (kotlin.math.abs(lat - camera.latitude) < 0.01) listOf(camera) else emptyList()
    }

    @Test
    fun simulatedApproach_triggersL3() {
        val engine = SpeedCamEngine(poi)
        val scenario = DrivingSimulationScenarios.byId(DrivingSimulationId.ApproachSpeedCamL3)
        val runner = DrivingSimulationRunner(scenario)
        var l3: AlertLevel? = null
        repeat(scenario.frames.size) {
            val state = runner.tick()
            val alert = engine.evaluate(
                state.latitude!!,
                state.longitude!!,
                state.headingDegrees!!,
                state.speedKmh,
            )
            if (alert?.level == AlertLevel.L3) l3 = AlertLevel.L3
        }
        assertEquals(AlertLevel.L3, l3)
    }
}
