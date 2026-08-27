package com.myt.ui.gauge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.ChargeInfo
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard

@Composable
fun ChargePanel(
    charge: ChargeInfo?,
    socPercent: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    embedded: Boolean = false,
) {
    val colors = GaugeTheme.colors
    val isCharging = charge?.isCharging == true
    val rateKw = charge?.chargeRateKw
    val limit = charge?.chargeLimitPercent ?: 80
    val fill = (socPercent / limit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(if (embedded) 4.dp else if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!embedded) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("충전", color = colors.textSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(
                        if (isCharging) "ACTIVE" else charge?.chargingState ?: "대기",
                        color = if (isCharging) colors.socGreen else colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 10.dp else 14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill)
                        .height(if (compact) 10.dp else 14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.socYellow.copy(alpha = if (isCharging) 0.95f else 0.45f)),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${socPercent.toInt()}% · 한도 $limit%",
                    color = colors.textPrimary,
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        rateKw?.let { append("${it.toInt()} kW") }
                        charge?.timeToFullMinutes?.let {
                            if (isNotEmpty()) append(" · ")
                            append("${it}분")
                        }
                    }.ifBlank { "--" },
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
    if (embedded) {
        Box(modifier.fillMaxWidth()) { body() }
    } else {
        TeslaCard(modifier = modifier.fillMaxWidth(), accent = colors.socYellow) { body() }
    }
}
