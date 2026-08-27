package com.myt.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.history.HistoryTab
import com.myt.domain.ledger.ChargeLedgerClassifier
import com.myt.domain.ledger.ChargeLedgerSummary
import com.myt.domain.usecase.HistoryUseCase
import com.myt.phase3.BatteryAnalyticsUseCase
import com.myt.phase3.BatteryHealthReport
import com.myt.phase3.CarbonBadgeState
import com.myt.phase3.CarbonBadgeUseCase
import com.myt.phase3.Co2Calculator
import com.myt.phase3.Co2SavingsSummary
import com.myt.phase3.DataPortability
import com.myt.phase3.DemoCameraFrame
import com.myt.phase3.DemoLiveCameraClient
import com.myt.phase3.LiveCameraClient
import com.myt.phase3.LiveCameraStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val historyUseCase: HistoryUseCase,
    private val batteryAnalytics: BatteryAnalyticsUseCase,
    private val carbonBadge: CarbonBadgeUseCase,
    private val dataPortability: DataPortability,
    private val liveCameraClient: LiveCameraClient,
) : ViewModel() {
    private val _batteryReport = MutableStateFlow(BatteryHealthReport(emptyList(), null))
    val batteryReport: StateFlow<BatteryHealthReport> = _batteryReport.asStateFlow()

    private val _carbonBadge = MutableStateFlow(
        CarbonBadgeState(
            tier = com.myt.phase3.CarbonBadgeTier.Seedling,
            co2SavedKg = 0f,
            nextTier = com.myt.phase3.CarbonBadgeTier.Commuter,
            progressToNext = 0f,
        ),
    )
    val carbonBadgeState: StateFlow<CarbonBadgeState> = _carbonBadge.asStateFlow()

    private val _co2Summary = MutableStateFlow(Co2SavingsSummary(0f, 0f, 0f))
    val co2Summary: StateFlow<Co2SavingsSummary> = _co2Summary.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _liveCamera = MutableStateFlow(LiveCameraStatus(false, ""))
    val liveCamera: StateFlow<LiveCameraStatus> = _liveCamera.asStateFlow()

    private val _cameraFrames = MutableStateFlow<List<DemoCameraFrame>>(emptyList())
    val cameraFrames: StateFlow<List<DemoCameraFrame>> = _cameraFrames.asStateFlow()

    private val _chargeLedger = MutableStateFlow(
        ChargeLedgerSummary("이번 달", 0f, 0f, emptyMap()),
    )
    val chargeLedger: StateFlow<ChargeLedgerSummary> = _chargeLedger.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val filter = HistoryFilterState(period = HistoryPeriodFilter.All)
            val trips = historyUseCase.trips(filter)
            val charges = historyUseCase.chargeSessions(
                HistoryFilterState(tab = HistoryTab.Charging, period = HistoryPeriodFilter.Days30),
            )
            _batteryReport.value = batteryAnalytics.report(filter)
            _carbonBadge.value = carbonBadge.state(filter)
            _co2Summary.value = Co2Calculator.summarizeTrips(trips)
            _chargeLedger.value = ChargeLedgerClassifier.summarize(charges, "최근 30일")
        }
    }

    fun exportTrips(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = dataPortability.exportTripsCsv()
            _exportMessage.value = "주행 CSV ${csv.lines().size - 1}건 준비됨"
            onResult(csv)
        }
    }

    fun exportCharges(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = dataPortability.exportChargesCsv()
            _exportMessage.value = "충전 CSV ${csv.lines().size - 1}건 준비됨"
            onResult(csv)
        }
    }

    fun loadLiveCameraStatus(vin: String?) {
        viewModelScope.launch {
            _liveCamera.value = liveCameraClient.status(vin ?: "")
            val demo = liveCameraClient as? DemoLiveCameraClient
            if (demo != null && _cameraFrames.value.isNotEmpty()) {
                _cameraFrames.value = demo.demoFrames()
            }
        }
    }

    fun startDemoCamera(vin: String?) {
        viewModelScope.launch {
            val result = liveCameraClient.startStream(vin ?: "")
            _liveCamera.value = liveCameraClient.status(vin ?: "")
            if (result.isSuccess) {
                val demo = liveCameraClient as? DemoLiveCameraClient
                _cameraFrames.value = demo?.demoFrames().orEmpty()
            }
        }
    }

    fun stopDemoCamera(vin: String?) {
        viewModelScope.launch {
            (liveCameraClient as? DemoLiveCameraClient)?.stopStream()
            _cameraFrames.value = emptyList()
            _liveCamera.value = liveCameraClient.status(vin ?: "")
        }
    }

    fun importTessieCsv(csv: String, defaultVin: String, onDone: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            val result = dataPortability.importFromTessieCsv(csv, defaultVin)
            if (result.isSuccess) {
                _exportMessage.value = "Tessie import ${result.getOrNull()}건 완료"
                refresh()
            } else {
                _exportMessage.value = "Import 실패: ${result.exceptionOrNull()?.message}"
            }
            onDone(result)
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }
}
