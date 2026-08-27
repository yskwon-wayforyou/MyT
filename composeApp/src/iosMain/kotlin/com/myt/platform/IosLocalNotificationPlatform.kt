package com.myt.platform

actual class LocalNotificationPlatform actual constructor(context: Any) {
    actual fun ensureChannels() = Unit
    actual fun post(title: String, body: String) = Unit
    actual fun openAppNotificationSettings(): Boolean = false
}
