package com.myt.ui

import com.myt.domain.model.ConnectionStatus
import com.myt.ui.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStateMachineTest {
    @Test
    fun startDestinationUsesGaugeWhenOnboarded() {
        assertEquals(Route.Gauge, AppStateMachine.startDestination(onboardingComplete = true))
        assertEquals(Route.Onboarding, AppStateMachine.startDestination(onboardingComplete = false))
    }

    @Test
    fun autoOpenGaugeWhenBtOrFleetConnected() {
        assertTrue(AppStateMachine.shouldAutoOpenGauge(true, bluetoothPresent = true, fleetConnected = false))
        assertTrue(AppStateMachine.shouldAutoOpenGauge(true, bluetoothPresent = false, fleetConnected = true))
        assertFalse(AppStateMachine.shouldAutoOpenGauge(false, bluetoothPresent = true, fleetConnected = true))
    }

    @Test
    fun connectionErrorKindMapsStatuses() {
        assertEquals(ConnectionErrorKind.Sleeping, AppStateMachine.connectionErrorKind(ConnectionStatus.Sleeping, true))
        assertEquals(ConnectionErrorKind.QuotaHold, AppStateMachine.connectionErrorKind(ConnectionStatus.QuotaHold, true))
        assertEquals(ConnectionErrorKind.BluetoothLost, AppStateMachine.connectionErrorKind(ConnectionStatus.Disconnected, false))
    }
}
