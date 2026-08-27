package com.myt.phase3

import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.usecase.HistoryUseCase

class SqlDataPortability(
    private val historyUseCase: HistoryUseCase,
    private val historyRepository: HistoryRepository,
) : DataPortability {
    override suspend fun exportTripsCsv(): String {
        val trips = historyUseCase.trips(allTimeFilter())
        return buildString {
            appendLine(TRIP_HEADER)
            trips.forEach { appendLine(it.toCsvRow()) }
        }
    }

    override suspend fun exportChargesCsv(): String {
        val charges = historyUseCase.chargeSessions(allTimeFilter())
        return buildString {
            appendLine(CHARGE_HEADER)
            charges.forEach { appendLine(it.toCsvRow()) }
        }
    }

    override suspend fun importFromTessieCsv(csvContent: String, defaultVin: String): Result<Int> {
        return runCatching {
            val parsed = TessieCsvParser.parseTrips(csvContent, defaultVin)
            parsed.forEach { historyRepository.recordTrip(it) }
            parsed.size
        }
    }

    private fun allTimeFilter() = HistoryFilterState(period = HistoryPeriodFilter.All)

    private fun TripHistoryItem.toCsvRow(): String = listOf(
        id,
        vin,
        startedAtMs,
        endedAtMs ?: "",
        distanceKm,
        avgSpeedKmh ?: "",
        maxSpeedKmh ?: "",
        startSoc ?: "",
        endSoc ?: "",
        polylineEncoded ?: "",
        efficiencyKmPerKwh ?: "",
    ).joinToString(",")

    private fun ChargeHistoryItem.toCsvRow(): String = listOf(
        id,
        vin,
        startedAtMs,
        endedAtMs ?: "",
        startSoc,
        endSoc ?: "",
        energyKwh ?: "",
        peakKw ?: "",
    ).joinToString(",")

    companion object {
        const val TRIP_HEADER =
            "id,vin,started_at_ms,ended_at_ms,distance_km,avg_speed_kmh,max_speed_kmh,start_soc,end_soc,polyline,efficiency_km_per_kwh"
        const val CHARGE_HEADER =
            "id,vin,started_at_ms,ended_at_ms,start_soc,end_soc,energy_kwh,peak_kw"
    }
}

class StubDataPortability : DataPortability {
    override suspend fun exportTripsCsv(): String = "${SqlDataPortability.TRIP_HEADER}\n"
    override suspend fun exportChargesCsv(): String = "${SqlDataPortability.CHARGE_HEADER}\n"
    override suspend fun importFromTessieCsv(csvContent: String, defaultVin: String): Result<Int> =
        Result.failure(UnsupportedOperationException("DataPortability not wired"))
}
