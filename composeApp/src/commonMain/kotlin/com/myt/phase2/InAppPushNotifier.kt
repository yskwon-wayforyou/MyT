package com.myt.phase2

import com.myt.domain.model.NotificationCategory
import com.myt.domain.model.NotificationPrefs
import com.myt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppToast(
    val title: String,
    val body: String,
)

object InAppToastBus {
    private val _toasts = MutableSharedFlow<InAppToast>(extraBufferCapacity = 8)
    val toasts: SharedFlow<InAppToast> = _toasts.asSharedFlow()

    suspend fun emit(title: String, body: String) {
        _toasts.emit(InAppToast(title, body))
    }
}

class InAppPushNotifier(
    private val settingsRepository: SettingsRepository,
    private val localNotify: (title: String, body: String, category: NotificationCategory, route: String?) -> Unit = { _, _, _, _ -> },
) : PushNotifier {
    override suspend fun notify(
        title: String,
        body: String,
        category: NotificationCategory,
        route: String?,
    ): Result<Unit> {
        val prefs = settingsRepository.getNotificationPrefs()
        val allowed = when (category) {
            NotificationCategory.Control -> prefs.controlEnabled
            NotificationCategory.Charge -> prefs.chargeEnabled
            NotificationCategory.Automation -> prefs.automationEnabled
            NotificationCategory.SpeedCam -> prefs.speedCamEnabled
        }
        if (!allowed) return Result.success(Unit)
        InAppToastBus.emit(title, body)
        runCatching { localNotify(title, body, category, route) }
        return Result.success(Unit)
    }
}
