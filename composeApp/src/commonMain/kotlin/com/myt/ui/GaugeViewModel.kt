package com.myt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myt.config.HaIntegrationConfig
import com.myt.config.HaIntegrationConfigStore
import com.myt.config.TeslaConfig
import com.myt.config.TeslaConfigStore
import com.myt.data.auth.OAuthCallbackBus
import com.myt.data.poi.PoiBootstrapUseCase
import com.myt.data.poi.SqlPoiRepository
import com.myt.domain.VehicleConfig
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeDisplayPrefs
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.PoiDataStatus
import com.myt.domain.model.SpeedCamAlert
import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass
import com.myt.domain.quota.QuotaSnapshot
import com.myt.domain.quota.emptyQuotaSnapshot
import com.myt.domain.repository.PoiRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.domain.usecase.AdaptiveLayoutUseCase
import com.myt.domain.usecase.AuthUseCase
import com.myt.domain.usecase.FleetQuotaUseCase
import com.myt.domain.usecase.PoiDataStatusUseCase
import com.myt.domain.usecase.PoiOtaSyncUseCase
import com.myt.domain.usecase.PoiSyncOutcome
import com.myt.domain.usecase.PresenceUseCase
import com.myt.domain.usecase.RoadSnapUseCase
import com.myt.domain.usecase.SpeedCamUseCase
import com.myt.domain.usecase.TelemetryUseCase
import com.myt.domain.usecase.UiFreshNeed
import com.myt.domain.usecase.VoiceCommandResult
import com.myt.domain.usecase.VoiceCommandUseCase
import com.myt.domain.usecase.VoiceNavUseCase
import com.myt.domain.automation.LocalAutomationEngine
import com.myt.phase2.WatchCompanionBridge
import com.myt.phase2.WatchGaugePayload
import com.myt.phase3.FsdAnalytics
import com.myt.phase3.HaRestStateBridge
import com.myt.platform.ScreenPlatform
import com.myt.ui.map.LiveMapMarker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GaugeViewModel(
    private val telemetryUseCase: TelemetryUseCase,
    private val presenceUseCase: PresenceUseCase,
    private val speedCamUseCase: SpeedCamUseCase,
    private val layoutUseCase: AdaptiveLayoutUseCase,
    private val voiceNavUseCase: VoiceNavUseCase,
    private val voiceCommandUseCase: VoiceCommandUseCase,
    private val authUseCase: AuthUseCase,
    private val settingsRepository: SettingsRepository,
    private val quotaUseCase: FleetQuotaUseCase,
    private val teslaConfigStore: TeslaConfigStore,
    private val screenPlatform: ScreenPlatform,
    private val poiOtaSyncUseCase: PoiOtaSyncUseCase,
    private val poiBootstrapUseCase: PoiBootstrapUseCase,
    private val poiDataStatusUseCase: PoiDataStatusUseCase,
    private val poiRepository: PoiRepository,
    private val sqlPoiRepository: SqlPoiRepository,
    private val roadSnapUseCase: RoadSnapUseCase,
    private val haRestStateBridge: HaRestStateBridge,
    private val haIntegrationConfigStore: HaIntegrationConfigStore,
    private val watchCompanionBridge: WatchCompanionBridge,
    private val localAutomationEngine: LocalAutomationEngine,
) : ViewModel() {

    val gaugeState: StateFlow<GaugeState> = telemetryUseCase.gaugeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GaugeState())

    val speedCamAlert: StateFlow<SpeedCamAlert?> = speedCamUseCase.alert
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val quotaSnapshot: StateFlow<QuotaSnapshot> = quotaUseCase.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyQuotaSnapshot())

    val teslaConfig: StateFlow<TeslaConfig> = teslaConfigStore.config
    val haIntegrationConfig: StateFlow<HaIntegrationConfig> = haIntegrationConfigStore.config

    fun saveHaConfig(updated: HaIntegrationConfig) {
        haIntegrationConfigStore.save(updated)
    }

    private val _speedUnitKmh = MutableStateFlow(true)
    val speedUnitKmh: StateFlow<Boolean> = _speedUnitKmh.asStateFlow()

    private val _gaugePrefs = MutableStateFlow(GaugeDisplayPrefs())
    val gaugePrefs: StateFlow<GaugeDisplayPrefs> = _gaugePrefs.asStateFlow()

    private val _layoutConfig = MutableStateFlow(LayoutConfig.SinglePane)
    val layoutConfig: StateFlow<LayoutConfig> = _layoutConfig.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _configuredVin = MutableStateFlow<String?>(null)
    val configuredVin: StateFlow<String?> = _configuredVin.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    private var autoLoginStarted = false
    private var poiOtaSyncStarted = false

    private val _poiDataStatus = MutableStateFlow(
        PoiDataStatus(
            installedCount = 0,
            bundledVersion = "",
            bundledCount = 0,
        ),
    )
    val poiDataStatus: StateFlow<PoiDataStatus> = _poiDataStatus.asStateFlow()

    private val _poiSyncInProgress = MutableStateFlow(false)
    val poiSyncInProgress: StateFlow<Boolean> = _poiSyncInProgress.asStateFlow()

    private val _mapDisplayLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapDisplayLocation: StateFlow<Pair<Double, Double>?> = _mapDisplayLocation.asStateFlow()

    private val _mapMarkers = MutableStateFlow<List<LiveMapMarker>>(emptyList())
    val mapMarkers: StateFlow<List<LiveMapMarker>> = _mapMarkers.asStateFlow()

    private var lastMapSnapKey: String? = null
    private var lastHaPublishMs: Long = 0L
    private var lastWatchPushMs: Long = 0L

    val oauthConfigured: Boolean get() = teslaConfigStore.current().isOAuthConfigured()

    val bluetoothPresent: StateFlow<Boolean> = presenceUseCase.isVehiclePresent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = gaugeState
        .combine(gaugeState) { state, _ -> state.connection }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionStatus.Disconnected)

    init {
        presenceUseCase.startMonitoring()
        localAutomationEngine.start(gaugeState)
        viewModelScope.launch { quotaUseCase.hydrate() }
        viewModelScope.launch {
            poiBootstrapUseCase.ensureSeeded()
            runPoiAutoSync()
        }
        viewModelScope.launch {
            _speedUnitKmh.value = settingsRepository.getSpeedUnitKmh()
            _gaugePrefs.value = settingsRepository.getGaugeDisplayPrefs()
            telemetryUseCase.setPreferDeviceSpeed(_gaugePrefs.value.preferDeviceSpeed)
            _darkTheme.value = settingsRepository.isDarkTheme()
        }
        refreshAuthState()
        viewModelScope.launch {
            gaugeState.collect { state ->
                speedCamUseCase.evaluateFromGaugeState(state)
                updateLayout()
                publishHaIfNeeded(state)
                pushWatchIfNeeded(state)
                FsdAnalytics.recordDemoDriveTick(state.speedKmh, state.isSimulated)
            }
        }
        viewModelScope.launch {
            combine(gaugeState, speedCamUseCase.alert) { state, alert -> state to alert }
                .collect { (state, alert) -> updateMapOverlays(state, alert) }
        }
        viewModelScope.launch {
            delay(POI_AUTO_SYNC_INITIAL_DELAY_MS)
            while (isActive) {
                runPoiAutoSync()
                delay(POI_AUTO_SYNC_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            OAuthCallbackBus.codes.collect {
                refreshAuthState()
            }
        }
    }

    fun refreshAuthState() {
        viewModelScope.launch {
            _isAuthenticated.value = authUseCase.isAuthenticated()
            val storedVin = settingsRepository.getVin()?.takeIf { it.isNotBlank() }
            val configVin = teslaConfigStore.current().vehicleVin.takeIf { it.isNotBlank() }
            val vin = storedVin ?: configVin
            if (storedVin.isNullOrBlank() && !vin.isNullOrBlank()) {
                settingsRepository.setVin(vin)
            }
            _configuredVin.value = vin
            val bakedIn = teslaConfigStore.current().isOAuthConfigured() && !vin.isNullOrBlank()
            if (bakedIn && !settingsRepository.isOnboardingComplete()) {
                authUseCase.completeOnboarding(vin)
            }
            val onboarded = bakedIn || (settingsRepository.isOnboardingComplete() && !vin.isNullOrBlank())
            _onboardingComplete.value = onboarded
            if (onboarded && !vin.isNullOrBlank()) {
                startGaugeSession(vin)
                if (!poiOtaSyncStarted) {
                    poiOtaSyncStarted = true
                    viewModelScope.launch { runPoiAutoSync() }
                }
            }
            _sessionReady.value = true
            if (!_isAuthenticated.value && teslaConfigStore.current().isOAuthConfigured() && !autoLoginStarted) {
                autoLoginStarted = true
                startTeslaLogin()
            }
        }
    }

    fun startTeslaLogin() {
        runCatching { authUseCase.startTeslaLogin() }
    }

    fun startGaugeSession(vin: String) {
        telemetryUseCase.startPolling(viewModelScope, VehicleConfig(vin = vin))
        screenPlatform.keepScreenOn(true)
    }

    fun stopGaugeSession() {
        telemetryUseCase.stopPolling()
        screenPlatform.keepScreenOn(false)
    }

    fun completeOnboarding(vin: String) {
        viewModelScope.launch {
            authUseCase.completeOnboarding(vin)
            _configuredVin.value = vin
            startGaugeSession(vin)
        }
    }

    fun setSpeedUnitKmh(useKmh: Boolean) {
        _speedUnitKmh.value = useKmh
        viewModelScope.launch {
            settingsRepository.setSpeedUnitKmh(useKmh)
        }
    }

    fun updateGaugePrefs(prefs: GaugeDisplayPrefs) {
        _gaugePrefs.value = prefs
        telemetryUseCase.setPreferDeviceSpeed(prefs.preferDeviceSpeed)
        viewModelScope.launch {
            settingsRepository.setGaugeDisplayPrefs(prefs)
        }
        updateLayout()
    }

    fun saveTeslaConfig(updated: TeslaConfig) {
        val previousUrl = teslaConfigStore.current().poiOtaCsvUrl
        val normalized = updated.copy(vehicleVin = updated.vehicleVin.trim().uppercase())
        teslaConfigStore.save(normalized)
        viewModelScope.launch {
            if (normalized.vehicleVin.isNotBlank()) {
                settingsRepository.setVin(normalized.vehicleVin)
                _configuredVin.value = normalized.vehicleVin
                startGaugeSession(normalized.vehicleVin)
            }
            _isAuthenticated.value = authUseCase.isAuthenticated()
            if (normalized.poiOtaCsvUrl.trim() != previousUrl.trim()) {
                runPoiAutoSync()
            }
        }
    }

    suspend fun executeVoiceCommand(): VoiceCommandResult = voiceCommandUseCase.listenAndExecute()

    suspend fun recognizeVoiceDestination(): String? {
        return when (val result = voiceNavUseCase.recognizeDestination()) {
            is com.myt.domain.usecase.VoiceNavResult.Recognized -> result.destination
            else -> null
        }
    }

    fun sendVoiceDestination(destination: String) {
        viewModelScope.launch {
            voiceNavUseCase.sendDestination(destination)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        viewModelScope.launch {
            settingsRepository.setDarkTheme(enabled)
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            authUseCase.ensureFreshAccessToken()
            refreshVehicleState()
        }
    }

    fun refreshVehicleState() {
        val vin = _configuredVin.value ?: return
        viewModelScope.launch {
            telemetryUseCase.refreshOnce(vin)
        }
    }

    fun requestFreshData(need: UiFreshNeed) {
        val vin = _configuredVin.value ?: return
        viewModelScope.launch {
            telemetryUseCase.refreshForUiNeed(vin, need)
        }
    }

    fun refreshPoiDataStatus() {
        _poiDataStatus.value = poiDataStatusUseCase.current(teslaConfigStore.current())
    }

    fun syncPoiData() {
        if (_poiSyncInProgress.value) return
        viewModelScope.launch {
            _poiSyncInProgress.value = true
            val result = poiOtaSyncUseCase.syncNow(teslaConfigStore.current())
            if (result.isSuccess) {
                applyPoiDataChanged()
            } else {
                refreshPoiDataStatus()
            }
            _poiSyncInProgress.value = false
        }
    }

    private suspend fun runPoiAutoSync() {
        if (_poiSyncInProgress.value) return
        _poiSyncInProgress.value = true
        val outcome = poiOtaSyncUseCase.syncIfUpdateAvailable(teslaConfigStore.current())
        if (outcome is PoiSyncOutcome.Updated) {
            applyPoiDataChanged()
        } else {
            refreshPoiDataStatus()
        }
        _poiSyncInProgress.value = false
    }

    private fun applyPoiDataChanged() {
        sqlPoiRepository.refreshSpatialIndex()
        val state = gaugeState.value
        speedCamUseCase.clearAlert()
        speedCamUseCase.evaluateFromGaugeState(state)
        refreshPoiDataStatus()
        viewModelScope.launch {
            updateMapOverlays(state, speedCamUseCase.alert.value)
        }
    }

    private suspend fun updateMapOverlays(state: GaugeState, alert: SpeedCamAlert?) {
        val lat = state.latitude
        val lng = state.longitude
        if (lat == null || lng == null) {
            _mapDisplayLocation.value = null
            _mapMarkers.value = emptyList()
            lastMapSnapKey = null
            return
        }
        val snapKey = "${"%.4f".format(lat)}:${"%.4f".format(lng)}"
        if (snapKey != lastMapSnapKey) {
            lastMapSnapKey = snapKey
            _mapDisplayLocation.value = roadSnapUseCase.snap(lat, lng)
        }
        val snapped = _mapDisplayLocation.value ?: (lat to lng)
        val radius = mapOverlayRadiusMeters(state, alert)
        val cameras = poiRepository.findNearbyCameras(snapped.first, snapped.second, radius)
        val markers = cameras.map { cam ->
            LiveMapMarker(
                latitude = cam.latitude,
                longitude = cam.longitude,
                kind = "camera",
                label = "${cam.speedLimitKmh} · ${cam.roadName ?: "단속"}",
            )
        }.toMutableList()
        val nav = state.navigation
        if (nav?.isActive == true && nav.destinationLatitude != null && nav.destinationLongitude != null) {
            markers += LiveMapMarker(
                latitude = nav.destinationLatitude,
                longitude = nav.destinationLongitude,
                kind = "dest",
                label = nav.destinationName ?: "목적지",
            )
        }
        _mapMarkers.value = markers
    }

    private fun mapOverlayRadiusMeters(state: GaugeState, alert: SpeedCamAlert?): Int = when {
        alert != null -> 500
        state.navigation?.isActive == true -> 800
        state.gear == Gear.DRIVE || state.gear == Gear.REVERSE || state.speedKmh >= 3f -> 750
        else -> 1_000
    }

    private fun publishHaIfNeeded(state: GaugeState) {
        val config = haIntegrationConfigStore.current()
        if (!config.enabled) return
        val vin = _configuredVin.value ?: return
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - lastHaPublishMs < HA_PUBLISH_INTERVAL_MS) return
        lastHaPublishMs = now
        viewModelScope.launch {
            haRestStateBridge.publishGaugeState(config, vin, state)
        }
    }

    private fun pushWatchIfNeeded(state: GaugeState) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - lastWatchPushMs < WATCH_PUSH_INTERVAL_MS) return
        lastWatchPushMs = now
        viewModelScope.launch {
            watchCompanionBridge.push(
                WatchGaugePayload(
                    socPercent = state.socPercent.toInt(),
                    speedKmh = state.speedKmh.toInt(),
                    rangeKm = state.rangeKm.toInt(),
                    locked = state.locked,
                    isCharging = state.charging?.isCharging == true,
                    updatedAtMs = now,
                ),
            )
        }
    }

    private fun updateLayout() {
        val widthDp = screenPlatform.currentWidthDp()
        val heightDp = screenPlatform.currentHeightDp()
        val widthClass = when {
            widthDp < 600 -> WindowWidthSizeClass.Compact
            widthDp < 840 -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
        val heightClass = when {
            heightDp < 480 -> WindowHeightSizeClass.Compact
            heightDp < 900 -> WindowHeightSizeClass.Medium
            else -> WindowHeightSizeClass.Expanded
        }
        _layoutConfig.value = layoutUseCase.computeLayout(
            widthClass,
            heightClass,
            _gaugePrefs.value.layoutMode,
        )
    }

    override fun onCleared() {
        stopGaugeSession()
        presenceUseCase.stopMonitoring()
        localAutomationEngine.stop()
        super.onCleared()
    }

    companion object {
        private const val POI_AUTO_SYNC_INITIAL_DELAY_MS = 60_000L
        private const val POI_AUTO_SYNC_INTERVAL_MS = 4L * 60 * 60 * 1000
        private const val HA_PUBLISH_INTERVAL_MS = 30_000L
        private const val WATCH_PUSH_INTERVAL_MS = 15_000L
    }
}
