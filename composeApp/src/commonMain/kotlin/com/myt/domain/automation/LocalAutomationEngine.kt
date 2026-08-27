package com.myt.domain.automation

import com.myt.domain.control.ControlRequest
import com.myt.domain.control.VehicleCommand
import com.myt.domain.control.VehicleControlGateway
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.repository.SettingsRepository
import com.myt.phase2.PushNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Evaluates local automation rules: event triggers, wall-clock schedule, geofence.
 */
class LocalAutomationEngine(
    private val repository: AutomationRepository,
    private val pushNotifier: PushNotifier,
    private val scope: CoroutineScope,
    private val controlGateway: VehicleControlGateway? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val clock: Clock = Clock.System,
) {
    private var job: Job? = null
    private var scheduleJob: Job? = null
    private var prevCharging: Boolean? = null
    private var prevGear: Gear? = null
    private var prevInsideGeofence: MutableMap<String, Boolean> = mutableMapOf()
    private val firedScheduleKeys = mutableSetOf<String>()

    fun start(gaugeState: StateFlow<GaugeState>) {
        if (job != null) return
        job = scope.launch {
            gaugeState.collect { state ->
                evaluate(state)
            }
        }
        scheduleJob = scope.launch {
            while (isActive) {
                evaluateSchedule()
                delay(30_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        scheduleJob?.cancel()
        job = null
        scheduleJob = null
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
        evaluateGeofence(rules, state)
        prevCharging = charging
        prevGear = state.gear
    }

    private suspend fun evaluateSchedule() {
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val rules = repository.listRules().filter {
            it.enabled && it.kind == AutomationTriggerKind.Schedule
        }
        for (rule in rules) {
            val hour = rule.scheduleHour ?: continue
            val minute = rule.scheduleMinute ?: 0
            if (now.hour != hour || now.minute != minute) continue
            val key = "${rule.id}-${now.date}-$hour-$minute"
            if (key in firedScheduleKeys) continue
            firedScheduleKeys.add(key)
            if (firedScheduleKeys.size > 64) {
                firedScheduleKeys.clear()
                firedScheduleKeys.add(key)
            }
            dispatchAction(rule, "스케줄 ${rule.name}", "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 실행")
        }
    }

    private suspend fun evaluateGeofence(rules: List<AutomationRule>, state: GaugeState) {
        val lat = state.latitude ?: return
        val lng = state.longitude ?: return
        rules.filter {
            it.kind == AutomationTriggerKind.GeofenceEnter ||
                it.kind == AutomationTriggerKind.GeofenceExit
        }.forEach { rule ->
            val gLat = rule.geofenceLat ?: return@forEach
            val gLng = rule.geofenceLng ?: return@forEach
            val radius = (rule.geofenceRadiusM ?: 200).toDouble()
            val dist = GeofenceMath.distanceMeters(lat, lng, gLat, gLng)
            val inside = dist <= radius
            val wasInside = prevInsideGeofence[rule.id]
            prevInsideGeofence[rule.id] = inside
            if (wasInside == null) return@forEach
            when {
                rule.kind == AutomationTriggerKind.GeofenceEnter && !wasInside && inside ->
                    dispatchAction(rule, "지오펜스 진입", rule.name)
                rule.kind == AutomationTriggerKind.GeofenceExit && wasInside && !inside ->
                    dispatchAction(rule, "지오펜스 이탈", rule.name)
            }
        }
    }

    private suspend fun fire(rules: List<AutomationRule>, trigger: String, title: String, body: String) {
        rules.filter { it.kind == AutomationTriggerKind.Event && it.trigger == trigger }.forEach { rule ->
            dispatchAction(rule, title, body)
        }
    }

    private suspend fun dispatchAction(rule: AutomationRule, title: String, body: String) {
        when (rule.action) {
            "push", "save_location" -> pushNotifier.notify(title, body)
            "climate_on" -> {
                pushNotifier.notify(title, body)
                val vin = settingsRepository?.getVin()
                if (!vin.isNullOrBlank() && controlGateway != null) {
                    controlGateway.execute(ControlRequest(VehicleCommand.ClimateOn, vin))
                }
            }
            "sentry_on" -> {
                pushNotifier.notify(title, body)
                val vin = settingsRepository?.getVin()
                if (!vin.isNullOrBlank() && controlGateway != null) {
                    controlGateway.execute(ControlRequest(VehicleCommand.SentryOn, vin))
                }
            }
            "defrost" -> {
                pushNotifier.notify(title, "$body (해동 예약)")
                val vin = settingsRepository?.getVin()
                if (!vin.isNullOrBlank() && controlGateway != null) {
                    controlGateway.execute(ControlRequest(VehicleCommand.ClimateOn, vin))
                }
            }
            else -> pushNotifier.notify(title, body)
        }
    }
}
