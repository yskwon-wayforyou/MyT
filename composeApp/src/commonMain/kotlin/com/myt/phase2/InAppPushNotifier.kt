package com.myt.phase2

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppToast(
    val title: String,
    val body: String,
)

/**
 * CommonMain toast bus — UI collects and shows snackbars.
 * Android can also mirror to NotificationManager via [LocalNotificationPlatform].
 */
object InAppToastBus {
    private val _toasts = MutableSharedFlow<InAppToast>(extraBufferCapacity = 8)
    val toasts: SharedFlow<InAppToast> = _toasts.asSharedFlow()

    suspend fun emit(title: String, body: String) {
        _toasts.emit(InAppToast(title, body))
    }
}

class InAppPushNotifier(
    private val localNotify: (title: String, body: String) -> Unit = { _, _ -> },
) : PushNotifier {
    override suspend fun notify(title: String, body: String): Result<Unit> {
        InAppToastBus.emit(title, body)
        runCatching { localNotify(title, body) }
        return Result.success(Unit)
    }
}
