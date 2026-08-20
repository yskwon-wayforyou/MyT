package com.myt.data.settings

import com.myt.domain.repository.SettingsRepository
import com.russhwolf.settings.Settings

class SettingsRepositoryImpl(
    private val settings: Settings,
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

    companion object {
        private const val KEY_VIN = "vin"
        private const val KEY_SPEED_KMH = "speed_unit_kmh"
        private const val KEY_ONBOARDING = "onboarding_complete"
    }
}
