package com.myt.phase2

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M34/M35 — Watch companion message contract (Apple Watch / Wear OS).
 * In-app preview consumes [lastPayload] until companion apps ship.
 */
data class WatchGaugePayload(
    val socPercent: Int,
    val speedKmh: Int,
    val rangeKm: Int,
    val locked: Boolean?,
    val isCharging: Boolean,
    val updatedAtMs: Long,
)

interface WatchCompanionBridge {
    val lastPayload: StateFlow<WatchGaugePayload?>
    suspend fun push(payload: WatchGaugePayload): Result<Unit>
}

class InMemoryWatchCompanionBridge : WatchCompanionBridge {
    private val _last = MutableStateFlow<WatchGaugePayload?>(null)
    override val lastPayload: StateFlow<WatchGaugePayload?> = _last.asStateFlow()

    override suspend fun push(payload: WatchGaugePayload): Result<Unit> {
        _last.value = payload
        return Result.success(Unit)
    }
}
