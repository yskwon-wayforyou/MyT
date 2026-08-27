package com.myt.config

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "tesla_config_json"
private val json = Json { ignoreUnknownKeys = true }

actual fun loadTeslaConfig(): TeslaConfig {
    val raw = Settings().getStringOrNull(KEY) ?: return TeslaConfig.placeholder()
    return runCatching { json.decodeFromString(TeslaConfig.serializer(), raw) }
        .getOrElse { TeslaConfig.placeholder() }
}

actual fun persistTeslaVehicleVin(vin: String) {
    val normalized = vin.trim().uppercase()
    if (normalized.isBlank()) return
    persistTeslaConfig(loadTeslaConfig().copy(vehicleVin = normalized))
}

actual fun persistTeslaConfig(config: TeslaConfig) {
    Settings().putString(KEY, json.encodeToString(TeslaConfig.serializer(), config))
}
