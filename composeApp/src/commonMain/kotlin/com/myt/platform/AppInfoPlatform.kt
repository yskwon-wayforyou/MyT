package com.myt.platform

data class AppInfo(
    val appVersion: String,
    val buildLabel: String,
    val osDescription: String,
    val deviceDescription: String,
    val platformLabel: String,
)

expect class AppInfoPlatform(context: Any) {
    fun collect(): AppInfo
}
