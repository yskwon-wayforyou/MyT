package com.myt.data.fleet

import com.myt.config.TeslaConfig
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.GaugeState as GaugeStateModel
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.TokenRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class KtorFleetRepository(
    private val httpClient: HttpClient,
    private val tokenRepository: TokenRepository,
    private val config: TeslaConfig,
    private val fleetApi: TeslaFleetApi = TeslaFleetApi(httpClient, config.fleetApiBase),
    private val oauthClient: TeslaOAuthClient = TeslaOAuthClient(httpClient, config),
) : FleetRepository {

    override fun observeVehicleState(vin: String): Flow<GaugeState> = flow {
        while (true) {
            emit(fetchVehicleState(vin).getOrElse { fallbackState(it) })
            delay(2_000)
        }
    }

    override suspend fun fetchVehicleState(vin: String): Result<GaugeState> = runCatching {
        val token = resolveAccessToken()
        fleetApi.fetchVehicleData(token, vin)
    }

    override suspend fun sendNavigationRequest(vin: String, destination: String): Result<Unit> =
        runCatching {
            val token = resolveAccessToken()
            fleetApi.sendNavigationRequest(token, vin, destination)
        }

    override suspend fun wakeVehicle(vin: String): Result<Unit> = runCatching {
        val token = resolveAccessToken()
        fleetApi.wakeUp(token, vin)
    }

    suspend fun listVehicles(): Result<List<TeslaVehicleSummary>> = runCatching {
        val token = resolveAccessToken()
        fleetApi.listVehicles(token)
    }

    fun oauthClient(): TeslaOAuthClient = oauthClient

    private suspend fun resolveAccessToken(): String {
        tokenRepository.getAccessToken()?.let { return it }
        val refresh = tokenRepository.getRefreshToken()
            ?: error("Tesla OAuth required — log in from onboarding")
        val refreshed = oauthClient.refreshAccessToken(refresh)
        tokenRepository.saveAccessToken(refreshed.accessToken)
        refreshed.refreshToken?.let { tokenRepository.saveRefreshToken(it) }
        return refreshed.accessToken
    }

    private fun fallbackState(cause: Throwable): GaugeState {
        if (!config.isOAuthConfigured()) return demoGaugeState()
        return GaugeStateModel(
            connection = ConnectionStatus.Disconnected,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
        ).also {
            // keep UI alive with disconnected state when API fails
            cause.printStackTrace()
        }
    }

    private fun demoGaugeState(): GaugeState = GaugeStateModel(
        speedKmh = 0f,
        gear = Gear.PARK,
        socPercent = 0f,
        connection = ConnectionStatus.Disconnected,
        lastUpdated = Clock.System.now().toEpochMilliseconds(),
    )
}
