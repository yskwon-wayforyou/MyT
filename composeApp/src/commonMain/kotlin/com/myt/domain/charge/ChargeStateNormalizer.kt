package com.myt.domain.charge

import com.myt.domain.model.ChargeInfo

/**
 * Normalizes Fleet `charging_state` into UI/session flags.
 * Tesla app often shows "complete" while a stale MyT cache still has `Charging` + old SOC.
 */
object ChargeStateNormalizer {
    private val activeStates = setOf("charging", "starting")
    private val completeStates = setOf("complete", "completed", "done")
    private val idleStates = setOf(
        "stopped",
        "disconnected",
        "nopower",
        "no_power",
        "idle",
        "unknown",
    )

    fun isActivelyCharging(
        chargingState: String?,
        batteryLevel: Int?,
        chargeLimitSoc: Int?,
        chargerPowerKw: Double?,
    ): Boolean {
        val lower = chargingState?.trim()?.lowercase()?.replace(" ", "").orEmpty()
        if (lower.isEmpty()) return false
        if (lower in completeStates || lower in idleStates) return false
        if (lower !in activeStates) return false
        val power = chargerPowerKw ?: 0.0
        val level = batteryLevel
        val limit = chargeLimitSoc
        if (level != null && limit != null && level >= limit && power <= 0.1) {
            return false
        }
        return true
    }

    fun toChargeInfo(
        chargingState: String?,
        batteryLevel: Int?,
        chargeLimitSoc: Int?,
        chargerPowerKw: Double?,
        chargeRate: Double?,
        timeToFullHours: Double?,
    ): ChargeInfo {
        val power = chargerPowerKw ?: chargeRate
        val active = isActivelyCharging(chargingState, batteryLevel, chargeLimitSoc, power)
        return ChargeInfo(
            isCharging = active,
            chargeRateKw = power?.toFloat(),
            timeToFullMinutes = timeToFullHours?.let { (it * 60).toInt() },
            chargeLimitPercent = chargeLimitSoc,
            chargingState = chargingState,
        )
    }

    fun uiChargeLabel(socPercent: Float, charge: ChargeInfo?): String {
        val soc = socPercent.toInt()
        val state = charge?.chargingState.orEmpty()
        val lower = state.lowercase()
        return when {
            charge?.isCharging == true -> "충전 중 $soc%"
            lower.contains("complete") -> "충전 완료 $soc%"
            charge?.chargeLimitPercent != null &&
                soc >= charge.chargeLimitPercent!! -> "한도 도달 $soc%"
            else -> "배터리 $soc%"
        }
    }
}
