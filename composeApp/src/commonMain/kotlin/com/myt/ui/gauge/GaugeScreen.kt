package com.myt.ui.gauge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.myt.ui.GaugeViewModel
import com.myt.ui.gauge.layout.AdaptiveGaugeLayout

@Composable
fun GaugeScreen(
    viewModel: GaugeViewModel,
    onVoiceNav: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gaugeState by viewModel.gaugeState.collectAsState()
    val alert by viewModel.speedCamAlert.collectAsState()
    val layoutConfig by viewModel.layoutConfig.collectAsState()

    AdaptiveGaugeLayout(
        state = gaugeState,
        alert = alert,
        layoutConfig = layoutConfig,
        onVoiceNav = onVoiceNav,
        onSettings = onSettings,
        modifier = modifier,
    )
}
