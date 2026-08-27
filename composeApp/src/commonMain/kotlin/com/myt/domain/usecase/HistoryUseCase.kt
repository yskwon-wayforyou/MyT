package com.myt.domain.usecase

import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.DailyAggregate
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.history.HistoryTab
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.repository.HistoryRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HistoryUseCase(
    private val historyRepository: HistoryRepository,
) {
    suspend fun trips(filter: HistoryFilterState): List<TripHistoryItem> =
        historyRepository.trips(filter)

    suspend fun tripById(id: String): TripHistoryItem? =
        historyRepository.tripById(id)

    suspend fun chargeSessions(filter: HistoryFilterState): List<ChargeHistoryItem> =
        historyRepository.chargeSessions(filter)

    suspend fun fleetEvents(filter: HistoryFilterState): List<FleetApiHistoryItem> =
        historyRepository.fleetEvents(filter)

    fun tripChart(items: List<TripHistoryItem>): List<DailyAggregate> =
        items.groupBy { dayLabel(it.startedAtMs) }
            .map { (day, list) -> DailyAggregate(day, list.sumOf { it.distanceKm.toDouble() }.toFloat()) }
            .sortedBy { it.dayLabel }

    fun chargeChart(items: List<ChargeHistoryItem>): List<DailyAggregate> =
        items.groupBy { dayLabel(it.startedAtMs) }
            .map { (day, list) -> DailyAggregate(day, list.sumOf { (it.energyKwh ?: 0f).toDouble() }.toFloat()) }
            .sortedBy { it.dayLabel }

    fun fleetChart(items: List<FleetApiHistoryItem>): List<DailyAggregate> =
        items.groupBy { dayLabel(it.atMs) }
            .map { (day, list) -> DailyAggregate(day, list.size.toFloat()) }
            .sortedBy { it.dayLabel }

    private fun dayLabel(epochMs: Long): String {
        val date = Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${date.monthNumber}/${date.dayOfMonth}"
    }
}
