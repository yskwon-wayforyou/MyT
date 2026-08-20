package com.myt.ui.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myt.ui.theme.GaugeTheme

@Composable
fun InfoRow(
    socPercent: Float,
    rangeKm: Float,
    insideTempC: Float?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocRing(socPercent = socPercent)
        Text(
            text = "${rangeKm.toInt()} km",
            color = GaugeTheme.colors.textPrimary,
        )
        Text(
            text = insideTempC?.let { "${it.toInt()}°C" } ?: "--°C",
            color = GaugeTheme.colors.textSecondary,
        )
    }
}
