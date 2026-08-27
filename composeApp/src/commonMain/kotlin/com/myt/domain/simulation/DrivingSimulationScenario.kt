package com.myt.domain.simulation

import com.myt.domain.model.ChargeInfo
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.NavInfo
import com.myt.domain.model.TelemetrySource
import com.myt.domain.model.TirePressures
import kotlinx.datetime.Clock

enum class DrivingSimulationId {
    /** 주차·충전·고정 좌표 (맵·충전 UI) — 광교중앙역 */
    ChargingParkedSuwon,
    /** 광교중앙로 cam-su-003 향해 95km/h — L3 과속 단속 */
    ApproachSpeedCamL3,
    /** 고속 주행 + 내비 활성 */
    HighwayWithNav,
}

data class DrivingSimulationScenario(
    val id: DrivingSimulationId,
    val name: String,
    val tickMs: Long = 500L,
    val frames: List<GaugeState>,
    val loop: Boolean = false,
)

object DrivingSimulationScenarios {
    /** 광교중앙역 (신분당선) */
    private const val GWANGGYO_JUNGANG_LAT = 37.3372
    private const val GWANGGYO_JUNGANG_LNG = 127.1023

    /** cam-su-003 광교중앙로 */
    private const val CAM_LAT = 37.2851
    private const val CAM_LNG = 127.0532

    fun byId(id: DrivingSimulationId): DrivingSimulationScenario = when (id) {
        DrivingSimulationId.ChargingParkedSuwon -> chargingParked()
        DrivingSimulationId.ApproachSpeedCamL3 -> approachSpeedCamL3()
        DrivingSimulationId.HighwayWithNav -> highwayWithNav()
    }

    fun all(): List<DrivingSimulationScenario> = DrivingSimulationId.entries.map { byId(it) }

    private fun base(
        speedKmh: Float,
        gear: Gear,
        lat: Double,
        lng: Double,
        heading: Float,
        scenarioLabel: String,
        charging: ChargeInfo? = null,
        nav: NavInfo? = null,
        tires: TirePressures? = defaultTires(),
    ): GaugeState {
        val now = Clock.System.now().toEpochMilliseconds()
        return GaugeState(
            speedKmh = speedKmh,
            gear = gear,
            socPercent = 62f,
            rangeKm = 280f,
            latitude = lat,
            longitude = lng,
            headingDegrees = heading,
            tires = tires,
            navigation = nav,
            charging = charging,
            connection = ConnectionStatus.FleetConnected,
            bluetoothPresent = true,
            locked = true,
            isSimulated = true,
            simulationLabel = scenarioLabel,
            speedSource = TelemetrySource.Device,
            locationSource = TelemetrySource.Device,
            lastUpdated = now,
        )
    }

    private fun defaultTires() = TirePressures(2.9f, 2.9f, 2.85f, 2.85f)

    private fun chargingParked(): DrivingSimulationScenario {
        val label = "충전 주차 (광교중앙역)"
        val state = base(
            speedKmh = 0f,
            gear = Gear.PARK,
            lat = GWANGGYO_JUNGANG_LAT,
            lng = GWANGGYO_JUNGANG_LNG,
            heading = 90f,
            scenarioLabel = label,
            charging = ChargeInfo(
                isCharging = true,
                chargeRateKw = 11.2f,
                timeToFullMinutes = 145,
                chargeLimitPercent = 80,
                chargingState = "Charging",
            ),
        )
        return DrivingSimulationScenario(
            id = DrivingSimulationId.ChargingParkedSuwon,
            name = label,
            frames = List(120) { state.copy(lastUpdated = Clock.System.now().toEpochMilliseconds()) },
            loop = true,
        )
    }

    private fun approachSpeedCamL3(): DrivingSimulationScenario {
        val label = "과속카메라 L3 접근 (광교중앙로)"
        // 북쪽 ~550m → cam-su-003 통과, 95km/h (한도 60)
        val frames = buildList {
            var lat = 37.2900
            val lng = CAM_LNG
            val stepLat = -0.000055 // ~6m per tick southbound
            for (i in 0 until 150) {
                val speed = when {
                    i < 8 -> 30f + i * 8f
                    i < 120 -> 95f
                    else -> (95f - (i - 120) * 12f).coerceAtLeast(0f)
                }
                add(
                    base(
                        speedKmh = speed,
                        gear = if (speed < 3f) Gear.PARK else Gear.DRIVE,
                        lat = lat,
                        lng = lng,
                        heading = 180f,
                        scenarioLabel = label,
                    ),
                )
                lat += stepLat
            }
        }
        return DrivingSimulationScenario(
            id = DrivingSimulationId.ApproachSpeedCamL3,
            name = label,
            frames = frames,
            loop = false,
        )
    }

    private fun highwayWithNav(): DrivingSimulationScenario {
        val label = "고속 주행 + 내비 (광교→판교)"
        val frames = (0 until 80).map { i ->
            base(
                speedKmh = 100f,
                gear = Gear.DRIVE,
                lat = GWANGGYO_JUNGANG_LAT + i * 0.00035,
                lng = GWANGGYO_JUNGANG_LNG + i * 0.00045,
                heading = 45f,
                scenarioLabel = label,
                nav = NavInfo(
                    destinationName = "판교역",
                    etaMinutes = 18 - i / 10,
                    distanceKm = 12f - i * 0.12f,
                    isActive = true,
                    destinationLatitude = 37.3947,
                    destinationLongitude = 127.1112,
                ),
            )
        }
        return DrivingSimulationScenario(
            id = DrivingSimulationId.HighwayWithNav,
            name = label,
            frames = frames,
            loop = true,
        )
    }
}
