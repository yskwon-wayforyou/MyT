package com.myt.data.fleet

import com.myt.debug.DebugLogger
import com.myt.config.TeslaConfig
import com.myt.config.TeslaConfigStore
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.GaugeState as GaugeStateModel
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.TokenRepository
import com.myt.domain.usecase.FleetQuotaUseCase
import com.myt.domain.usecase.QuotaExceededException
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class KtorFleetRepository(
    private val httpClient: HttpClient,
    private val tokenRepository: TokenRepository,
    private val configStore: TeslaConfigStore,
    private val quota: FleetQuotaUseCase,
    private val historyRepository: com.myt.domain.repository.HistoryRepository,
    private val debugLogger: DebugLogger,
    private val fleetApi: TeslaFleetApi = TeslaFleetApi(
        httpClient = httpClient,
        baseUrlProvider = { configStore.current().fleetApiBase },
    ),
    private val oauthClient: TeslaOAuthClient = TeslaOAuthClient(
        httpClient = httpClient,
        configProvider = { configStore.current() },
    ),
) : FleetRepository {
    private val config: TeslaConfig get() = configStore.current()

    @Volatile
    private var memorySnapshot: GaugeState? = null

    override fun observeVehicleState(vin: String): Flow<GaugeState> = flow {
        while (true) {
            emit(fetchVehicleState(vin).getOrElse { fallbackState(it) })
            delay(60_000)
        }
    }

    override suspend fun fetchVehicleState(vin: String): Result<GaugeState> = runCatching {
        debugLogger.d("Fleet", "fetchVehicleState VIN…${vin.takeLast(4)}")
        hydrateCache(vin)
        requireCall(FleetCallCategory.Data)
        val token = resolveAccessToken()
        val first = runCatching { fleetApi.fetchVehicleData(token, vin) }
        quota.record(FleetCallCategory.Data, first.isSuccess)
        val state = first.getOrElse { error ->
            debugLogger.w("Fleet", "Initial fetch failed: ${error.message}; wake=${shouldWake(error)}")
            if (!shouldWake(error)) throw error
            requireCall(FleetCallCategory.Wake)
            val woke = runCatching { fleetApi.wakeUp(token, vin) }
            quota.record(FleetCallCategory.Wake, woke.isSuccess)
            woke.getOrThrow()
            delay(2_500)
            requireCall(FleetCallCategory.Data)
            val second = runCatching { fleetApi.fetchVehicleData(token, vin) }
            quota.record(FleetCallCategory.Data, second.isSuccess)
            second.getOrElse { secondError ->
                memorySnapshot?.copy(
                    connection = ConnectionStatus.Disconnected,
                    isSleeping = true,
                    lastUpdated = Clock.System.now().toEpochMilliseconds(),
                ) ?: throw secondError
            }
        }
        historyRepository.saveVehicleSnapshot(vin, state)
        memorySnapshot = state
        debugLogger.i("Fleet", "Snapshot saved soc=${state.socPercent.toInt()} sleeping=${state.isSleeping}")
        state
    }.onFailure { error ->
        val soft = error is VehicleDataUnavailableException || error is QuotaExceededException
        if (soft) {
            debugLogger.w("Fleet", "fetchVehicleState soft-fail: ${error.message}")
        } else {
            debugLogger.e("Fleet", "fetchVehicleState failed: ${error.message}", error)
        }
    }

    override suspend fun sendNavigationRequest(vin: String, destination: String): Result<Unit> =
        runCatching {
            requireCall(FleetCallCategory.Command)
            val token = resolveAccessToken()
            val result = runCatching { fleetApi.sendNavigationRequest(token, vin, destination) }
            quota.record(FleetCallCategory.Command, result.isSuccess)
            result.getOrThrow()
        }

    override suspend fun sendVehicleCommand(
        vin: String,
        commandName: String,
        whichTrunk: String?,
    ): Result<Unit> =
        runCatching {
            requireCall(FleetCallCategory.Command)
            val token = resolveAccessToken()
            val first = runCatching { fleetApi.sendVehicleCommand(token, vin, commandName, whichTrunk) }
            if (first.isSuccess) {
                quota.record(FleetCallCategory.Command, true)
                return@runCatching
            }
            val error = first.exceptionOrNull()!!
            if (!shouldWake(error)) {
                quota.record(FleetCallCategory.Command, false)
                throw error
            }
            requireCall(FleetCallCategory.Wake)
            val woke = runCatching { fleetApi.wakeUp(token, vin) }
            quota.record(FleetCallCategory.Wake, woke.isSuccess)
            woke.getOrThrow()
            delay(2_500)
            requireCall(FleetCallCategory.Command)
            val second = runCatching { fleetApi.sendVehicleCommand(token, vin, commandName, whichTrunk) }
            quota.record(FleetCallCategory.Command, second.isSuccess)
            second.getOrThrow()
        }.onFailure { error ->
            debugLogger.w("Fleet", "sendVehicleCommand $commandName failed: ${error.message}")
        }

    override suspend fun wakeVehicle(vin: String): Result<Unit> = runCatching {
        requireCall(FleetCallCategory.Wake)
        val token = resolveAccessToken()
        val result = runCatching { fleetApi.wakeUp(token, vin) }
        quota.record(FleetCallCategory.Wake, result.isSuccess)
        result.getOrThrow()
    }

    suspend fun listVehicles(): Result<List<TeslaVehicleSummary>> = runCatching {
        requireCall(FleetCallCategory.Data)
        val token = resolveAccessToken()
        val result = runCatching { fleetApi.listVehicles(token) }
        quota.record(FleetCallCategory.Data, result.isSuccess)
        result.getOrThrow()
    }

    fun oauthClient(): TeslaOAuthClient = oauthClient

    private suspend fun requireCall(category: FleetCallCategory) {
        val decision = quota.evaluate(category)
        if (!decision.allowed) {
            throw QuotaExceededException(decision.reason ?: "Fleet API 무료 한도 보호")
        }
    }

    private suspend fun resolveAccessToken(): String {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        tokenRepository.getAccessToken()?.let { token ->
            if (!tokenRepository.isAccessTokenExpired(nowMs)) return token
        }
        val refresh = tokenRepository.getRefreshToken()
            ?: error("Tesla OAuth required — log in from onboarding")
        val refreshed = oauthClient.refreshAccessToken(refresh)
        val expiresAt = refreshed.expiresIn?.let { nowMs + it * 1000 }
        tokenRepository.saveAccessToken(refreshed.accessToken, expiresAt)
        refreshed.refreshToken?.let { tokenRepository.saveRefreshToken(it) }
        return refreshed.accessToken
    }

    private fun shouldWake(cause: Throwable): Boolean {
        if (cause is QuotaExceededException) return false
        if (cause is VehicleDataUnavailableException) return true
        val message = cause.message.orEmpty().lowercase()
        return "408" in message ||
            "412" in message ||
            "asleep" in message ||
            "unavailable" in message ||
            "offline" in message ||
            "empty vehicle_data" in message
    }

    private suspend fun hydrateCache(vin: String) {
        if (memorySnapshot == null) {
            memorySnapshot = historyRepository.loadVehicleSnapshot(vin)
        }
    }

    private fun fallbackState(cause: Throwable): GaugeState {
        if (!config.isOAuthConfigured()) return demoGaugeState()
        memorySnapshot?.let { cached ->
            return cached.copy(
                connection = when {
                    cause is QuotaExceededException -> ConnectionStatus.QuotaHold
                    else -> cached.connection
                },
            )
        }
        return GaugeStateModel(
            connection = if (cause is QuotaExceededException) {
                ConnectionStatus.QuotaHold
            } else {
                ConnectionStatus.Disconnected
            },
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
        )
    }

    private fun demoGaugeState(): GaugeState = GaugeStateModel(
        speedKmh = 0f,
        gear = Gear.PARK,
        socPercent = 0f,
        connection = ConnectionStatus.Disconnected,
        lastUpdated = Clock.System.now().toEpochMilliseconds(),
    )
}
