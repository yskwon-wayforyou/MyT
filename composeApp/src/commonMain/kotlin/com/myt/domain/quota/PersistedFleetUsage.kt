package com.myt.domain.quota

import kotlinx.serialization.Serializable

@Serializable
data class PersistedFleetUsage(
    val month: String = "",
    val dataCount: Int = 0,
    val commandCount: Int = 0,
    val wakeCount: Int = 0,
    val daily: Map<String, PersistedDayUsage> = emptyMap(),
    val recent: List<PersistedUsageEvent> = emptyList(),
)

@Serializable
data class PersistedDayUsage(
    val data: Int = 0,
    val command: Int = 0,
    val wake: Int = 0,
)

@Serializable
data class PersistedUsageEvent(
    val atEpochMs: Long,
    val category: String,
    val ok: Boolean,
)
