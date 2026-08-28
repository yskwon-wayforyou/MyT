package com.myt.platform

import com.myt.domain.model.NotificationCategory

actual class LocalNotificationPlatform actual constructor(context: Any) {
    actual fun ensureChannels() = Unit
    actual fun post(title: String, body: String, category: NotificationCategory, route: String?) = Unit
    actual fun openAppNotificationSettings(): Boolean = false
}
