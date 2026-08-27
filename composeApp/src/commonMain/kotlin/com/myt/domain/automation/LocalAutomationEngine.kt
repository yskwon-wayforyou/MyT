package com.myt.domain.automation

import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.phase2.PushNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Evaluates local automation rules against gauge updates (M32 demo engine).
 */
class LocalAutomationEngine(
    private val repository: AutomationRepository,
    private val pushNotifier: PushNotifier,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var prevCharging: Boolean? = null
    private var prevGear: Gear? = null

    fun start(gaugeState: StateFlow<GaugeState>) {
        if (job != null) return
        job = scope.launch {
            gaugeState.collect { state ->
                evaluate(state)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun evaluate(state: GaugeState) {
        val rules = repository.listRules().filter { it.enabled }
        val charging = state.charging?.isCharging == true
        if (prevCharging == true && !charging) {
            fire(rules, "charge_complete", "충전 완료", "충전 세션이 종료되었습니다")
        }
        if (prevGear != Gear.PARK && state.gear == Gear.PARK) {
            fire(rules, "gear_park", "주차 위치", "주차 전환 — 위치 저장 데모")
        }
        if (state.outsideTempC != null && state.outsideTempC < 5f && state.climateOn != true) {
            fire(rules, "outside_temp_below_5", "저온 프리컨디션", "외기 ${state.outsideTempC.toInt()}°C — 공조 권장")
        }
        prevCharging = charging
        prevGear = state.gear
    }

    private suspend fun fire(rules: List<AutomationRule>, trigger: String, title: String, body: String) {
        if (rules.none { it.trigger == trigger }) return
        pushNotifier.notify(title, body)
    }
}
