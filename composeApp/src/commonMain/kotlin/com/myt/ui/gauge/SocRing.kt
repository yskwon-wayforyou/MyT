package com.myt.ui.gauge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.clusterArcTrack
import com.myt.ui.theme.clusterFaceDeep

@Composable
fun SocRing(
    socPercent: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 6.dp,
    showLabel: Boolean = true,
) {
    val colors = GaugeTheme.colors
    val clamped = socPercent.coerceIn(0f, 100f)
    val color = when {
        clamped > 50f -> colors.socGreen
        clamped > 20f -> colors.socYellow
        else -> colors.socRed
    }

    Box(
        modifier = modifier
            .size(size + 8.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.12f), colors.clusterFaceDeep.copy(alpha = 0.9f)),
                ),
            )
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset = strokePx * 1.2f
            // Segment ticks (12 segments — digital battery feel)
            repeat(12) { i ->
                val angle = (-90f + i * 30f) * (Math.PI / 180.0).toFloat()
                val inner = size.toPx() / 2f - strokePx - 2f
                val outer = size.toPx() / 2f - 1f
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val lit = i < (clamped / 100f * 12f)
                drawLine(
                    color = if (lit) color.copy(alpha = 0.55f) else colors.stroke.copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(
                        cx + kotlin.math.cos(angle) * inner,
                        cy + kotlin.math.sin(angle) * inner,
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        cx + kotlin.math.cos(angle) * outer,
                        cy + kotlin.math.sin(angle) * outer,
                    ),
                    strokeWidth = 1.5f,
                )
            }
            drawArc(
                color = colors.clusterArcTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.toPx() - inset * 2, size.toPx() - inset * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = color.copy(alpha = 0.45f),
                startAngle = -90f,
                sweepAngle = 360f * (clamped / 100f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset - 2f, inset - 2f),
                size = androidx.compose.ui.geometry.Size(size.toPx() - (inset - 2f) * 2, size.toPx() - (inset - 2f) * 2),
                style = Stroke(width = strokePx + 5f, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0f to color.copy(alpha = 0.6f),
                    0.5f to color,
                    1f to color.copy(alpha = 0.8f),
                ),
                startAngle = -90f,
                sweepAngle = 360f * (clamped / 100f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.toPx() - inset * 2, size.toPx() - inset * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        if (showLabel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${clamped.toInt()}",
                    color = colors.textPrimary,
                    fontSize = (size.value * 0.24f).sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = (size.value * 0.24f).sp,
                )
                Text(
                    text = "%",
                    color = color,
                    fontSize = (size.value * 0.11f).sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
