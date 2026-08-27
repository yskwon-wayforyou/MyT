package com.myt.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GaugeLayoutMode {
    Auto,
    Portrait,
    Landscape,
    Split,
}

@Serializable
enum class GaugeField {
    Speed,
    Gear,
    Battery,
    Range,
    InsideTemp,
    OutsideTemp,
    Power,
    Charge,
    Lock,
    Sentry,
    Climate,
    Tires,
    Odometer,
    Nav,
    SpeedCam,
    Actions,
}

@Serializable
data class GaugeDisplayPrefs(
    val layoutMode: GaugeLayoutMode = GaugeLayoutMode.Auto,
    val visibleFields: Set<GaugeField> = GaugeField.entries.toSet(),
    val gridColumns: Int = 0,
    val driveDensity: DriveDensity = DriveDensity.Standard,
    /** When true and BT connected, prefer handset GPS for speed/location. */
    val preferDeviceSpeed: Boolean = true,
    val pressureUnit: PressureUnit = PressureUnit.Psi,
) {
    fun shows(field: GaugeField): Boolean = field in visibleFields

    fun showsOnDriveHome(field: GaugeField): Boolean =
        field in driveHomeFields() && shows(field)

    fun usePsi(): Boolean = pressureUnit == PressureUnit.Psi

    fun resolvedColumns(defaultColumns: Int): Int =
        if (gridColumns in 2..4) gridColumns else defaultColumns

    fun driveHomeFields(): Set<GaugeField> = when (driveDensity) {
        DriveDensity.Minimal -> setOf(
            GaugeField.Speed, GaugeField.Gear, GaugeField.Battery, GaugeField.Range, GaugeField.SpeedCam, GaugeField.Actions,
        )
        DriveDensity.Standard -> setOf(
            GaugeField.Speed, GaugeField.Gear, GaugeField.Battery, GaugeField.Range,
            GaugeField.Tires, GaugeField.SpeedCam, GaugeField.Actions, GaugeField.Lock, GaugeField.Climate,
        )
        DriveDensity.Pro -> visibleFields
    }
}

fun GaugeField.labelKo(): String = when (this) {
    GaugeField.Speed -> "속도"
    GaugeField.Gear -> "기어"
    GaugeField.Battery -> "배터리"
    GaugeField.Range -> "주행 가능"
    GaugeField.InsideTemp -> "실내 온도"
    GaugeField.OutsideTemp -> "외기 온도"
    GaugeField.Power -> "전력"
    GaugeField.Charge -> "충전"
    GaugeField.Lock -> "잠금"
    GaugeField.Sentry -> "Sentry"
    GaugeField.Climate -> "공조"
    GaugeField.Tires -> "타이어"
    GaugeField.Odometer -> "누적 거리"
    GaugeField.Nav -> "내비"
    GaugeField.SpeedCam -> "과속 카메라"
    GaugeField.Actions -> "하단 버튼"
}

fun GaugeLayoutMode.labelKo(): String = when (this) {
    GaugeLayoutMode.Auto -> "자동"
    GaugeLayoutMode.Portrait -> "세로"
    GaugeLayoutMode.Landscape -> "가로"
    GaugeLayoutMode.Split -> "분할"
}
