package com.myt.phase2

/**
 * M33 — push notification channel stub (FCM/APNs later).
 */
interface PushNotifier {
    suspend fun notify(title: String, body: String): Result<Unit>
}

class LogPushNotifier(
    private val log: (String) -> Unit = {},
) : PushNotifier {
    override suspend fun notify(title: String, body: String): Result<Unit> {
        log("push: $title — $body")
        return Result.success(Unit)
    }
}
