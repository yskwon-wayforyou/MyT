package com.myt.domain.control

object VehicleCommandLabels {
    fun ko(command: VehicleCommand): String = when (command) {
        VehicleCommand.Lock -> "잠금"
        VehicleCommand.Unlock -> "잠금 해제"
        VehicleCommand.ClimateOn -> "공조 ON"
        VehicleCommand.ClimateOff -> "공조 OFF"
        VehicleCommand.Trunk -> "트렁크"
        VehicleCommand.Frunk -> "프렁크"
        VehicleCommand.Flash -> "라이트"
        VehicleCommand.Honk -> "경적"
        VehicleCommand.SentryOn -> "Sentry ON"
        VehicleCommand.SentryOff -> "Sentry OFF"
        VehicleCommand.DogMode -> "Dog Mode"
        VehicleCommand.CampMode -> "Camp Mode"
        VehicleCommand.WindowVent -> "환기"
        VehicleCommand.ChargePortOpen -> "충전포트 열기"
        VehicleCommand.ChargePortClose -> "충전포트 닫기"
    }
}
