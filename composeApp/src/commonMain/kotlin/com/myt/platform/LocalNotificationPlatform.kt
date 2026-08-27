package com.myt.platform

/**
 * M33 — OS local notifications (Android NotificationManager).
 * FCM token/remote push is a follow-up once google-services.json is available.
 */
expect class LocalNotificationPlatform(context: Any) {
    fun ensureChannels()
    fun post(title: String, body: String)
    fun openAppNotificationSettings(): Boolean
}
