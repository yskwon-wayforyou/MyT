package com.myt.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class TeslaConfig(
    val appId: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val partnerDomain: String,
    val fleetApiBase: String,
    val authBaseUrl: String,
    val scopes: String,
    val vehicleVin: String,
    val poiOtaCsvUrl: String = "",
    /** Optional Fleet Telemetry WebSocket URL (M20). Empty = REST polling only. */
    val telemetryWssUrl: String = "",
) {
    val authorizeUrl: String get() = "$authBaseUrl/authorize"
    val tokenUrl: String get() = "$authBaseUrl/token"

    fun isOAuthConfigured(): Boolean =
        clientId.isNotBlank() && clientSecret.isNotBlank() && redirectUri.isNotBlank()

    fun isPartnerConfigured(): Boolean = partnerDomain.isNotBlank()

    companion object {
        val OAuthScopes = listOf(
            "openid",
            "offline_access",
            "vehicle_device_data",
            "vehicle_location",
            "vehicle_cmds",
        ).joinToString(" ")

        fun placeholder(appId: String = ""): TeslaConfig = TeslaConfig(
            appId = appId,
            clientId = "",
            clientSecret = "",
            redirectUri = "https://yskwon-wayforyou.github.io/myt/oauth/callback",
            partnerDomain = "",
            fleetApiBase = "https://fleet-api.prd.na.vn.cloud.tesla.com",
            authBaseUrl = "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3",
            scopes = OAuthScopes,
            vehicleVin = "",
        )
    }
}

class TeslaConfigStore(
    initial: TeslaConfig = loadTeslaConfig(),
) {
    private val _config = MutableStateFlow(initial)
    val config: StateFlow<TeslaConfig> = _config.asStateFlow()

    fun current(): TeslaConfig = _config.value

    fun save(updated: TeslaConfig) {
        persistTeslaConfig(updated)
        _config.value = updated
    }
}

expect fun loadTeslaConfig(): TeslaConfig

expect fun persistTeslaVehicleVin(vin: String)

expect fun persistTeslaConfig(config: TeslaConfig)
