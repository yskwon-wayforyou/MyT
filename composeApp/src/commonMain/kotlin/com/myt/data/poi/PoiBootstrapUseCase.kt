package com.myt.data.poi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds speed cameras from bundled CSV (M5) when DB is empty.
 * Falls back to inline demo list if bundle parse yields nothing.
 */
class PoiBootstrapUseCase(
    private val sql: SqlPoiRepository,
) {
    @Volatile
    private var seeded = false

    suspend fun ensureSeeded() {
        if (seeded) return
        withContext(Dispatchers.Default) {
            if (sql.cameraCount() > 0L) {
                seeded = true
                return@withContext
            }
            val bundle = PoiOtaCsvParser.parseSpeedCameras(PoiNationalBundle.CSV)
            val cameras = bundle.ifEmpty { PoiSeedRepository.demoCameras() }
            sql.seedIfEmpty(cameras)
            seeded = true
        }
    }
}
