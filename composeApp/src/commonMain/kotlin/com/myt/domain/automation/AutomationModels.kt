package com.myt.domain.automation

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
enum class AutomationTriggerKind {
    Event,
    Schedule,
    GeofenceEnter,
    GeofenceExit,
}

@Serializable
data class AutomationRule(
    val id: String,
    val name: String,
    val trigger: String,
    val action: String,
    val enabled: Boolean = true,
    val kind: AutomationTriggerKind = AutomationTriggerKind.Event,
    /** Local hour 0–23 when [kind] is Schedule. */
    val scheduleHour: Int? = null,
    val scheduleMinute: Int? = null,
    val geofenceLat: Double? = null,
    val geofenceLng: Double? = null,
    /** Meters; used with GeofenceEnter/Exit. */
    val geofenceRadiusM: Int? = null,
)

interface AutomationRepository {
    suspend fun listRules(): List<AutomationRule>
    suspend fun saveRule(rule: AutomationRule): Result<AutomationRule>
    suspend fun deleteRule(id: String): Result<Unit>
}

/** Persisted local automation rules (W3). */
class SettingsAutomationRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : AutomationRepository {
    override suspend fun listRules(): List<AutomationRule> {
        val raw = settings.getStringOrNull(KEY) ?: return defaultRules().also { persist(it) }
        return runCatching {
            json.decodeFromString(ListSerializer(AutomationRule.serializer()), raw)
        }.getOrElse { defaultRules().also { persist(it) } }
    }

    override suspend fun saveRule(rule: AutomationRule): Result<AutomationRule> = runCatching {
        val next = listRules().toMutableList()
        val idx = next.indexOfFirst { it.id == rule.id }
        if (idx >= 0) next[idx] = rule else next += rule
        persist(next)
        rule
    }

    override suspend fun deleteRule(id: String): Result<Unit> = runCatching {
        persist(listRules().filterNot { it.id == id })
    }

    private fun persist(items: List<AutomationRule>) {
        settings.putString(KEY, json.encodeToString(ListSerializer(AutomationRule.serializer()), items))
    }

    private fun defaultRules(): List<AutomationRule> = listOf(
        AutomationRule("auto-1", "충전 완료 알림", "charge_complete", "push"),
        AutomationRule("auto-2", "저온 프리컨디션", "outside_temp_below_5", "climate_on"),
        AutomationRule(
            id = "auto-3",
            name = "출근 전 해동",
            trigger = "schedule",
            action = "climate_on",
            kind = AutomationTriggerKind.Schedule,
            scheduleHour = 7,
            scheduleMinute = 0,
            enabled = false,
        ),
        AutomationRule(
            id = "auto-4",
            name = "Sentry 야간",
            trigger = "schedule",
            action = "sentry_on",
            kind = AutomationTriggerKind.Schedule,
            scheduleHour = 22,
            scheduleMinute = 0,
            enabled = false,
        ),
        AutomationRule("auto-5", "주차 위치 저장", "gear_park", "save_location"),
        AutomationRule(
            id = "auto-geo-home",
            name = "집 도착 알림",
            trigger = "geofence",
            action = "push",
            kind = AutomationTriggerKind.GeofenceEnter,
            // 수원 영통 기본 샘플 좌표 (광교중앙역 인근)
            geofenceLat = 37.2886,
            geofenceLng = 127.0515,
            geofenceRadiusM = 300,
            enabled = false,
        ),
    )

    companion object {
        const val KEY = "automation_rules_v1"
    }
}

object GeofenceMath {
    /** Haversine distance in meters. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = toRad(lat2 - lat1)
        val dLng = toRad(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(toRad(lat1)) * cos(toRad(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun toRad(deg: Double): Double = deg * kotlin.math.PI / 180.0
}

/** Compatibility alias used by older DI/tests. */
typealias LocalAutomationRepository = SettingsAutomationRepository
