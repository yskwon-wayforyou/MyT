package com.myt.platform

import com.myt.domain.model.NotificationCategory

/**
 * M33 — OS local notifications (Android NotificationManager).
 * FCM token/remote push is a follow-up once google-services.json is available.
 */
expect class LocalNotificationPlatform(context: Any) {
    fun ensureChannels()
    fun post(title: String, body: String, category: NotificationCategory, route: String?)
    fun openAppNotificationSettings(): Boolean
}
