package com.myt.ui.gauge.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.SpeedCamAlert
import com.myt.ui.gauge.InfoRow
import com.myt.ui.gauge.NavRow
import com.myt.ui.gauge.SpeedDisplay
import com.myt.ui.speedcam.SpeedCamOverlay
import com.myt.ui.theme.GaugeTheme

@Composable
fun AdaptiveGaugeLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    layoutConfig: LayoutConfig,
    onVoiceNav: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (layoutConfig) {
        LayoutConfig.SinglePane -> GaugeSinglePaneLayout(
            state = state,
            alert = alert,
            onVoiceNav = onVoiceNav,
            onSettings = onSettings,
            modifier = modifier,
        )
        LayoutConfig.TwoPane -> GaugeTwoPaneLayout(
            state = state,
            alert = alert,
            onVoiceNav = onVoiceNav,
            onSettings = onSettings,
            modifier = modifier,
        )
        LayoutConfig.ThreePane -> GaugeTwoPaneLayout(
            state = state,
            alert = alert,
            onVoiceNav = onVoiceNav,
            onSettings = onSettings,
            modifier = modifier,
        )
    }
}

@Composable
fun GaugeSinglePaneLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    onVoiceNav: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GaugeTheme.colors.bg),
    ) {
        StatusBar(connection = state.connection)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SpeedDisplay(speedKmh = state.speedKmh, gear = state.gear)
        }
        InfoRow(
            socPercent = state.socPercent,
            rangeKm = state.rangeKm,
            insideTempC = state.insideTempC,
        )
        NavRow(navigation = state.navigation)
        SpeedCamOverlay(alert = alert)
        ActionBar(onVoiceNav = onVoiceNav, onSettings = onSettings)
    }
}

@Composable
fun GaugeTwoPaneLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    onVoiceNav: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GaugeTheme.colors.bg),
    ) {
        StatusBar(connection = state.connection)
        SpeedCamOverlay(alert = alert)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                SpeedDisplay(speedKmh = state.speedKmh, gear = state.gear)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                InfoRow(
                    socPercent = state.socPercent,
                    rangeKm = state.rangeKm,
                    insideTempC = state.insideTempC,
                )
                NavRow(navigation = state.navigation)
                state.powerKw?.let {
                    Text("Power: ${"%.1f".format(it)} kW", color = GaugeTheme.colors.textSecondary)
                }
            }
        }
        ActionBar(onVoiceNav = onVoiceNav, onSettings = onSettings)
    }
}

@Composable
private fun StatusBar(connection: ConnectionStatus) {
    val label = when (connection) {
        ConnectionStatus.Disconnected -> "● BT off"
        ConnectionStatus.BluetoothOnly -> "● BT"
        ConnectionStatus.FleetConnected -> "● Fleet"
        ConnectionStatus.Sleeping -> "😴 Sleep"
        ConnectionStatus.Error -> "⚠ Error"
    }
    Text(
        text = label,
        color = GaugeTheme.colors.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ActionBar(
    onVoiceNav: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
    ) {
        TextButton(onClick = onVoiceNav) {
            Text("🎤 Voice", color = GaugeTheme.colors.accent)
        }
        TextButton(onClick = onSettings) {
            Text("⚙ Settings", color = GaugeTheme.colors.textSecondary)
        }
    }
}
