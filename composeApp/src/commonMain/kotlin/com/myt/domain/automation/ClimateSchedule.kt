package com.myt.domain.automation

import kotlinx.serialization.Serializable

@Serializable
enum class ClimateRepeat {
    Once,
    Daily,
    Weekdays,
}

/**
 * FR-V06a — fine-grained climate precondition schedule (Plus at W9; Free-included until then).
 */
@Serializable
data class ClimateSchedule(
    val id: String,
    val name: String,
    val hour: Int,
    val minute: Int,
    val targetTempC: Float = 21f,
    val driverSeatHeat: Int = 0,
    val passengerSeatHeat: Int = 0,
    val steeringHeat: Boolean = false,
    val defrost: Boolean = false,
    val repeat: ClimateRepeat = ClimateRepeat.Weekdays,
    val enabled: Boolean = true,
    /** Last fired calendar key `yyyy-MM-dd` to avoid duplicate runs in the same minute window. */
    val lastFiredKey: String? = null,
)

object ClimateScheduleMatcher {
    fun dayKey(year: Int, month: Int, day: Int): String =
        "${year.toString().padStart(4, '0')}-" +
            "${month.toString().padStart(2, '0')}-" +
            day.toString().padStart(2, '0')

    /**
     * @param dayOfWeek ISO-8601: Monday=1 … Sunday=7
     */
    fun shouldFire(
        schedule: ClimateSchedule,
        hour: Int,
        minute: Int,
        dayOfWeek: Int,
        dayKey: String,
    ): Boolean {
        if (!schedule.enabled) return false
        if (schedule.hour != hour || schedule.minute != minute) return false
        if (schedule.lastFiredKey == dayKey) return false
        return when (schedule.repeat) {
            ClimateRepeat.Once -> schedule.lastFiredKey == null
            ClimateRepeat.Daily -> true
            ClimateRepeat.Weekdays -> dayOfWeek in 1..5
        }
    }

    fun summarize(schedule: ClimateSchedule): String = buildString {
        append(schedule.hour.toString().padStart(2, '0'))
        append(':')
        append(schedule.minute.toString().padStart(2, '0'))
        append(" · ${schedule.targetTempC.toInt()}°C")
        if (schedule.defrost) append(" · 해동")
        if (schedule.steeringHeat) append(" · 스티어링")
        if (schedule.driverSeatHeat > 0 || schedule.passengerSeatHeat > 0) {
            append(" · 열선 D${schedule.driverSeatHeat}/P${schedule.passengerSeatHeat}")
        }
        append(
            when (schedule.repeat) {
                ClimateRepeat.Once -> " · 1회"
                ClimateRepeat.Daily -> " · 매일"
                ClimateRepeat.Weekdays -> " · 주중"
            },
        )
    }
}
