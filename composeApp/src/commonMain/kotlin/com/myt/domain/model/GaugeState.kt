package com.myt.domain.model

enum class ConnectionStatus {
    Disconnected,
    BluetoothOnly,
    FleetConnected,
    Sleeping,
    Error,
}

data class NavInfo(
    val destinationName: String?,
    val etaMinutes: Int?,
    val distanceKm: Float?,
)

data class ChargeInfo(
    val isCharging: Boolean = false,
    val chargeRateKw: Float? = null,
    val timeToFullMinutes: Int? = null,
    val chargeLimitPercent: Int? = null,
)

data class TirePressures(
    val frontLeftBar: Float,
    val frontRightBar: Float,
    val rearLeftBar: Float,
    val rearRightBar: Float,
)

data class GaugeState(
    val speedKmh: Float = 0f,
    val gear: Gear = Gear.PARK,
    val socPercent: Float = 0f,
    val rangeKm: Float = 0f,
    val insideTempC: Float? = null,
    val outsideTempC: Float? = null,
    val powerKw: Float? = null,
    val longAccelG: Float = 0f,
    val latAccelG: Float = 0f,
    val tires: TirePressures? = null,
    val navigation: NavInfo? = null,
    val charging: ChargeInfo? = null,
    val connection: ConnectionStatus = ConnectionStatus.Disconnected,
    val isSleeping: Boolean = false,
    val lastUpdated: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val headingDegrees: Float? = null,
)
