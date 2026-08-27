package com.myt.domain.automation

import com.myt.domain.control.ControlRequest
import com.myt.domain.control.ControlResult
import com.myt.domain.control.VehicleCommand
import com.myt.domain.control.VehicleControlGateway
import com.myt.domain.repository.SettingsRepository
import com.myt.phase2.PushNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Runs FR-V06a climate schedules on a 30s tick while Gauge is active.
 */
class ClimateScheduleEngine(
    private val repository: ClimateScheduleRepository,
    private val controlGateway: VehicleControlGateway,
    private val settingsRepository: SettingsRepository,
    private val pushNotifier: PushNotifier,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                tick()
                delay(30_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun tick() {
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dayKey = ClimateScheduleMatcher.dayKey(now.year, now.monthNumber, now.dayOfMonth)
        val vin = settingsRepository.getVin()?.trim().orEmpty()
        val due = repository.list().filter {
            ClimateScheduleMatcher.shouldFire(
                schedule = it,
                hour = now.hour,
                minute = now.minute,
                dayOfWeek = isoDayOfWeek(now.dayOfWeek),
                dayKey = dayKey,
            )
        }
        for (schedule in due) {
            execute(schedule, vin, dayKey)
        }
    }

    private suspend fun execute(schedule: ClimateSchedule, vin: String, dayKey: String) {
        val marked = schedule.copy(lastFiredKey = dayKey)
        repository.save(marked)
        val summary = ClimateScheduleMatcher.summarize(schedule)
        if (vin.isBlank()) {
            pushNotifier.notify(schedule.name, "VIN 없음 · $summary (예약만 기록)")
            return
        }
        val result = controlGateway.execute(
            ControlRequest(command = VehicleCommand.ClimateOn, vin = vin),
        )
        val body = when (result) {
            is ControlResult.Accepted -> "공조 시작 · $summary"
            is ControlResult.Rejected -> "차단됨: ${result.reason} · $summary"
        }
        pushNotifier.notify(schedule.name, body)
    }

    companion object {
        private fun isoDayOfWeek(day: kotlinx.datetime.DayOfWeek): Int =
            when (day) {
                kotlinx.datetime.DayOfWeek.MONDAY -> 1
                kotlinx.datetime.DayOfWeek.TUESDAY -> 2
                kotlinx.datetime.DayOfWeek.WEDNESDAY -> 3
                kotlinx.datetime.DayOfWeek.THURSDAY -> 4
                kotlinx.datetime.DayOfWeek.FRIDAY -> 5
                kotlinx.datetime.DayOfWeek.SATURDAY -> 6
                kotlinx.datetime.DayOfWeek.SUNDAY -> 7
            }
    }
}
