package com.myt.domain.simulation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrivingSimulationRunnerTest {
    @Test
    fun approachSpeedCam_increasesSpeed() {
        val scenario = DrivingSimulationScenarios.byId(DrivingSimulationId.ApproachSpeedCamL3)
        val runner = DrivingSimulationRunner(scenario)
        val speeds = (0 until 20).map { runner.tick().speedKmh }
        assertTrue(speeds.last() >= 90f, "expected high speed near camera, got ${speeds.last()}")
    }

    @Test
    fun chargingParked_nearGwanggyoJungangStation() {
        val scenario = DrivingSimulationScenarios.byId(DrivingSimulationId.ChargingParkedSuwon)
        val state = DrivingSimulationRunner(scenario).tick()
        assertEquals(true, state.charging?.isCharging)
        assertTrue(state.latitude != null && state.longitude!! > 127.10)
        assertTrue(state.latitude!! > 37.33)
        assertEquals("충전 주차 (광교중앙역)", state.simulationLabel)
    }

    @Test
    fun loopsWhenConfigured() {
        val scenario = DrivingSimulationScenarios.byId(DrivingSimulationId.ChargingParkedSuwon)
        val runner = DrivingSimulationRunner(scenario)
        repeat(scenario.frames.size + 5) { runner.tick() }
        // Should not throw; loop keeps emitting
        assertTrue(runner.tick().isSimulated)
    }
}
