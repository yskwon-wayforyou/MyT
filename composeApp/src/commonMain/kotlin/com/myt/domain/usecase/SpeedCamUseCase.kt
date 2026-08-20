package com.myt.domain.usecase

import com.myt.domain.SpeedCamEngine
import com.myt.domain.model.GaugeState
import com.myt.domain.model.SpeedCamAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeedCamUseCase(
    private val engine: SpeedCamEngine,
) {
    private val _alert = MutableStateFlow<SpeedCamAlert?>(null)
    val alert: StateFlow<SpeedCamAlert?> = _alert.asStateFlow()

    fun evaluateFromGaugeState(state: GaugeState) {
        val lat = state.latitude
        val lng = state.longitude
        val heading = state.headingDegrees
        if (lat == null || lng == null || heading == null) {
            _alert.value = null
            return
        }
        _alert.value = engine.evaluate(lat, lng, heading, state.speedKmh)
    }

    fun clearAlert() {
        _alert.value = null
        engine.resetSectionTracking()
    }
}
