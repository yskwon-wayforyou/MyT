package com.myt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myt.config.TeslaConfig
import com.myt.data.auth.OAuthCallbackBus
import com.myt.domain.VehicleConfig
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.SpeedCamAlert
import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass
import com.myt.domain.usecase.AdaptiveLayoutUseCase
import com.myt.domain.usecase.AuthUseCase
import com.myt.domain.usecase.PresenceUseCase
import com.myt.domain.usecase.SpeedCamUseCase
import com.myt.domain.usecase.TelemetryUseCase
import com.myt.domain.usecase.VoiceNavUseCase
import com.myt.domain.usecase.VoiceNavResult
import com.myt.domain.repository.SettingsRepository
import com.myt.platform.ScreenPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GaugeViewModel(
    private val telemetryUseCase: TelemetryUseCase,
    private val presenceUseCase: PresenceUseCase,
    private val speedCamUseCase: SpeedCamUseCase,
    private val layoutUseCase: AdaptiveLayoutUseCase,
    private val voiceNavUseCase: VoiceNavUseCase,
    private val authUseCase: AuthUseCase,
    private val settingsRepository: SettingsRepository,
    private val screenPlatform: ScreenPlatform,
    private val teslaConfig: TeslaConfig,
) : ViewModel() {

    val gaugeState: StateFlow<GaugeState> = telemetryUseCase.gaugeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GaugeState())

    val speedCamAlert: StateFlow<SpeedCamAlert?> = speedCamUseCase.alert
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _layoutConfig = MutableStateFlow(LayoutConfig.SinglePane)
    val layoutConfig: StateFlow<LayoutConfig> = _layoutConfig.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _configuredVin = MutableStateFlow<String?>(null)
    val configuredVin: StateFlow<String?> = _configuredVin.asStateFlow()

    val oauthConfigured: Boolean get() = teslaConfig.isOAuthConfigured()

    val connectionStatus: StateFlow<ConnectionStatus> = gaugeState
        .combine(gaugeState) { state, _ -> state.connection }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionStatus.Disconnected)

    init {
        presenceUseCase.startMonitoring()
        refreshAuthState()
        viewModelScope.launch {
            gaugeState.collect { state ->
                speedCamUseCase.evaluateFromGaugeState(state)
                updateLayout()
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
            _configuredVin.value = settingsRepository.getVin() ?: teslaConfig.vehicleVin.takeIf { it.isNotBlank() }
        }
    }

    fun startTeslaLogin() {
        authUseCase.startTeslaLogin()
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
        viewModelScope.launch {
            settingsRepository.setSpeedUnitKmh(useKmh)
        }
    }

    suspend fun recognizeVoiceDestination(): String? {
        return when (val result = voiceNavUseCase.recognizeDestination()) {
            is VoiceNavResult.Recognized -> result.destination
            else -> null
        }
    }

    fun sendVoiceDestination(destination: String) {
        viewModelScope.launch {
            voiceNavUseCase.sendDestination(destination)
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
        _layoutConfig.value = layoutUseCase.computeLayout(widthClass, heightClass)
    }

    override fun onCleared() {
        stopGaugeSession()
        presenceUseCase.stopMonitoring()
        super.onCleared()
    }
}
