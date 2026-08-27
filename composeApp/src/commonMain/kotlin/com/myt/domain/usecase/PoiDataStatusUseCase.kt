package com.myt.domain.usecase

import com.myt.config.TeslaConfig
import com.myt.data.poi.PoiNationalBundle
import com.myt.data.poi.SqlPoiRepository
import com.myt.domain.model.PoiDataStatus
import com.myt.data.settings.optionalString
import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock

class PoiDataStatusUseCase(
    private val sqlPoiRepository: SqlPoiRepository,
    private val settings: Settings,
    private val clock: Clock = Clock.System,
) {
    fun current(config: TeslaConfig): PoiDataStatus {
        val count = sqlPoiRepository.cameraCount().toInt()
        val lastSync = settings.getLongOrNull(PoiOtaSyncUseCase.KEY_LAST_SYNC_MS)
        val localFp = settings.optionalString(PoiOtaSyncUseCase.KEY_LOCAL_FINGERPRINT)
        val remoteFp = settings.optionalString(PoiOtaSyncUseCase.KEY_REMOTE_FINGERPRINT)
        val autoSyncFailed = settings.getBoolean(PoiOtaSyncUseCase.KEY_AUTO_SYNC_FAILED, false)
        val otaConfigured = config.poiOtaCsvUrl.trim().isNotBlank()
        val isDemo = count <= PoiNationalBundle.DEMO_THRESHOLD_COUNT

        val isLatest = when {
            !otaConfigured -> false
            autoSyncFailed -> false
            isDemo -> false
            localFp != null && remoteFp != null && localFp == remoteFp -> true
            !isDemo && lastSync != null -> true
            else -> false
        }

        val updateReason = when {
            !otaConfigured && isDemo ->
                "데모 ${count}건 · 설정에서 CSV URL 등록 필요 (${PoiNationalBundle.NATIONAL_ESTIMATE_LABEL})"
            autoSyncFailed ->
                "자동 업데이트 실패 · 네트워크·URL 확인 후 수동 재시도"
            otaConfigured && !isLatest ->
                "최신 전국 데이터 아님 · 경찰청 CSV 갱신 권장"
            else -> null
        }

        // Manual update is surfaced in More hub only — never as a map overlay.
        val manualUpdateRequired = when {
            autoSyncFailed -> true
            otaConfigured && !isLatest -> true
            else -> false
        }

        return PoiDataStatus(
            installedCount = count,
            bundledVersion = PoiNationalBundle.VERSION,
            bundledCount = PoiNationalBundle.BUNDLED_COUNT,
            lastSyncEpochMs = lastSync,
            isDemoSubset = isDemo,
            otaUrlConfigured = otaConfigured,
            isLatest = isLatest,
            manualUpdateRequired = manualUpdateRequired,
            updateReason = updateReason,
            autoSyncFailed = autoSyncFailed,
        )
    }
}
