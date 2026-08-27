package com.myt.phase3

import com.myt.domain.history.TripHistoryItem

/**
 * EV vs ICE CO₂ comparison for gamification (M42/M45).
 * Uses Korea grid average (~0.42 kg/kWh) and ICE reference (~0.12 kg/km).
 */
object Co2Calculator {
    const val GRID_KG_PER_KWH = 0.417f
    const val ICE_KG_PER_KM = 0.120f
    const val DEFAULT_CONSUMPTION_KWH_PER_100KM = 16f

    fun summarizeTrips(trips: List<TripHistoryItem>): Co2SavingsSummary {
        val totalKm = trips.sumOf { it.distanceKm.toDouble() }.toFloat()
        if (totalKm <= 0f) {
            return Co2SavingsSummary(0f, 0f, 0f)
        }
        val iceKg = totalKm * ICE_KG_PER_KM
        val evKg = trips.sumOf { trip ->
            val kwh = trip.energyUsedKwh()
            (kwh * GRID_KG_PER_KWH).toDouble()
        }.toFloat()
        val saved = (iceKg - evKg).coerceAtLeast(0f)
        return Co2SavingsSummary(
            totalDistanceKm = totalKm,
            co2SavedKg = saved,
            equivalentTrees = saved / 21f,
        )
    }

    private fun TripHistoryItem.energyUsedKwh(): Float {
        efficiencyKmPerKwh?.takeIf { it > 0f }?.let { eff ->
            return distanceKm / eff
        }
        val delta = if (startSoc != null && endSoc != null) (startSoc - endSoc).coerceAtLeast(0f) else null
        if (delta != null && delta > 0f) {
            // Rough Model 3 pack ~60 kWh usable
            return (delta / 100f) * 60f
        }
        return (distanceKm / 100f) * DEFAULT_CONSUMPTION_KWH_PER_100KM
    }
}
