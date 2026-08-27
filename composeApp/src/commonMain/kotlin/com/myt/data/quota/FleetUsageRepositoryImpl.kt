package com.myt.data.quota

import com.myt.domain.quota.FleetUsageRepository
import com.myt.domain.quota.PersistedFleetUsage
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FleetUsageRepositoryImpl(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : FleetUsageRepository {
    override suspend fun load(): PersistedFleetUsage {
        val raw = settings.getStringOrNull(KEY) ?: return PersistedFleetUsage()
        return runCatching { json.decodeFromString<PersistedFleetUsage>(raw) }
            .getOrElse { PersistedFleetUsage() }
    }

    override suspend fun save(state: PersistedFleetUsage) {
        settings.putString(KEY, json.encodeToString(state))
    }

    companion object {
        private const val KEY = "fleet_api_usage_v1"
    }
}
