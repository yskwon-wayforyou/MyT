package com.myt.phase3

/**
 * FSD-related analytics (M42). Demo mode estimates minutes from simulated drive.
 */
data class FsdUsageSummary(
    val autopilotMinutes: Int,
    val interventions: Int,
    val message: String,
)

object FsdAnalytics {
    private var demoDriveSeconds: Int = 0

    fun recordDemoDriveTick(speedKmh: Float, isSimulated: Boolean) {
        if (isSimulated && speedKmh >= 30f) {
            demoDriveSeconds += 2
        }
    }

    fun summary(): FsdUsageSummary {
        val minutes = demoDriveSeconds / 60
        return if (minutes <= 0) {
            FsdUsageSummary(
                autopilotMinutes = 0,
                interventions = 0,
                message = "시뮬 주행(≥30km/h)이 쌓이면 Autopilot 추정 분이 표시됩니다. 실 FSD는 Fleet Telemetry 연동 후.",
            )
        } else {
            FsdUsageSummary(
                autopilotMinutes = minutes,
                interventions = (minutes / 15).coerceAtLeast(0),
                message = "데모 추정 · 시뮬 ${minutes}분 상당 (실 Autopilot 필드 아님)",
            )
        }
    }

    fun placeholder(): FsdUsageSummary = summary()
}
