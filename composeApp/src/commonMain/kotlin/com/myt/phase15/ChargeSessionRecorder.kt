package com.myt.phase15

import com.myt.domain.model.GaugeState
import kotlinx.datetime.Instant

data class ChargeSessionRecord(
    val id: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val startSoc: Float,
    val endSoc: Float? = null,
    val energyKwh: Float? = null,
)

/** M19 — Phase 1.5: Charge session recording (stub). */
interface ChargeSessionRecorder {
    fun onGaugeUpdate(state: GaugeState)
    suspend fun activeSession(): ChargeSessionRecord?
}

class StubChargeSessionRecorder : ChargeSessionRecorder {
    override fun onGaugeUpdate(state: GaugeState) = Unit
    override suspend fun activeSession(): ChargeSessionRecord? = null
}
