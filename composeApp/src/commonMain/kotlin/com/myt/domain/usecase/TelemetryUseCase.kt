package com.myt.domain.usecase

import com.myt.debug.DebugLogger
import com.myt.domain.VehicleConfig
import com.myt.domain.device.DeviceFix
import com.myt.domain.device.TelemetryMerger
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.TelemetrySource
import com.myt.domain.simulation.DrivingSimulationId
import com.myt.domain.simulation.DrivingSimulationRunner
import com.myt.domain.simulation.DrivingSimulationScenarios
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.quota.FleetQuotaPolicy
import com.myt.domain.repository.BluetoothRepository
import com.myt.domain.repository.ChargeSessionRecorder
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.repository.TripRecorder
import com.myt.platform.DeviceLocationPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class TelemetryUseCase(
    private val fleetRepository: FleetRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val quota: FleetQuotaUseCase,
    private val tripRecorder: TripRecorder,
    private val chargeSessionRecorder: ChargeSessionRecorder,
    private val historyRepository: HistoryRepository,
    private val debugLogger: DebugLogger,
    private val deviceLocation: DeviceLocationPlatform,
    private val clock: Clock = Clock.System,
) {
    private val _gaugeState = MutableStateFlow(GaugeState())
    val gaugeState: StateFlow<GaugeState> = _gaugeState.asStateFlow()

    private var fleetBase: GaugeState = GaugeState()
    private var lastDeviceFix: DeviceFix? = null
    private var preferDeviceSpeed: Boolean = true
    private var lastBtConnected: Boolean = false
    private var lastKnownLatitude: Double? = null
    private var lastKnownLongitude: Double? = null
    private var lastKnownHeading: Float? = null

    private var pollingJob: Job? = null
    private var deviceJob: Job? = null
    private var btJob: Job? = null
    private var simulationJob: Job? = null
    private var lastUiRefreshMs: Long = 0L
    private var allowDeviceLocationFallback: Boolean = false

    val isSimulating: Boolean get() = simulationJob?.isActive == true

    fun setPreferDeviceSpeed(enabled: Boolean) {
        preferDeviceSpeed = enabled
        syncDeviceUpdates(lastBtConnected)
        republish(lastBtConnected)
    }

    /**
     * When the map needs coordinates and Fleet has none yet, allow phone GPS
     * even without BT (approximate, e.g. while standing next to a charging car).
     */
    fun setAllowDeviceLocationFallback(enabled: Boolean) {
        if (allowDeviceLocationFallback == enabled) return
        allowDeviceLocationFallback = enabled
        syncDeviceUpdates(lastBtConnected)
        republish(lastBtConnected)
    }

    fun startPolling(scope: CoroutineScope, config: VehicleConfig) {
        pollingJob?.cancel()
        deviceJob?.cancel()
        btJob?.cancel()

        btJob = scope.launch {
            bluetoothRepository.isConnected.collect { bt ->
                lastBtConnected = bt
                syncDeviceUpdates(bt)
                republish(bt)
            }
        }

        deviceJob = scope.launch {
            deviceLocation.fixes.collect { fix ->
                lastDeviceFix = fix
                republish(lastBtConnected)
            }
        }

        pollingJob = scope.launch {
            quota.hydrate()
            debugLogger.i("Telemetry", "Polling started for VIN…${config.vin.takeLast(4)}")
            historyRepository.loadVehicleSnapshot(config.vin)?.let { cached ->
                fleetBase = retainLocation(cached)
                republish(lastBtConnected)
                debugLogger.d(
                    "Telemetry",
                    "Hydrated snapshot age=${clock.now().toEpochMilliseconds() - cached.lastUpdated}ms lat=${fleetBase.latitude != null}",
                )
            }
            while (isActive) {
                if (isSimulating) {
                    delay(500)
                    continue
                }
                if (!quota.appInForeground) {
                    debugLogger.d("Telemetry", "App background — idle wait")
                    deviceLocation.stopUpdates()
                    delay(FleetQuotaPolicy.BACKGROUND_IDLE_MS)
                    continue
                }

                val btConnected = bluetoothRepository.isConnected.first()
                lastBtConnected = btConnected
                syncDeviceUpdates(btConnected)

                val decision = quota.evaluate(FleetCallCategory.Data)
                if (!decision.allowed) {
                    debugLogger.w("Telemetry", "Quota blocked data poll: ${decision.reason}")
                    historyRepository.loadVehicleSnapshot(config.vin)?.let { cached ->
                        fleetBase = retainLocation(cached.copy(connection = ConnectionStatus.QuotaHold))
                    } ?: run {
                        fleetBase = fleetBase.copy(connection = ConnectionStatus.QuotaHold)
                    }
                    republish(btConnected)
                    delay(decision.retryAfterMs)
                    continue
                }

                if (fleetBase.isSleeping) {
                    val wake = quota.evaluate(FleetCallCategory.Wake)
                    if (!wake.allowed) {
                        delay(config.pollingIntervalSleepMs)
                        continue
                    }
                }

                val interval = pollingInterval(fleetBase, config, btConnected) *
                    quota.intervalMultiplier(quota.snapshot.value.mode)

                val cached = historyRepository.loadVehicleSnapshot(config.vin)
                if (cached != null) {
                    val ageMs = clock.now().toEpochMilliseconds() - cached.lastUpdated
                    val incomplete = cached.latitude == null ||
                        cached.longitude == null ||
                        cached.tires == null
                    // Charging sessions end quickly near limit — never skip Fleet while
                    // last snapshot said charging (avoids stale "94% 충전중" after Complete).
                    val chargingStaleRisk = cached.charging?.isCharging == true
                    if (ageMs in 0 until interval && !incomplete && !chargingStaleRisk) {
                        debugLogger.d("Telemetry", "Cache hit — skip Fleet API (age=${ageMs}ms < ${interval}ms)")
                        fleetBase = retainLocation(cached.copy(bluetoothPresent = btConnected))
                        republish(btConnected)
                        delay(interval - ageMs)
                        continue
                    }
                    if (chargingStaleRisk) {
                        debugLogger.d("Telemetry", "Cache was charging — force Fleet refresh")
                    }
                    if (incomplete) {
                        debugLogger.d(
                            "Telemetry",
                            "Cache incomplete (lat=${cached.latitude != null} tires=${cached.tires != null}) — fetch Fleet",
                        )
                    }
                }

                val result = fleetRepository.fetchVehicleState(config.vin)
                result.onSuccess { state ->
                    debugLogger.i(
                        "Telemetry",
                        "Fleet fetch ok speed=${state.speedKmh.toInt()} gear=${state.gear} soc=${state.socPercent.toInt()}",
                    )
                    fleetBase = state.copy(
                        bluetoothPresent = btConnected,
                        connection = when {
                            state.isSleeping -> ConnectionStatus.Sleeping
                            else -> ConnectionStatus.FleetConnected
                        },
                    ).let(::retainLocation)
                    val merged = republish(btConnected)
                    tripRecorder.onGaugeUpdate(merged, config.vin)
                    chargeSessionRecorder.onGaugeUpdate(merged, config.vin)
                }.onFailure { error ->
                    val soft = error is QuotaExceededException ||
                        error::class.simpleName?.contains("VehicleDataUnavailable") == true ||
                        error.message.orEmpty().contains("Empty vehicle_data", ignoreCase = true)
                    if (soft) {
                        debugLogger.w("Telemetry", "Fleet fetch soft-fail: ${error.message}")
                    } else {
                        debugLogger.e("Telemetry", "Fleet fetch failed: ${error.message}", error)
                    }
                    val snap = historyRepository.loadVehicleSnapshot(config.vin)
                    if (snap != null) {
                        fleetBase = retainLocation(
                            snap.copy(
                                bluetoothPresent = btConnected,
                                connection = when {
                                    error is QuotaExceededException -> ConnectionStatus.QuotaHold
                                    else -> snap.connection
                                },
                            ),
                        )
                    } else {
                        fleetBase = fleetBase.copy(
                            bluetoothPresent = btConnected,
                            connection = when {
                                error is QuotaExceededException -> ConnectionStatus.QuotaHold
                                btConnected -> ConnectionStatus.BluetoothOnly
                                else -> ConnectionStatus.Error
                            },
                        )
                    }
                    republish(btConnected)
                }

                val nextInterval = pollingInterval(fleetBase, config, btConnected) *
                    quota.intervalMultiplier(quota.snapshot.value.mode)
                delay(nextInterval)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        deviceJob?.cancel()
        btJob?.cancel()
        pollingJob = null
        deviceJob = null
        btJob = null
        deviceLocation.stopUpdates()
        lastDeviceFix = null
        debugLogger.i("Telemetry", "Polling stopped")
    }

    fun startDrivingSimulation(scope: CoroutineScope, scenarioId: DrivingSimulationId) {
        stopDrivingSimulation()
        val scenario = DrivingSimulationScenarios.byId(scenarioId)
        val runner = DrivingSimulationRunner(scenario)
        debugLogger.i("Telemetry", "Driving simulation started: ${scenario.name}")
        simulationJob = scope.launch {
            while (isActive) {
                _gaugeState.value = runner.tick()
                delay(scenario.tickMs)
            }
        }
    }

    fun stopDrivingSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        debugLogger.i("Telemetry", "Driving simulation stopped")
        republish(lastBtConnected)
    }

    suspend fun refreshOnce(vin: String) {
        debugLogger.i("Telemetry", "Manual refresh requested")
        quota.hydrate()
        val btConnected = bluetoothRepository.isConnected.first()
        lastBtConnected = btConnected
        syncDeviceUpdates(btConnected)
        fleetRepository.fetchVehicleState(vin)
            .onSuccess { state ->
                fleetBase = state.copy(
                    bluetoothPresent = btConnected,
                    connection = when {
                        state.isSleeping -> ConnectionStatus.Sleeping
                        else -> ConnectionStatus.FleetConnected
                    },
                ).let(::retainLocation)
                republish(btConnected)
            }
            .onFailure { error ->
                fleetBase = fleetBase.copy(
                    bluetoothPresent = btConnected,
                    connection = when {
                        error is QuotaExceededException -> ConnectionStatus.QuotaHold
                        btConnected -> ConnectionStatus.BluetoothOnly
                        else -> ConnectionStatus.Error
                    },
                )
                republish(btConnected)
            }
    }

    /**
     * On-demand Fleet fetch for UI panes (map / tires / status).
     * Missing fields bypass soft throttle; otherwise min interval is 45s.
     */
    suspend fun refreshForUiNeed(vin: String, need: UiFreshNeed) {
        val now = clock.now().toEpochMilliseconds()
        val missing = when (need) {
            UiFreshNeed.Location -> fleetBase.latitude == null || fleetBase.longitude == null
            UiFreshNeed.Tires -> fleetBase.tires == null
            UiFreshNeed.Status -> false
        }
        if (need == UiFreshNeed.Location && missing) {
            setAllowDeviceLocationFallback(true)
        }
        val ageMs = now - fleetBase.lastUpdated
        val stale = ageMs >= UI_STALE_MS
        if (!missing && !stale) {
            debugLogger.d("Telemetry", "UI refresh skip ($need) — fresh age=${ageMs}ms")
            return
        }
        val minGap = if (missing) UI_MISSING_MIN_GAP_MS else UI_SOFT_MIN_GAP_MS
        if (now - lastUiRefreshMs < minGap) {
            debugLogger.d(
                "Telemetry",
                "UI refresh throttle ($need missing=$missing) gap=${now - lastUiRefreshMs}ms",
            )
            return
        }
        lastUiRefreshMs = now
        debugLogger.i("Telemetry", "UI refresh ($need missing=$missing stale=$stale)")
        refreshOnce(vin)
    }

    private fun syncDeviceUpdates(btConnected: Boolean) {
        val wantDevice = quota.appInForeground && (
            (btConnected && preferDeviceSpeed) || allowDeviceLocationFallback
        )
        if (wantDevice) {
            if (deviceLocation.hasPermission()) {
                deviceLocation.startUpdates()
            } else {
                deviceLocation.stopUpdates()
                lastDeviceFix = null
                debugLogger.d("Telemetry", "Device GPS skipped — no location permission")
            }
        } else {
            if (!btConnected && !allowDeviceLocationFallback) {
                debugLogger.d("Telemetry", "Device GPS off — BT not connected")
            }
            deviceLocation.stopUpdates()
            lastDeviceFix = null
        }
    }

    private fun retainLocation(state: GaugeState): GaugeState {
        val lat = state.latitude ?: lastKnownLatitude
        val lng = state.longitude ?: lastKnownLongitude
        val heading = state.headingDegrees ?: lastKnownHeading
        if (state.latitude != null && state.longitude != null) {
            lastKnownLatitude = state.latitude
            lastKnownLongitude = state.longitude
            lastKnownHeading = state.headingDegrees ?: lastKnownHeading
            if (allowDeviceLocationFallback) {
                allowDeviceLocationFallback = false
                syncDeviceUpdates(lastBtConnected)
            }
            return state
        }
        if (lat == null || lng == null) return state
        return state.copy(
            latitude = lat,
            longitude = lng,
            headingDegrees = heading,
            locationSource = when {
                state.locationSource != TelemetrySource.None -> state.locationSource
                else -> TelemetrySource.Cache
            },
        )
    }

    private fun republish(btConnected: Boolean): GaugeState {
        if (isSimulating) return _gaugeState.value
        val merged = TelemetryMerger.merge(
            fleet = fleetBase,
            deviceFix = lastDeviceFix,
            bluetoothPresent = btConnected,
            preferDeviceSpeed = preferDeviceSpeed,
            previousSpeedKmh = _gaugeState.value.speedKmh,
            clock = clock,
        )
        val withLocation = retainLocation(merged)
        if (withLocation.latitude != null && withLocation.longitude != null) {
            lastKnownLatitude = withLocation.latitude
            lastKnownLongitude = withLocation.longitude
            lastKnownHeading = withLocation.headingDegrees ?: lastKnownHeading
        }
        _gaugeState.value = withLocation
        return withLocation
    }

    /** Demo / sandbox vehicle commands until Fleet command proxy is live. */
    fun applyDemoControl(command: com.myt.domain.control.VehicleCommand) {
        val current = _gaugeState.value
        _gaugeState.value = when (command) {
            com.myt.domain.control.VehicleCommand.Lock -> current.copy(locked = true)
            com.myt.domain.control.VehicleCommand.Unlock -> current.copy(locked = false)
            com.myt.domain.control.VehicleCommand.ClimateOn -> current.copy(climateOn = true)
            com.myt.domain.control.VehicleCommand.ClimateOff -> current.copy(climateOn = false)
            com.myt.domain.control.VehicleCommand.SentryOn -> current.copy(sentryMode = true)
            com.myt.domain.control.VehicleCommand.SentryOff -> current.copy(sentryMode = false)
            com.myt.domain.control.VehicleCommand.Trunk,
            com.myt.domain.control.VehicleCommand.Frunk,
            com.myt.domain.control.VehicleCommand.Flash,
            com.myt.domain.control.VehicleCommand.Honk,
            com.myt.domain.control.VehicleCommand.DogMode,
            com.myt.domain.control.VehicleCommand.CampMode,
            com.myt.domain.control.VehicleCommand.WindowVent,
            com.myt.domain.control.VehicleCommand.ChargePortOpen,
            com.myt.domain.control.VehicleCommand.ChargePortClose,
            -> current
        }
        fleetBase = fleetBase.copy(
            locked = _gaugeState.value.locked ?: fleetBase.locked,
            climateOn = _gaugeState.value.climateOn ?: fleetBase.climateOn,
            sentryMode = _gaugeState.value.sentryMode ?: fleetBase.sentryMode,
        )
    }

    private fun pollingInterval(state: GaugeState, config: VehicleConfig, btConnected: Boolean): Long = when {
        state.connection == ConnectionStatus.QuotaHold -> 15 * 60_000L
        state.connection == ConnectionStatus.Error -> 60_000L
        state.isSleeping -> config.pollingIntervalSleepMs
        state.charging?.isCharging == true -> {
            val limit = state.charging.chargeLimitPercent
            val soc = state.socPercent
            if (limit != null && soc >= (limit - 6)) {
                config.pollingIntervalChargingNearLimitMs
            } else {
                config.pollingIntervalChargingMs
            }
        }
        state.gear == Gear.PARK -> config.pollingIntervalParkedMs
        btConnected && preferDeviceSpeed && lastDeviceFix != null -> config.pollingIntervalDrivingMs
        else -> config.pollingIntervalDrivingNoDeviceMs
    }

    companion object {
        private const val UI_STALE_MS = 90_000L
        private const val UI_SOFT_MIN_GAP_MS = 45_000L
        private const val UI_MISSING_MIN_GAP_MS = 15_000L
    }
}
