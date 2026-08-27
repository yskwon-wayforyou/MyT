package com.myt.ui.gauge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.GaugeDisplayPrefs
import com.myt.domain.model.GaugeField
import com.myt.domain.model.GaugeState
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaGlassPanel

@Composable
fun VehicleStatusGrid(
    state: GaugeState,
    columns: Int = 3,
    compact: Boolean = false,
    prefs: GaugeDisplayPrefs = GaugeDisplayPrefs(),
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val tiles = statusTiles(state, colors).filter { prefs.shows(it.field) }
    val resolvedColumns = prefs.resolvedColumns(columns)
    if (tiles.isEmpty()) return

    TeslaGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp),
        accent = Color(0xFF64D2FF),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        ) {
            Text(
                "차량 상태",
                color = colors.textSecondary,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
            )
            tiles.chunked(resolvedColumns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                ) {
                    row.forEach { tile ->
                        StatusTileCard(tile = tile, compact = compact, modifier = Modifier.weight(1f))
                    }
                    repeat(resolvedColumns - row.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class StatusTile(
    val field: GaugeField,
    val label: String,
    val value: String,
    val unit: String = "",
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun StatusTileCard(
    tile: StatusTile,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    // Flat tile inside outer glass — avoids double card borders on phone.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceHigh.copy(alpha = 0.72f))
            .border(1.dp, tile.accent.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 8.dp else 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.label,
                tint = tile.accent,
                modifier = Modifier
                    .padding(end = if (compact) 6.dp else 8.dp)
                    .width(if (compact) 16.dp else 18.dp),
            )
            Column {
                Text(
                    tile.label,
                    color = colors.textSecondary,
                    fontSize = if (compact) 10.sp else 11.sp,
                    letterSpacing = 0.4.sp,
                )
                Text(
                    tile.value,
                    color = tile.accent,
                    fontSize = if (compact) 15.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 1.dp),
                )
                if (tile.unit.isNotEmpty()) {
                    Text(tile.unit, color = colors.textSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun statusTiles(state: GaugeState, c: com.myt.ui.theme.GaugeColors): List<StatusTile> {
    val charge = state.charging
    val chargeValue = when {
        charge?.isCharging == true ->
            listOfNotNull(
                charge.chargeRateKw?.let { "${it.toInt()}kW" },
                charge.timeToFullMinutes?.let { "${it}분" },
            ).joinToString(" · ").ifBlank { "충전 중" }
        !charge?.chargingState.isNullOrBlank() -> charge?.chargingState.orEmpty()
        else -> "대기"
    }
    val tires = state.tires?.let {
        "${"%.1f".format(it.frontLeftBar)}/${"%.1f".format(it.frontRightBar)}"
    } ?: "--"
    return listOf(
        StatusTile(GaugeField.Battery, "배터리", "${state.socPercent.toInt()}", "%", c.socGreen, ClusterIcons.battery),
        StatusTile(GaugeField.Range, "주행 가능", "${state.rangeKm.toInt()}", "km", Color(0xFF64D2FF), ClusterIcons.range),
        StatusTile(GaugeField.InsideTemp, "실내", state.insideTempC?.toInt()?.toString() ?: "--", "°C", Color(0xFFFF9F0A), ClusterIcons.climate),
        StatusTile(GaugeField.OutsideTemp, "외기", state.outsideTempC?.toInt()?.toString() ?: "--", "°C", Color(0xFF5AC8FA), ClusterIcons.climate),
        StatusTile(GaugeField.Power, "전력", state.powerKw?.let { "%.1f".format(it) } ?: "--", "kW", Color(0xFFBF5AF2), ClusterIcons.batteryCharging),
        StatusTile(GaugeField.Charge, "충전", chargeValue, charge?.chargeLimitPercent?.let { "한도 $it%" }.orEmpty(), c.socYellow, ClusterIcons.charging),
        StatusTile(GaugeField.Lock, "잠금", state.locked?.let { if (it) "잠김" else "열림" } ?: "--", accent = Color(0xFF40C8E0), icon = lockIcon(state.locked)),
        StatusTile(GaugeField.Sentry, "Sentry", state.sentryMode?.let { if (it) "ON" else "OFF" } ?: "--", accent = c.accent, icon = ClusterIcons.warning),
        StatusTile(GaugeField.Climate, "공조", state.climateOn?.let { if (it) "ON" else "OFF" } ?: "--", accent = Color(0xFF64D2FF), icon = ClusterIcons.climate),
        StatusTile(GaugeField.Tires, "타이어", tires, "bar", c.textPrimary, ClusterIcons.tire),
        StatusTile(GaugeField.Odometer, "누적", state.odometerKm?.toInt()?.toString() ?: "--", "km", c.textSecondary, ClusterIcons.range),
        StatusTile(
            GaugeField.Nav,
            "내비",
            state.navigation?.destinationName ?: "없음",
            state.navigation?.etaMinutes?.let { "${it}분" }.orEmpty(),
            Color(0xFFBF5AF2),
            ClusterIcons.navigation,
        ),
    )
}
