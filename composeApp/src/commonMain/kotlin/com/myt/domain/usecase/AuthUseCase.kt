package com.myt.domain.usecase

import com.myt.debug.DebugLogger
import com.myt.config.TeslaConfigStore
import com.myt.data.fleet.KtorFleetRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.domain.repository.TokenRepository
import com.myt.platform.OAuthPlatform
import com.myt.platform.SecureStoragePlatform

class AuthUseCase(
    private val tokenRepository: TokenRepository,
    private val settingsRepository: SettingsRepository,
    private val secureStorage: SecureStoragePlatform,
    private val fleetRepository: KtorFleetRepository,
    private val configStore: TeslaConfigStore,
    private val oauthPlatform: OAuthPlatform,
    private val debugLogger: DebugLogger,
) {
    private val config get() = configStore.current()
    suspend fun isAuthenticated(): Boolean = tokenRepository.isAuthenticated()

    fun startTeslaLogin(state: String = "myt-onboarding") {
        require(config.isOAuthConfigured()) { "Tesla OAuth is not configured — fill tesla.local.properties" }
        debugLogger.i("Auth", "Starting Tesla OAuth")
        val url = fleetRepository.oauthClient().buildAuthorizationUrl(state)
        oauthPlatform.openAuthorizationUrl(url)
    }

    suspend fun handleOAuthCallback(code: String): Result<String?> = runCatching {
        debugLogger.i("Auth", "OAuth callback received")
        val tokens = fleetRepository.oauthClient().exchangeAuthorizationCode(code)
        saveTokens(tokens.accessToken, tokens.refreshToken.orEmpty(), tokens.expiresIn)
        val vin = fleetRepository.listVehicles()
            .getOrNull()
            ?.firstNotNullOfOrNull { it.vin?.takeIf { v -> v.isNotBlank() } }
        vin?.let { saveVin(it) }
        debugLogger.i("Auth", "OAuth success vin=${vin?.takeLast(4) ?: "none"}")
        vin
    }.onFailure { debugLogger.e("Auth", "OAuth callback failed: ${it.message}", it) }

    suspend fun ensureFreshAccessToken(nowMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()): Result<String> =
        runCatching {
            val current = tokenRepository.getAccessToken()
            if (current != null && !tokenRepository.isAccessTokenExpired(nowMs)) {
                return@runCatching current
            }
            val refresh = tokenRepository.getRefreshToken()
                ?: error("Tesla OAuth required — log in from onboarding")
            val refreshed = fleetRepository.oauthClient().refreshAccessToken(refresh)
            saveTokens(refreshed.accessToken, refreshed.refreshToken.orEmpty(), refreshed.expiresIn)
            refreshed.accessToken
        }.onFailure { debugLogger.e("Auth", "Token refresh failed: ${it.message}", it) }

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresInSec: Long? = null) {
        val expiresAtMs = expiresInSec?.let {
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + it * 1000
        }
        tokenRepository.saveAccessToken(accessToken, expiresAtMs)
        if (refreshToken.isNotBlank()) {
            tokenRepository.saveRefreshToken(refreshToken)
        }
        secureStorage.saveToken(KEY_ACCESS, accessToken)
        if (refreshToken.isNotBlank()) {
            secureStorage.saveToken(KEY_REFRESH, refreshToken)
        }
    }

    suspend fun completeOnboarding(vin: String) {
        saveVin(vin)
        settingsRepository.setOnboardingComplete(true)
    }

    private suspend fun saveVin(vin: String) {
        val normalized = vin.trim().uppercase()
        if (normalized.isBlank()) return
        settingsRepository.setVin(normalized)
        configStore.save(config.copy(vehicleVin = normalized))
    }

    suspend fun logout() {
        debugLogger.i("Auth", "Logout")
        tokenRepository.clearTokens()
        secureStorage.deleteToken(KEY_ACCESS)
        secureStorage.deleteToken(KEY_REFRESH)
        settingsRepository.setOnboardingComplete(false)
    }

    /** W1 Auth 테스트 — 토큰 갱신 + Virtual Key 공개키 URL 안내. */
    suspend fun runAuthSelfTest(): AuthSelfTestReport {
        val authed = tokenRepository.isAuthenticated()
        val refresh = if (!authed) {
            "미로그인 — 온보딩에서 Tesla 로그인 필요"
        } else {
            ensureFreshAccessToken().fold(
                onSuccess = { "토큰 갱신 OK · …${it.takeLast(8)}" },
                onFailure = { "토큰 갱신 실패: ${it.message}" },
            )
        }
        val domain = config.partnerDomain.trim()
        val keyHint = if (domain.isBlank()) {
            "partnerDomain 미설정 — tesla.local.properties에 도메인 후 공개키 호스팅"
        } else {
            "https://$domain/.well-known/appspecific/com.tesla.3p.public-key.pem"
        }
        return AuthSelfTestReport(
            authenticated = authed,
            tokenStatus = refresh,
            virtualKeyPublicKeyUrl = keyHint,
            vin = settingsRepository.getVin() ?: config.vehicleVin.ifBlank { null },
        )
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}

data class AuthSelfTestReport(
    val authenticated: Boolean,
    val tokenStatus: String,
    val virtualKeyPublicKeyUrl: String,
    val vin: String?,
) {
    fun summary(): String = buildString {
        append(if (authenticated) "로그인됨" else "미로그인")
        append(" · ")
        append(tokenStatus)
        append("\nVK 공개키: ")
        append(virtualKeyPublicKeyUrl)
        vin?.let { append("\nVIN …${it.takeLast(6)}") }
    }
}
