package com.myt.domain

import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.usecase.HistoryUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryUseCaseTest {
    private val useCase = HistoryUseCase(
        historyRepository = object : com.myt.domain.repository.HistoryRepository {
            override suspend fun recordTrip(item: TripHistoryItem) = Unit
            override suspend fun updateTripEnd(item: TripHistoryItem) = Unit
            override suspend fun recordCharge(item: com.myt.domain.history.ChargeHistoryItem) = Unit
            override suspend fun recordFleetEvent(
                category: com.myt.domain.quota.FleetCallCategory,
                ok: Boolean,
                detail: String?,
            ) = Unit
            override suspend fun saveVehicleSnapshot(vin: String, state: com.myt.domain.model.GaugeState) = Unit
            override suspend fun loadVehicleSnapshot(vin: String) = null
            override suspend fun trips(filter: HistoryFilterState): List<TripHistoryItem> = emptyList()
            override suspend fun tripById(id: String): TripHistoryItem? = null
            override suspend fun chargeSessions(filter: HistoryFilterState): List<ChargeHistoryItem> = emptyList()
            override suspend fun fleetEvents(filter: HistoryFilterState): List<FleetApiHistoryItem> = emptyList()
        },
    )

    @Test
    fun tripChartSumsDistanceByDay() {
        val items = listOf(
            trip(startedAtMs = 1_700_000_000_000, distanceKm = 10f),
            trip(startedAtMs = 1_700_000_000_000 + 3_600_000, distanceKm = 5f),
        )
        val chart = useCase.tripChart(items)
        assertEquals(1, chart.size)
        assertEquals(15f, chart.first().value)
    }

    @Test
    fun chargeChartSumsEnergyByDay() {
        val items = listOf(
            charge(startedAtMs = 1_700_000_000_000, energyKwh = 12f),
            charge(startedAtMs = 1_700_000_000_000 + 1_800_000, energyKwh = 8f),
        )
        val chart = useCase.chargeChart(items)
        assertEquals(1, chart.size)
        assertEquals(20f, chart.first().value)
    }

    private fun trip(startedAtMs: Long, distanceKm: Float) = TripHistoryItem(
        id = startedAtMs.toString(),
        vin = "VIN",
        startedAtMs = startedAtMs,
        endedAtMs = startedAtMs + 1_000,
        distanceKm = distanceKm,
        avgSpeedKmh = null,
        maxSpeedKmh = null,
        startSoc = null,
        endSoc = null,
    )

    private fun charge(startedAtMs: Long, energyKwh: Float) = ChargeHistoryItem(
        id = startedAtMs.toString(),
        vin = "VIN",
        startedAtMs = startedAtMs,
        endedAtMs = startedAtMs + 1_000,
        startSoc = 20f,
        endSoc = 80f,
        energyKwh = energyKwh,
        peakKw = null,
    )
}
