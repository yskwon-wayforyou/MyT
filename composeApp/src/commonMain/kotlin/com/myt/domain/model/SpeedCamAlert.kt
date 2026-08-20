package com.myt.domain.model

enum class AlertLevel {
    L1,
    L2,
    L3,
    SECTION,
}

enum class CameraType {
    FIXED,
    MOBILE,
    SECTION_START,
    SECTION_END,
    SIGNAL,
}

data class SpeedCamera(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val speedLimitKmh: Int,
    val roadName: String? = null,
    val roadDirection: Float? = null,
    val cameraType: CameraType = CameraType.FIXED,
    val sectionLengthM: Int? = null,
)

data class SpeedCamAlert(
    val camera: SpeedCamera,
    val distanceM: Int,
    val currentSpeedKmh: Float,
    val level: AlertLevel,
    val message: String,
    val sectionAverageKmh: Float? = null,
)
