package com.myt.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.myt.domain.device.DeviceFix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
actual class DeviceLocationPlatform actual constructor(context: Any) {
    private val appContext = (context as Context).applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val enabled = MutableStateFlow(false)

    actual val fixes: Flow<DeviceFix?> = enabled.flatMapLatest { on ->
        if (!on) flowOf(null) else locationUpdates()
    }

    actual fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    actual fun startUpdates() {
        if (!hasPermission()) {
            enabled.value = false
            return
        }
        enabled.value = true
    }

    actual fun stopUpdates() {
        enabled.value = false
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates(): Flow<DeviceFix?> = callbackFlow {
        if (!hasPermission()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toDeviceFix())
            }

            @Deprecated("Deprecated in API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        locationManager.getLastKnownLocation(provider)?.let { trySend(it.toDeviceFix()) }
        locationManager.requestLocationUpdates(
            provider,
            100L,
            0f,
            listener,
            Looper.getMainLooper(),
        )
        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }

    private fun Location.toDeviceFix(): DeviceFix {
        val speedMps = if (hasSpeed()) speed else 0f
        val speedKmh = max(0f, speedMps * 3.6f)
        val heading = if (hasBearing()) bearing else null
        return DeviceFix(
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            headingDegrees = heading,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            timestampMs = time,
        )
    }
}
