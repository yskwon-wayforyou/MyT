package com.myt.domain.model

enum class ConnectionStatus {
    Disconnected,
    BluetoothOnly,
    FleetConnected,
    Sleeping,
    Error,
    QuotaHold,
}

data class NavInfo(
    val destinationName: String?,
    val etaMinutes: Int?,
    val distanceKm: Float?,
    /** True when car has an active navigation route from Fleet drive_state. */
    val isActive: Boolean = destinationName != null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
)

data class ChargeInfo(
    val isCharging: Boolean = false,
    val chargeRateKw: Float? = null,
    val timeToFullMinutes: Int? = null,
    val chargeLimitPercent: Int? = null,
    val chargingState: String? = null,
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
    val locked: Boolean? = null,
    val odometerKm: Float? = null,
    val sentryMode: Boolean? = null,
    val climateOn: Boolean? = null,
    val bluetoothPresent: Boolean = false,
    val speedSource: TelemetrySource = TelemetrySource.None,
    val locationSource: TelemetrySource = TelemetrySource.None,
    /**
     * Turn / hazard indicators. Fleet `vehicle_data` does not expose these;
     * reserved for future BLE/CAN. Null = unknown / unavailable.
     */
    val turnSignalLeft: Boolean? = null,
    val turnSignalRight: Boolean? = null,
    val hazardLightsOn: Boolean? = null,
    /** Debug / simulation overlay — SpeedCam·주행 테스트용 */
    val isSimulated: Boolean = false,
    /** Active simulation scenario label for UI banner */
    val simulationLabel: String? = null,
)
