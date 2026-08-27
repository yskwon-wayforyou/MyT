package com.myt.domain.usecase

import com.myt.domain.SpeedCamEngine
import com.myt.domain.model.GaugeState
import com.myt.domain.model.SpeedCamAlert
import com.myt.domain.model.AlertLevel
import com.myt.platform.AudioAlertPlatform
import com.myt.platform.HapticPlatform
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeedCamUseCase(
    private val engine: SpeedCamEngine,
    private val audio: AudioAlertPlatform,
    private val haptic: HapticPlatform,
    private val clock: Clock = Clock.System,
) {
    private val _alert = MutableStateFlow<SpeedCamAlert?>(null)
    val alert: StateFlow<SpeedCamAlert?> = _alert.asStateFlow()

    private var lastBeepAtMs: Long = 0L
    private var lastLevel: AlertLevel? = null
    private val cooldownMs: Long = 8_000L

    fun evaluateFromGaugeState(state: GaugeState) {
        // BT gate: simulation bypasses (isSimulated + bluetoothPresent in frames).
        if (!state.bluetoothPresent && !state.isSimulated) {
            _alert.value = null
            return
        }
        val lat = state.latitude
        val lng = state.longitude
        val heading = state.headingDegrees
        if (lat == null || lng == null || heading == null) {
            _alert.value = null
            return
        }
        val newAlert = engine.evaluate(lat, lng, heading, state.speedKmh)
        val oldAlert = _alert.value
        _alert.value = newAlert

        // 경보가 새로 뜨거나 레벨이 바뀔 때만(쿨다운 적용) 오디오/햅틱을 울립니다.
        val shouldTrigger = newAlert != null && (
            oldAlert == null ||
                oldAlert.level != newAlert.level ||
                (clock.now().toEpochMilliseconds() - lastBeepAtMs) >= cooldownMs
            )
        if (shouldTrigger) {
            val nowMs = clock.now().toEpochMilliseconds()
            if (nowMs - lastBeepAtMs >= cooldownMs) {
                if (!state.isSimulated && !audio.areAlertsSuppressed()) {
                    trigger(newAlert)
                }
                lastBeepAtMs = nowMs
            }
            lastLevel = newAlert.level
        }
    }

    fun clearAlert() {
        _alert.value = null
        engine.resetSectionTracking()
    }

    private fun trigger(alert: SpeedCamAlert) {
        if (!audio.areAlertsSuppressed()) {
            val (freq, duration, count) = when (alert.level) {
                AlertLevel.L1 -> Triple(980, 180, 1)
                AlertLevel.L2 -> Triple(1140, 220, 2)
                AlertLevel.L3 -> Triple(1280, 260, 3)
                AlertLevel.SECTION -> Triple(740, 320, 2)
            }
            audio.playBeep(freq, duration, count)
        }
        if (haptic.areHapticsSuppressed()) return
        val hapticMs = when (alert.level) {
            AlertLevel.L1 -> 80L
            AlertLevel.L2 -> 140L
            AlertLevel.L3 -> 220L
            AlertLevel.SECTION -> 260L
        }
        haptic.vibrate(hapticMs)
    }
}
