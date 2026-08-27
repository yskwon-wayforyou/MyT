package com.myt.ui

import com.myt.domain.model.ConnectionStatus
import com.myt.ui.navigation.Route

/** Central navigation/session resolution (M13). */
enum class AppSessionPhase {
    Splash,
    Onboarding,
    Home,
    Gauge,
}

enum class ConnectionErrorKind {
    None,
    Sleeping,
    BluetoothLost,
    ApiError,
    QuotaHold,
}

object AppStateMachine {
    fun startDestination(onboardingComplete: Boolean): Route =
        if (onboardingComplete) Route.Gauge else Route.Onboarding

    fun sessionPhase(sessionReady: Boolean, onboardingComplete: Boolean): AppSessionPhase = when {
        !sessionReady -> AppSessionPhase.Splash
        !onboardingComplete -> AppSessionPhase.Onboarding
        else -> AppSessionPhase.Gauge
    }

    fun connectionErrorKind(status: ConnectionStatus, bluetoothPresent: Boolean): ConnectionErrorKind = when {
        status == ConnectionStatus.Sleeping -> ConnectionErrorKind.Sleeping
        status == ConnectionStatus.QuotaHold -> ConnectionErrorKind.QuotaHold
        status == ConnectionStatus.Error -> ConnectionErrorKind.ApiError
        status == ConnectionStatus.BluetoothOnly && !bluetoothPresent -> ConnectionErrorKind.BluetoothLost
        status == ConnectionStatus.Disconnected && !bluetoothPresent -> ConnectionErrorKind.BluetoothLost
        else -> ConnectionErrorKind.None
    }

    fun shouldAutoOpenGauge(
        onboardingComplete: Boolean,
        bluetoothPresent: Boolean,
        fleetConnected: Boolean,
    ): Boolean = onboardingComplete && (bluetoothPresent || fleetConnected)
}
