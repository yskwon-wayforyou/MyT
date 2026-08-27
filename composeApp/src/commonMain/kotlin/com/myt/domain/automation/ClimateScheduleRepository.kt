package com.myt.domain.automation

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface ClimateScheduleRepository {
    suspend fun list(): List<ClimateSchedule>
    suspend fun save(schedule: ClimateSchedule): Result<ClimateSchedule>
    suspend fun delete(id: String): Result<Unit>
}

class SettingsClimateScheduleRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : ClimateScheduleRepository {
    override suspend fun list(): List<ClimateSchedule> {
        val raw = settings.getStringOrNull(KEY) ?: return defaultSchedules()
        return runCatching {
            json.decodeFromString(ListSerializer(ClimateSchedule.serializer()), raw)
        }.getOrElse { defaultSchedules() }
    }

    override suspend fun save(schedule: ClimateSchedule): Result<ClimateSchedule> = runCatching {
        val next = list().toMutableList()
        val idx = next.indexOfFirst { it.id == schedule.id }
        if (idx >= 0) next[idx] = schedule else next += schedule
        persist(next)
        schedule
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        persist(list().filterNot { it.id == id })
    }

    private fun persist(items: List<ClimateSchedule>) {
        settings.putString(
            KEY,
            json.encodeToString(ListSerializer(ClimateSchedule.serializer()), items),
        )
    }

    private fun defaultSchedules(): List<ClimateSchedule> = listOf(
        ClimateSchedule(
            id = "clim-weekday-0700",
            name = "출근 프리컨디션",
            hour = 7,
            minute = 0,
            targetTempC = 21f,
            driverSeatHeat = 2,
            steeringHeat = true,
            defrost = true,
            repeat = ClimateRepeat.Weekdays,
            enabled = false,
        ),
    )

    companion object {
        const val KEY = "climate_schedules_v1"
    }
}
