package com.myt.phase15

import com.myt.domain.model.GaugeState
import kotlinx.coroutines.flow.Flow

/** M18 — Phase 1.5: Trip recording (stub). */
interface TripRecorder {
    val isRecording: Boolean
    fun onGaugeUpdate(state: GaugeState)
    suspend fun flushCurrentTrip()
}

class StubTripRecorder : TripRecorder {
    override val isRecording: Boolean = false
    override fun onGaugeUpdate(state: GaugeState) = Unit
    override suspend fun flushCurrentTrip() = Unit
}
