package com.myt.domain.usecase

import com.myt.config.TeslaConfig
import com.myt.data.poi.PoiNationalBundle
import com.myt.data.poi.SqlPoiRepository
import com.myt.data.poi.PoiOtaCsvParser
import com.myt.domain.model.SpeedCamera
import com.myt.data.settings.optionalString
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

sealed class PoiSyncOutcome {
    data object NoUrl : PoiSyncOutcome()
    data object UpToDate : PoiSyncOutcome()
    data class Updated(val count: Int) : PoiSyncOutcome()
    data class Failed(val message: String) : PoiSyncOutcome()
}

class PoiOtaSyncUseCase(
    private val httpClient: HttpClient,
    private val sqlPoiRepository: SqlPoiRepository,
    private val settings: Settings,
    private val parser: PoiOtaCsvParser = PoiOtaCsvParser,
    private val clock: Clock = Clock.System,
) {
    /**
     * Checks remote CSV fingerprint (ETag / Last-Modified) and downloads when newer.
     * Ignores weekly throttle when the remote fingerprint changed.
     */
    suspend fun syncIfUpdateAvailable(config: TeslaConfig): PoiSyncOutcome {
        val url = config.poiOtaCsvUrl.trim()
        if (url.isBlank()) return PoiSyncOutcome.NoUrl

        return runCatching {
            val remoteFp = fetchRemoteFingerprint(url)
            if (remoteFp != null) {
                settings.putString(KEY_REMOTE_FINGERPRINT, remoteFp)
            }
            val localFp = settings.optionalString(KEY_LOCAL_FINGERPRINT)
            val installedCount = countCameras()
            val upToDate = remoteFp != null &&
                remoteFp == localFp &&
                installedCount > PoiNationalBundle.DEMO_THRESHOLD_COUNT
            if (upToDate) {
                settings.putBoolean(KEY_AUTO_SYNC_FAILED, false)
                return PoiSyncOutcome.UpToDate
            }

            val nowMs = clock.now().toEpochMilliseconds()
            val lastMs = settings.getLongOrNull(KEY_LAST_SYNC_MS)
            val sameRemote = remoteFp != null && remoteFp == localFp
            if (sameRemote && lastMs != null && nowMs - lastMs < MIN_INTERVAL_MS) {
                return PoiSyncOutcome.UpToDate
            }

            val csvText = httpClient.get(url).bodyAsText()
            val cameras = parser.parseSpeedCameras(csvText)
            withContext(Dispatchers.Default) { upsertAll(cameras) }
            settings.putLong(KEY_LAST_SYNC_MS, nowMs)
            if (remoteFp != null) {
                settings.putString(KEY_LOCAL_FINGERPRINT, remoteFp)
            } else {
                settings.putString(KEY_LOCAL_FINGERPRINT, "count:${cameras.size}:${csvText.hashCode()}")
            }
            settings.putBoolean(KEY_AUTO_SYNC_FAILED, false)
            PoiSyncOutcome.Updated(cameras.size)
        }.getOrElse { error ->
            settings.putBoolean(KEY_AUTO_SYNC_FAILED, true)
            PoiSyncOutcome.Failed(error.message ?: error.toString())
        }
    }

    /** Manual sync from settings / banner (always downloads). */
    suspend fun syncNow(config: TeslaConfig): Result<Int> {
        val url = config.poiOtaCsvUrl.trim()
        if (url.isBlank()) {
            return Result.failure(IllegalStateException("poi OTA URL not configured"))
        }
        return runCatching {
            val remoteFp = fetchRemoteFingerprint(url)
            val csvText = httpClient.get(url).bodyAsText()
            val cameras = parser.parseSpeedCameras(csvText)
            withContext(Dispatchers.Default) { upsertAll(cameras) }
            val nowMs = clock.now().toEpochMilliseconds()
            settings.putLong(KEY_LAST_SYNC_MS, nowMs)
            if (remoteFp != null) {
                settings.putString(KEY_REMOTE_FINGERPRINT, remoteFp)
                settings.putString(KEY_LOCAL_FINGERPRINT, remoteFp)
            } else {
                val fp = "count:${cameras.size}:${csvText.hashCode()}"
                settings.putString(KEY_LOCAL_FINGERPRINT, fp)
                settings.putString(KEY_REMOTE_FINGERPRINT, fp)
            }
            settings.putBoolean(KEY_AUTO_SYNC_FAILED, false)
            cameras.size
        }.onFailure {
            settings.putBoolean(KEY_AUTO_SYNC_FAILED, true)
        }
    }

    private suspend fun fetchRemoteFingerprint(url: String): String? {
        val response = httpClient.head(url)
        if (!response.status.isSuccess()) return null
        return response.headers["ETag"]
            ?: response.headers["Last-Modified"]
            ?: response.headers["Content-Length"]?.let { "len:$it" }
    }

    private fun countCameras(): Long = sqlPoiRepository.cameraCount()

    private fun upsertAll(cameras: List<SpeedCamera>) {
        sqlPoiRepository.bulkUpsert(cameras)
        sqlPoiRepository.refreshSpatialIndex()
    }

    companion object {
        const val KEY_LAST_SYNC_MS = "poi_ota_last_sync_ms_v1"
        const val KEY_LOCAL_FINGERPRINT = "poi_ota_local_fp_v1"
        const val KEY_REMOTE_FINGERPRINT = "poi_ota_remote_fp_v1"
        const val KEY_AUTO_SYNC_FAILED = "poi_ota_auto_sync_failed_v1"
        private const val MIN_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
