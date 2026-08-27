package com.myt.phase3

/**
 * Demo live camera — synthetic frames without Tesla stream (M44).
 */
class DemoLiveCameraClient : LiveCameraClient {
    private var streaming = false

    override suspend fun status(vin: String): LiveCameraStatus =
        LiveCameraStatus(
            available = true,
            message = if (streaming) {
                val shortVin = vin.takeLast(6).ifBlank { "------" }
                "데모 스트림 재생 중 · VIN $shortVin"
            } else {
                "데모 카메라 준비됨 · 전/후/좌/우 프레임 (Tesla Live Camera 아님)"
            },
        )

    override suspend fun startStream(vin: String): Result<Unit> {
        streaming = true
        return Result.success(Unit)
    }

    fun stopStream() {
        streaming = false
    }

    fun demoFrames(): List<DemoCameraFrame> = listOf(
        DemoCameraFrame("전방", 0xFF1B3A4B.toInt()),
        DemoCameraFrame("후방", 0xFF2D1B4B.toInt()),
        DemoCameraFrame("좌측", 0xFF1B4B2D.toInt()),
        DemoCameraFrame("우측", 0xFF4B3A1B.toInt()),
    )
}

data class DemoCameraFrame(
    val label: String,
    val colorArgb: Int,
)
