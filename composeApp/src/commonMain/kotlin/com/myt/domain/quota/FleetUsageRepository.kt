package com.myt.domain.quota

interface FleetUsageRepository {
    suspend fun load(): PersistedFleetUsage
    suspend fun save(state: PersistedFleetUsage)
}
