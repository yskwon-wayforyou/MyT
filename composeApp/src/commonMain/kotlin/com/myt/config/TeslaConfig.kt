package com.myt.config

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
            redirectUri = "myt://auth/callback",
            partnerDomain = "",
            fleetApiBase = "https://fleet-api.prd.na.vn.cloud.tesla.com",
            authBaseUrl = "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3",
            scopes = OAuthScopes,
            vehicleVin = "",
        )
    }
}

expect fun loadTeslaConfig(): TeslaConfig
