package com.myt.ui.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard

@Composable
fun InfoRow(
    socPercent: Float,
    rangeKm: Float,
    insideTempC: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeslaCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SocRing(socPercent = socPercent)
                Text("배터리", color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        TeslaCard(modifier = Modifier.weight(1f)) {
            MetricCell(label = "주행 가능", value = "${rangeKm.toInt()}", unit = "km")
        }
        TeslaCard(modifier = Modifier.weight(1f)) {
            MetricCell(
                label = "실내",
                value = insideTempC?.toInt()?.toString() ?: "--",
                unit = "°C",
            )
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    unit: String,
) {
    val colors = GaugeTheme.colors
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp, letterSpacing = 0.8.sp)
        Text(
            value,
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(unit, color = colors.textSecondary, fontSize = 12.sp)
    }
}
