package com.myt.data.token

import com.myt.domain.repository.TokenRepository
import com.myt.platform.SecureStoragePlatform
import com.russhwolf.settings.Settings

class TokenRepositoryImpl(
    private val settings: Settings,
    private val secureStorage: SecureStoragePlatform,
) : TokenRepository {
    override suspend fun saveAccessToken(token: String) {
        settings.putString(KEY_ACCESS, token)
        secureStorage.saveToken(KEY_ACCESS, token)
    }

    override suspend fun getAccessToken(): String? {
        return secureStorage.getToken(KEY_ACCESS) ?: settings.getStringOrNull(KEY_ACCESS)
    }

    override suspend fun saveRefreshToken(token: String) {
        settings.putString(KEY_REFRESH, token)
        secureStorage.saveToken(KEY_REFRESH, token)
    }

    override suspend fun getRefreshToken(): String? {
        return secureStorage.getToken(KEY_REFRESH) ?: settings.getStringOrNull(KEY_REFRESH)
    }

    override suspend fun clearTokens() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
        secureStorage.deleteToken(KEY_ACCESS)
        secureStorage.deleteToken(KEY_REFRESH)
    }

    override suspend fun isAuthenticated(): Boolean = getAccessToken() != null

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
