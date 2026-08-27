package com.myt.domain

import com.myt.debug.DebugLogger
import com.myt.test.TestSettings
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.model.GaugeState
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.quota.FleetQuotaPolicy
import com.myt.domain.quota.FleetUsageRepository
import com.myt.domain.quota.PersistedFleetUsage
import com.myt.domain.quota.QuotaMode
import com.myt.domain.usecase.FleetQuotaUseCase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FleetQuotaUseCaseTest {
    @Test
    fun costMatchesOfficialRates() {
        assertEquals(1.0, FleetQuotaPolicy.costUsd(500, 0, 0), 0.0001)
        assertEquals(1.0, FleetQuotaPolicy.costUsd(0, 1000, 0), 0.0001)
        assertEquals(1.0, FleetQuotaPolicy.costUsd(0, 0, 50), 0.0001)
        assertEquals(7.2, FleetQuotaPolicy.costUsd(3000, 200, 50), 0.0001)
    }

    @Test
    fun modeSwitchesAtCreditThresholds() {
        assertEquals(QuotaMode.Normal, FleetQuotaPolicy.mode(0.69f))
        assertEquals(QuotaMode.Conserve, FleetQuotaPolicy.mode(0.70f))
        assertEquals(QuotaMode.Blocked, FleetQuotaPolicy.mode(0.95f))
    }

    @Test
    fun deniesDataAfterMonthlyCap() = runBlocking {
        val repo = InMemoryUsage()
        val quota = FleetQuotaUseCase(repo, NoOpHistory(), DebugLogger(TestSettings()))
        quota.hydrate()
        repeat(FleetQuotaPolicy.MONTHLY_DATA) {
            quota.record(FleetCallCategory.Data, true)
        }
        val decision = quota.evaluate(FleetCallCategory.Data)
        assertFalse(decision.allowed)
        assertEquals(0.6, quota.snapshot.value.usedRatio.toDouble(), 0.001)
    }

    @Test
    fun deniesWakeAfterDailyCap() = runBlocking {
        val repo = InMemoryUsage()
        val quota = FleetQuotaUseCase(repo, NoOpHistory(), DebugLogger(TestSettings()))
        quota.hydrate()
        repeat(FleetQuotaPolicy.DAILY_WAKE) {
            quota.record(FleetCallCategory.Wake, true)
        }
        val decision = quota.evaluate(FleetCallCategory.Wake)
        assertFalse(decision.allowed)
        assertTrue(quota.snapshot.value.dailyWakeCount >= FleetQuotaPolicy.DAILY_WAKE)
    }

    @Test
    fun allowsDataUnderDailyCap() = runBlocking {
        val repo = InMemoryUsage()
        val quota = FleetQuotaUseCase(repo, NoOpHistory(), DebugLogger(TestSettings()))
        quota.hydrate()
        val decision = quota.evaluate(FleetCallCategory.Data)
        assertTrue(decision.allowed)
        assertEquals(QuotaMode.Normal, decision.mode)
    }

    private class NoOpHistory : HistoryRepository {
        override suspend fun recordTrip(item: TripHistoryItem) = Unit
        override suspend fun updateTripEnd(item: TripHistoryItem) = Unit
        override suspend fun recordCharge(item: ChargeHistoryItem) = Unit
        override suspend fun recordFleetEvent(category: FleetCallCategory, ok: Boolean, detail: String?) = Unit
        override suspend fun saveVehicleSnapshot(vin: String, state: GaugeState) = Unit
        override suspend fun loadVehicleSnapshot(vin: String): GaugeState? = null
        override suspend fun trips(filter: HistoryFilterState): List<TripHistoryItem> = emptyList()
        override suspend fun tripById(id: String): TripHistoryItem? = null
        override suspend fun chargeSessions(filter: HistoryFilterState): List<ChargeHistoryItem> = emptyList()
        override suspend fun fleetEvents(filter: HistoryFilterState): List<FleetApiHistoryItem> = emptyList()
    }

    private class InMemoryUsage : FleetUsageRepository {
        private var state = PersistedFleetUsage()
        override suspend fun load(): PersistedFleetUsage = state
        override suspend fun save(state: PersistedFleetUsage) {
            this.state = state
        }
    }
}
