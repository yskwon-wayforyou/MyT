package com.myt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPrefs(
    val controlEnabled: Boolean = true,
    val chargeEnabled: Boolean = true,
    val automationEnabled: Boolean = true,
    val speedCamEnabled: Boolean = true,
)

enum class NotificationCategory {
    Control,
    Charge,
    Automation,
    SpeedCam,
}
