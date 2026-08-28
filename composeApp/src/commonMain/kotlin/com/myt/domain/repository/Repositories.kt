package com.myt.domain.repository

import com.myt.domain.model.GaugeState
import com.myt.domain.model.NavInfo
import kotlinx.coroutines.flow.Flow

interface FleetRepository {
    fun observeVehicleState(vin: String): Flow<GaugeState>
    suspend fun fetchVehicleState(vin: String): Result<GaugeState>
    suspend fun sendNavigationRequest(vin: String, destination: String): Result<Unit>
    suspend fun wakeVehicle(vin: String): Result<Unit>
    /** Fleet signed/unsigned vehicle command path name, e.g. `door_lock`. */
    suspend fun sendVehicleCommand(
        vin: String,
        commandName: String,
        whichTrunk: String? = null,
        jsonBody: String? = null,
    ): Result<Unit>
}

interface PoiRepository {
    fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int): List<com.myt.domain.model.SpeedCamera>
}

interface BluetoothRepository {
    val isConnected: Flow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}

interface TokenRepository {
    suspend fun saveAccessToken(token: String, expiresAtMs: Long? = null)
    suspend fun getAccessToken(): String?
    suspend fun getAccessTokenExpiresAt(): Long?
    suspend fun isAccessTokenExpired(nowMs: Long): Boolean
    suspend fun saveRefreshToken(token: String)
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun isAuthenticated(): Boolean
}

interface SettingsRepository {
    suspend fun getVin(): String?
    suspend fun setVin(vin: String)
    suspend fun getSpeedUnitKmh(): Boolean
    suspend fun setSpeedUnitKmh(useKmh: Boolean)
    suspend fun isOnboardingComplete(): Boolean
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun getGaugeDisplayPrefs(): com.myt.domain.model.GaugeDisplayPrefs
    suspend fun setGaugeDisplayPrefs(prefs: com.myt.domain.model.GaugeDisplayPrefs)
    suspend fun isDarkTheme(): Boolean
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun isDriveSafetyAcknowledged(): Boolean
    suspend fun setDriveSafetyAcknowledged(acknowledged: Boolean)
    suspend fun getNotificationPrefs(): com.myt.domain.model.NotificationPrefs
    suspend fun setNotificationPrefs(prefs: com.myt.domain.model.NotificationPrefs)
}
