package com.myt.ui.speedcam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myt.domain.model.AlertLevel
import com.myt.domain.model.SpeedCamAlert
import com.myt.ui.theme.GaugeTheme

@Composable
fun SpeedCamOverlay(
    alert: SpeedCamAlert?,
    modifier: Modifier = Modifier,
) {
    if (alert == null) return

    val (bg, fg) = when (alert.level) {
        AlertLevel.L1 -> Color(0x33FFF3E0) to GaugeTheme.colors.warningL1
        AlertLevel.L2 -> Color(0x66FFE0B2) to GaugeTheme.colors.warningL2
        AlertLevel.L3 -> Color(0x99FFCDD2) to GaugeTheme.colors.warningL3
        AlertLevel.SECTION -> Color(0x4DE3F2FD) to Color(0xFF1565C0)
    }

    Text(
        text = alert.message,
        color = fg,
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
