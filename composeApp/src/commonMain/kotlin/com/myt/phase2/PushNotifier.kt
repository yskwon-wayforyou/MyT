package com.myt.phase2

import com.myt.domain.model.NotificationCategory

interface PushNotifier {
    suspend fun notify(
        title: String,
        body: String,
        category: NotificationCategory = NotificationCategory.Automation,
        route: String? = null,
    ): Result<Unit>
}

class LogPushNotifier(
    private val log: (String) -> Unit = {},
) : PushNotifier {
    override suspend fun notify(
        title: String,
        body: String,
        category: NotificationCategory,
        route: String?,
    ): Result<Unit> {
        log("push[$category]: $title — $body route=$route")
        return Result.success(Unit)
    }
}
