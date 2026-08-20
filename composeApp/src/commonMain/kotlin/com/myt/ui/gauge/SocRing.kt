package com.myt.ui.gauge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.theme.GaugeTheme

@Composable
fun SocRing(
    socPercent: Float,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val color = when {
        socPercent > 50f -> colors.socGreen
        socPercent > 20f -> colors.socYellow
        else -> colors.socRed
    }

    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = colors.surface,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (socPercent / 100f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${socPercent.toInt()}%",
            color = colors.textPrimary,
            fontSize = 16.sp,
        )
    }
}
