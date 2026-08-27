package com.myt.phase3

/** M44 — Tesla Live Camera API (Phase 3 scaffold). */
interface LiveCameraClient {
    suspend fun status(vin: String): LiveCameraStatus
    suspend fun startStream(vin: String): Result<Unit>
}

class StubLiveCameraClient : LiveCameraClient {
    override suspend fun status(vin: String): LiveCameraStatus =
        LiveCameraStatus(
            available = false,
            message = "Live Camera는 차량 깨우기 + Fleet API 스트림 연동 후 사용 가능합니다 (M44).",
        )

    override suspend fun startStream(vin: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Live Camera not configured"))
}
