package com.myt.config

import com.myt.MyTApplication
import java.util.Properties

private const val CONFIG_FILE_NAME = "tesla.local.properties"

actual fun loadTeslaConfig(): TeslaConfig {
    val props = Properties()
    loadFromApkAssets(props)
    if (props.isEmpty) {
        loadFromWorkspaceFile(props)
    }
    val writable = writableConfigFile()
    if (writable?.exists() == true) {
        val overlay = Properties()
        writable.inputStream().use { overlay.load(it) }
        overlay.forEach { key, value ->
            val text = value.toString().trim()
            if (text.isNotBlank()) {
                props.setProperty(key.toString(), text)
            }
        }
    } else {
        seedWritableConfigFromAssets()
    }

    fun prop(key: String, fallback: String = ""): String =
        props.getProperty(key)?.trim().orEmpty().ifBlank { fallback }

    return TeslaConfig(
        appId = prop("tesla.app.id"),
        clientId = prop("tesla.client.id"),
        clientSecret = prop("tesla.client.secret"),
        redirectUri = prop(
            "tesla.oauth.redirect.uri",
            "https://yskwon-wayforyou.github.io/myt/oauth/callback",
        ),
        partnerDomain = prop("tesla.partner.domain"),
        fleetApiBase = prop("tesla.fleet.api.base", "https://fleet-api.prd.na.vn.cloud.tesla.com"),
        authBaseUrl = prop("tesla.auth.url", "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3"),
        scopes = prop("tesla.oauth.scopes", TeslaConfig.OAuthScopes),
        vehicleVin = prop("tesla.vehicle.vin"),
        poiOtaCsvUrl = prop("tesla.poi.ota.csv.url"),
        telemetryWssUrl = prop("tesla.telemetry.wss.url"),
    )
}

actual fun persistTeslaVehicleVin(vin: String) {
    val normalized = vin.trim().uppercase()
    if (normalized.isBlank()) return
    persistTeslaConfig(loadTeslaConfig().copy(vehicleVin = normalized))
}

actual fun persistTeslaConfig(config: TeslaConfig) {
    seedWritableConfigFromAssets()
    val file = writableConfigFile() ?: return
    val props = java.util.Properties()
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    fun put(key: String, value: String) {
        props.setProperty(key, value)
    }
    put("tesla.app.id", config.appId)
    put("tesla.client.id", config.clientId)
    put("tesla.client.secret", config.clientSecret)
    put("tesla.oauth.redirect.uri", config.redirectUri)
    put("tesla.partner.domain", config.partnerDomain)
    put("tesla.fleet.api.base", config.fleetApiBase)
    put("tesla.auth.url", config.authBaseUrl)
    put("tesla.oauth.scopes", config.scopes)
    put("tesla.vehicle.vin", config.vehicleVin.trim().uppercase())
    put("tesla.poi.ota.csv.url", config.poiOtaCsvUrl)
    put("tesla.telemetry.wss.url", config.telemetryWssUrl)
    file.bufferedWriter().use { writer ->
        writer.appendLine("# MyT Tesla config (device copy of tesla.local.properties)")
        props.stringPropertyNames().sorted().forEach { key ->
            writer.appendLine("$key=${props.getProperty(key).orEmpty()}")
        }
    }
}

private fun writableConfigFile(): java.io.File? {
    if (!MyTApplication.isInitialized) return null
    return java.io.File(MyTApplication.instance.filesDir, CONFIG_FILE_NAME)
}

private fun seedWritableConfigFromAssets() {
    val dest = writableConfigFile() ?: return
    if (dest.exists()) return
    if (!MyTApplication.isInitialized) return
    runCatching {
        MyTApplication.instance.assets.open(CONFIG_FILE_NAME).use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

private fun loadFromApkAssets(props: Properties) {
    if (!MyTApplication.isInitialized) return
    runCatching {
        MyTApplication.instance.assets.open(CONFIG_FILE_NAME).use { props.load(it) }
    }
}

private fun loadFromWorkspaceFile(props: Properties) {
    val candidates = listOf(
        java.io.File(CONFIG_FILE_NAME),
        java.io.File("../$CONFIG_FILE_NAME"),
        java.io.File("../../$CONFIG_FILE_NAME"),
        System.getProperty("user.dir")?.let { java.io.File(it, CONFIG_FILE_NAME) },
        System.getProperty("user.dir")?.let { java.io.File(it).parentFile?.resolve(CONFIG_FILE_NAME) },
    ).filterNotNull()
    val file = candidates.firstOrNull { it.exists() } ?: return
    file.inputStream().use { props.load(it) }
}
