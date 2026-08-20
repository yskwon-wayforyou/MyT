package com.myt.config

import java.util.Properties

actual fun loadTeslaConfig(): TeslaConfig {
    val props = Properties()
    val candidates = listOf(
        java.io.File("tesla.local.properties"),
        java.io.File("../tesla.local.properties"),
        java.io.File("../../tesla.local.properties"),
        java.io.File(System.getProperty("user.dir"), "tesla.local.properties"),
        java.io.File(System.getProperty("user.dir")).parentFile?.resolve("tesla.local.properties"),
    ).filterNotNull()
    val file = candidates.firstOrNull { it.exists() }

    if (file != null) {
        file.inputStream().use { props.load(it) }
    }

    fun prop(key: String, fallback: String = ""): String =
        props.getProperty(key)?.trim().orEmpty().ifBlank { fallback }

    return TeslaConfig(
        appId = prop("tesla.app.id"),
        clientId = prop("tesla.client.id"),
        clientSecret = prop("tesla.client.secret"),
        redirectUri = prop("tesla.oauth.redirect.uri", "myt://auth/callback"),
        partnerDomain = prop("tesla.partner.domain"),
        fleetApiBase = prop("tesla.fleet.api.base", "https://fleet-api.prd.na.vn.cloud.tesla.com"),
        authBaseUrl = prop("tesla.auth.url", "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3"),
        scopes = prop("tesla.oauth.scopes", TeslaConfig.OAuthScopes),
        vehicleVin = prop("tesla.vehicle.vin"),
    )
}
