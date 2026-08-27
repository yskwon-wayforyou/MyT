package com.myt.domain.repository

import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.history.VehicleSnapshotPayload
import com.myt.domain.model.GaugeState
import com.myt.domain.quota.FleetCallCategory

interface HistoryRepository {
    suspend fun recordTrip(item: TripHistoryItem)
    suspend fun updateTripEnd(item: TripHistoryItem)
    suspend fun recordCharge(item: ChargeHistoryItem)
    suspend fun recordFleetEvent(category: FleetCallCategory, ok: Boolean, detail: String? = null)
    suspend fun saveVehicleSnapshot(vin: String, state: GaugeState)
    suspend fun loadVehicleSnapshot(vin: String): GaugeState?
    suspend fun trips(filter: HistoryFilterState): List<TripHistoryItem>
    suspend fun tripById(id: String): TripHistoryItem?
    suspend fun chargeSessions(filter: HistoryFilterState): List<ChargeHistoryItem>
    suspend fun fleetEvents(filter: HistoryFilterState): List<FleetApiHistoryItem>
}

interface TripRecorder {
    val isRecording: Boolean
    fun onGaugeUpdate(state: GaugeState, vin: String)
    suspend fun flushCurrentTrip()
}

interface ChargeSessionRecorder {
    fun onGaugeUpdate(state: GaugeState, vin: String)
    suspend fun activeSessionId(): String?
}
