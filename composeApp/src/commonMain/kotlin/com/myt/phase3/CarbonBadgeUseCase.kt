package com.myt.phase3

import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.usecase.HistoryUseCase

/** M45 — CO₂ savings gamification. */
class CarbonBadgeUseCase(
    private val historyUseCase: HistoryUseCase,
) {
    suspend fun state(filter: HistoryFilterState = HistoryFilterState(period = HistoryPeriodFilter.All)): CarbonBadgeState {
        val trips = historyUseCase.trips(filter)
        val saved = Co2Calculator.summarizeTrips(trips).co2SavedKg
        val tier = CarbonBadgeTier.entries.lastOrNull { saved >= it.minCo2SavedKg } ?: CarbonBadgeTier.Seedling
        val next = CarbonBadgeTier.entries.firstOrNull { it.minCo2SavedKg > saved }
        val progress = if (next == null) {
            1f
        } else {
            val span = next.minCo2SavedKg - tier.minCo2SavedKg
            if (span <= 0f) 1f else ((saved - tier.minCo2SavedKg) / span).coerceIn(0f, 1f)
        }
        return CarbonBadgeState(tier = tier, co2SavedKg = saved, nextTier = next, progressToNext = progress)
    }
}
