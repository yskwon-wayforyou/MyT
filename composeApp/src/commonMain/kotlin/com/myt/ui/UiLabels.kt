package com.myt.ui

import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.TelemetrySource

/**
 * 사용자-facing UI 문자열. 코드·로그의 Fleet 등 내부 용어와 분리한다.
 */
object UiLabels {
    const val teslaApi = "테슬라API"
    const val teslaApiShort = "API"

    fun connection(status: ConnectionStatus): String = when (status) {
        ConnectionStatus.FleetConnected -> teslaApi
        ConnectionStatus.BluetoothOnly -> "블루투스"
        ConnectionStatus.Sleeping -> "대기"
        ConnectionStatus.QuotaHold -> "한도"
        ConnectionStatus.Error -> "오류"
        ConnectionStatus.Disconnected -> "오프라인"
    }

    fun connectionShort(status: ConnectionStatus): String = when (status) {
        ConnectionStatus.FleetConnected -> teslaApiShort
        ConnectionStatus.BluetoothOnly -> "BT"
        ConnectionStatus.Sleeping -> "대기"
        ConnectionStatus.QuotaHold -> "한도"
        ConnectionStatus.Error -> "ERR"
        ConnectionStatus.Disconnected -> "OFF"
    }

    fun telemetrySource(source: TelemetrySource): String = when (source) {
        TelemetrySource.Device -> "GPS"
        TelemetrySource.Degraded -> "GPS…"
        TelemetrySource.Fleet -> teslaApiShort
        TelemetrySource.Cache -> "캐시"
        TelemetrySource.None -> ""
    }

    const val mapLocationWaiting = "위치 수신 중"
    const val mapLocationHint = "테슬라API·GPS 좌표 필요"
    const val mapLoading = "지도 로딩 중… (OpenStreetMap)"
    const val simulationTesting = "시뮬레이션 테스트 중"
    const val speedCamDataTitle = "과속카메라 데이터"
    const val speedCamUpdateDefault = "최신 전국 데이터로 업데이트를 권장합니다"
    const val speedCamUpdateNow = "지금 업데이트"
    const val speedCamOpenSettings = "설정에서 URL"
    const val laneTurnUnsupported = "차선·회전은 테슬라API 미지원"
}
