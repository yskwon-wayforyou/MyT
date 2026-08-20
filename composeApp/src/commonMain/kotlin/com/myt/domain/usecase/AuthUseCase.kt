package com.myt.domain.usecase

import com.myt.config.TeslaConfig
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
    private val config: TeslaConfig,
    private val oauthPlatform: OAuthPlatform,
) {
    suspend fun isAuthenticated(): Boolean = tokenRepository.isAuthenticated()

    fun startTeslaLogin(state: String = "myt-onboarding") {
        require(config.isOAuthConfigured()) { "Tesla OAuth is not configured — fill tesla.local.properties" }
        val url = fleetRepository.oauthClient().buildAuthorizationUrl(state)
        oauthPlatform.openAuthorizationUrl(url)
    }

    suspend fun handleOAuthCallback(code: String): Result<String?> = runCatching {
        val tokens = fleetRepository.oauthClient().exchangeAuthorizationCode(code)
        saveTokens(tokens.accessToken, tokens.refreshToken.orEmpty())
        val vin = fleetRepository.listVehicles()
            .getOrNull()
            ?.firstNotNullOfOrNull { it.vin?.takeIf { v -> v.isNotBlank() } }
        vin?.let { settingsRepository.setVin(it) }
        vin
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        tokenRepository.saveAccessToken(accessToken)
        if (refreshToken.isNotBlank()) {
            tokenRepository.saveRefreshToken(refreshToken)
        }
        secureStorage.saveToken(KEY_ACCESS, accessToken)
        if (refreshToken.isNotBlank()) {
            secureStorage.saveToken(KEY_REFRESH, refreshToken)
        }
    }

    suspend fun completeOnboarding(vin: String) {
        settingsRepository.setVin(vin)
        settingsRepository.setOnboardingComplete(true)
    }

    suspend fun logout() {
        tokenRepository.clearTokens()
        secureStorage.deleteToken(KEY_ACCESS)
        secureStorage.deleteToken(KEY_REFRESH)
        settingsRepository.setOnboardingComplete(false)
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
