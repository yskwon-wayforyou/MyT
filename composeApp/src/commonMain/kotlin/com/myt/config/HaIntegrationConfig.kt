package com.myt.config

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HaIntegrationConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val accessToken: String = "",
    val topicPrefix: String = "myt",
    val vinSuffix: String = "",
)

class HaIntegrationConfigStore(
    private val settings: Settings,
) {
    private val _config = MutableStateFlow(load())
    val config: StateFlow<HaIntegrationConfig> = _config.asStateFlow()

    fun current(): HaIntegrationConfig = _config.value

    fun save(updated: HaIntegrationConfig) {
        settings.putBoolean(KEY_ENABLED, updated.enabled)
        settings.putString(KEY_BASE_URL, updated.baseUrl.trim())
        settings.putString(KEY_TOKEN, updated.accessToken.trim())
        settings.putString(KEY_TOPIC_PREFIX, updated.topicPrefix.trim().ifBlank { "myt" })
        settings.putString(KEY_VIN_SUFFIX, updated.vinSuffix.trim())
        _config.value = load()
    }

    private fun load(): HaIntegrationConfig = HaIntegrationConfig(
        enabled = settings.getBoolean(KEY_ENABLED, false),
        baseUrl = settings.getStringOrNull(KEY_BASE_URL).orEmpty(),
        accessToken = settings.getStringOrNull(KEY_TOKEN).orEmpty(),
        topicPrefix = settings.getStringOrNull(KEY_TOPIC_PREFIX)?.ifBlank { "myt" } ?: "myt",
        vinSuffix = settings.getStringOrNull(KEY_VIN_SUFFIX).orEmpty(),
    )

    companion object {
        private const val KEY_ENABLED = "ha_integration_enabled_v1"
        private const val KEY_BASE_URL = "ha_integration_base_url_v1"
        private const val KEY_TOKEN = "ha_integration_token_v1"
        private const val KEY_TOPIC_PREFIX = "ha_integration_topic_prefix_v1"
        private const val KEY_VIN_SUFFIX = "ha_integration_vin_suffix_v1"
    }
}

private fun Settings.getStringOrNull(key: String): String? =
    if (hasKey(key)) getString(key, "") else null
