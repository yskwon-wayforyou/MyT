package com.myt.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.history.TripHistoryItem
import com.myt.ui.map.MapRouteView
import com.myt.ui.map.buildRoutePreviewStats
import com.myt.ui.map.decodeRoutePoints
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaGlassPanel
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue

@Composable
fun TripRouteScreen(
    trip: TripHistoryItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val routeStats = remember(trip.polylineEncoded) {
        buildRoutePreviewStats(decodeRoutePoints(trip.polylineEncoded))
    }

    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("주행 경로", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHigh)) {
                    Text("닫기", color = colors.textPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${"%.1f".format(trip.distanceKm)} km · 최고 ${trip.maxSpeedKmh?.toInt() ?: "--"} km/h",
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            Text(
                "SOC ${trip.startSoc?.toInt() ?: "--"} → ${trip.endSoc?.toInt() ?: "--"}" +
                    trip.efficiencyKmPerKwh?.let { v -> " · ${"%.2f".format(v)} km/kWh" }.orEmpty(),
                color = colors.textPrimary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            routeStats?.let { stats ->
                TeslaGlassPanel(modifier = Modifier.fillMaxWidth(), accent = colors.accentBlue) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("경로 ${"%.1f".format(stats.approxDistanceKm)} km", color = colors.textPrimary, fontSize = 12.sp)
                            Text("${stats.pointCount} pts", color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Text(
                            "범위 ${stats.latSpanMeters}m × ${stats.lngSpanMeters}m",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            MapRouteView(
                polylineEncoded = trip.polylineEncoded,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}
