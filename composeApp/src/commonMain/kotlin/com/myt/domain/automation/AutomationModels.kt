package com.myt.domain.automation

import kotlinx.serialization.Serializable

@Serializable
data class AutomationRule(
    val id: String,
    val name: String,
    val trigger: String,
    val action: String,
    val enabled: Boolean = true,
)

interface AutomationRepository {
    suspend fun listRules(): List<AutomationRule>
    suspend fun saveRule(rule: AutomationRule): Result<AutomationRule>
}

/** In-memory defaults matching backend demo (M32). */
class LocalAutomationRepository : AutomationRepository {
    private val rules = mutableListOf(
        AutomationRule("auto-1", "충전 완료 알림", "charge_complete", "push"),
        AutomationRule("auto-2", "저온 프리컨디션", "outside_temp_below_5", "climate_on"),
        AutomationRule("auto-3", "출발 전 해동", "weekday_07_00", "defrost"),
        AutomationRule("auto-4", "Sentry 야간", "time_22_00", "sentry_on"),
        AutomationRule("auto-5", "주차 위치 저장", "gear_park", "save_location"),
    )

    override suspend fun listRules(): List<AutomationRule> = rules.toList()

    override suspend fun saveRule(rule: AutomationRule): Result<AutomationRule> {
        val idx = rules.indexOfFirst { it.id == rule.id }
        if (idx >= 0) rules[idx] = rule else rules += rule
        return Result.success(rule)
    }
}
