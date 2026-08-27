package com.myt.ui.gauge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import kotlin.math.min

@Composable
fun GMeter(
    longAccelG: Float,
    latAccelG: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = GaugeTheme.colors
    val dialHeight = if (compact) 120.dp else 150.dp

    TeslaCard(modifier = modifier.fillMaxWidth(), accent = Color(0xFFBF5AF2)) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("G-METER", color = colors.textSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dialHeight),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    val radius = min(cx, cy) * 0.82f
                    drawCircle(colors.textSecondary.copy(alpha = 0.25f), radius, Offset(cx, cy), style = Stroke(2f))
                    drawCircle(colors.textSecondary.copy(alpha = 0.12f), radius * 0.5f, Offset(cx, cy), style = Stroke(1f))
                    drawLine(colors.textSecondary.copy(alpha = 0.2f), Offset(cx - radius, cy), Offset(cx + radius, cy), 1f)
                    drawLine(colors.textSecondary.copy(alpha = 0.2f), Offset(cx, cy - radius), Offset(cx, cy + radius), 1f)

                    val clampedLat = latAccelG.coerceIn(-1.2f, 1.2f)
                    val clampedLong = longAccelG.coerceIn(-1.2f, 1.2f)
                    val px = cx + (clampedLat / 1.2f) * radius
                    val py = cy - (clampedLong / 1.2f) * radius
                    drawCircle(Color(0xFFBF5AF2), 8.dp.toPx(), Offset(px, py))
                    drawCircle(Color(0xFFBF5AF2).copy(alpha = 0.35f), 14.dp.toPx(), Offset(px, py))
                }
            }
            Text(
                "L ${"%.2f".format(latAccelG)} · Lg ${"%.2f".format(longAccelG)}",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
