package com.myt.domain.simulation

import com.myt.domain.model.GaugeState
import kotlinx.datetime.Clock

class DrivingSimulationRunner(
    private val scenario: DrivingSimulationScenario,
) {
    private var frameIndex: Int = 0

    fun reset() {
        frameIndex = 0
    }

    fun tick(): GaugeState {
        if (scenario.frames.isEmpty()) return GaugeState(isSimulated = true)
        val frame = scenario.frames[frameIndex]
        frameIndex = if (frameIndex + 1 >= scenario.frames.size) {
            if (scenario.loop) 0 else scenario.frames.lastIndex
        } else {
            frameIndex + 1
        }
        return frame.copy(
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
            simulationLabel = scenario.name,
        )
    }
}
