package com.myt.data.settings

import com.myt.domain.model.GaugeDisplayPrefs
import com.myt.domain.repository.SettingsRepository
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SettingsRepository {
    override suspend fun getVin(): String? = settings.getStringOrNull(KEY_VIN)

    override suspend fun setVin(vin: String) {
        settings.putString(KEY_VIN, vin)
    }

    override suspend fun getSpeedUnitKmh(): Boolean =
        settings.getBoolean(KEY_SPEED_KMH, defaultValue = true)

    override suspend fun setSpeedUnitKmh(useKmh: Boolean) {
        settings.putBoolean(KEY_SPEED_KMH, useKmh)
    }

    override suspend fun isOnboardingComplete(): Boolean =
        settings.getBoolean(KEY_ONBOARDING, defaultValue = false)

    override suspend fun setOnboardingComplete(complete: Boolean) {
        settings.putBoolean(KEY_ONBOARDING, complete)
    }

    override suspend fun getGaugeDisplayPrefs(): GaugeDisplayPrefs {
        val raw = settings.getStringOrNull(KEY_GAUGE_PREFS) ?: return GaugeDisplayPrefs()
        return runCatching { json.decodeFromString(GaugeDisplayPrefs.serializer(), raw) }
            .getOrElse { GaugeDisplayPrefs() }
    }

    override suspend fun setGaugeDisplayPrefs(prefs: GaugeDisplayPrefs) {
        settings.putString(KEY_GAUGE_PREFS, json.encodeToString(GaugeDisplayPrefs.serializer(), prefs))
    }

    override suspend fun isDarkTheme(): Boolean =
        settings.getBoolean(KEY_DARK_THEME, defaultValue = true)

    override suspend fun setDarkTheme(enabled: Boolean) {
        settings.putBoolean(KEY_DARK_THEME, enabled)
    }

    override suspend fun isDriveSafetyAcknowledged(): Boolean =
        settings.getBoolean(KEY_DRIVE_SAFETY, defaultValue = false)

    override suspend fun setDriveSafetyAcknowledged(acknowledged: Boolean) {
        settings.putBoolean(KEY_DRIVE_SAFETY, acknowledged)
    }

    companion object {
        private const val KEY_VIN = "vin"
        private const val KEY_SPEED_KMH = "speed_unit_kmh"
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_GAUGE_PREFS = "gauge_display_prefs_v1"
        private const val KEY_DARK_THEME = "dark_theme_v1"
        private const val KEY_DRIVE_SAFETY = "drive_safety_ack_v1"
    }
}
