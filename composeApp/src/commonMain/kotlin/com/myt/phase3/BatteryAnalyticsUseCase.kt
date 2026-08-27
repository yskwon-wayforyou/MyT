package com.myt.phase3

import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.usecase.HistoryUseCase
import kotlin.math.abs

/** M42 — battery health trend from charge session efficiency. */
class BatteryAnalyticsUseCase(
    private val historyUseCase: HistoryUseCase,
) {
    suspend fun report(filter: HistoryFilterState = HistoryFilterState(period = HistoryPeriodFilter.All)): BatteryHealthReport {
        val charges = historyUseCase.chargeSessions(filter)
        val points = charges.mapNotNull { estimateCapacityPoint(it) }
            .sortedBy { it.atMs }
        val trend = computeTrend(points)
        return BatteryHealthReport(points = points, trendPctPerYear = trend)
    }

    private fun estimateCapacityPoint(item: ChargeHistoryItem): BatteryHealthPoint? {
        val energy = item.energyKwh ?: return null
        val end = item.endSoc ?: return null
        val delta = end - item.startSoc
        if (delta <= 1f || energy <= 0f) return null
        val nominalKwh = 60f
        val inferredFull = energy / (delta / 100f)
        val capacityPct = ((inferredFull / nominalKwh) * 100f).coerceIn(50f, 105f)
        return BatteryHealthPoint(
            atMs = item.endedAtMs ?: item.startedAtMs,
            capacityPct = capacityPct,
            sourceLabel = "충전 ${item.startSoc.toInt()}→${end.toInt()}%",
        )
    }

    private fun computeTrend(points: List<BatteryHealthPoint>): Float? {
        if (points.size < 2) return null
        val first = points.first()
        val last = points.last()
        val years = (last.atMs - first.atMs).coerceAtLeast(1L) / (365.25 * 24 * 60 * 60 * 1000.0)
        if (years <= 0.01) return null
        return ((last.capacityPct - first.capacityPct) / years.toFloat())
    }
}
