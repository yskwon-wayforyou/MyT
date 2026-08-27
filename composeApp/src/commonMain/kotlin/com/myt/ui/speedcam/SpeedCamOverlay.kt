package com.myt.ui.speedcam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.UnitConverter
import com.myt.domain.model.AlertLevel
import com.myt.domain.model.SpeedCamAlert
import com.myt.ui.gauge.ClusterIcons

/**
 * Full-area speed camera alert. Replaces beep/vibration with strong visual feedback.
 * [visualBoost] = simulation / silent test → faster pulse, larger type.
 */
@Composable
fun SpeedCamVisualAlert(
    alert: SpeedCamAlert,
    useKmh: Boolean,
    visualBoost: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = alertAccent(alert.level)
    val pulseMs = when (alert.level) {
        AlertLevel.L3 -> if (visualBoost) 320 else 480
        AlertLevel.L2 -> if (visualBoost) 480 else 650
        AlertLevel.L1 -> if (visualBoost) 650 else 900
        AlertLevel.SECTION -> 750
    }
    val transition = rememberInfiniteTransition(label = "speedCamPulse")
    val pulse by transition.animateFloat(
        initialValue = if (visualBoost) 0.45f else 0.28f,
        targetValue = if (visualBoost) 1f else 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val borderDp = when (alert.level) {
        AlertLevel.L3 -> if (visualBoost) 8.dp else 6.dp
        AlertLevel.L2 -> if (visualBoost) 6.dp else 4.dp
        AlertLevel.L1 -> if (visualBoost) 4.dp else 3.dp
        AlertLevel.SECTION -> 4.dp
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = "speed_cam_alert_${alert.level.name}" }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0101820))
            .border(borderDp, accent.copy(alpha = 0.35f + pulse * 0.55f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.06f + pulse * 0.06f),
                            accent.copy(alpha = 0.22f + pulse * 0.18f),
                            accent.copy(alpha = 0.34f + pulse * 0.12f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = ClusterIcons.speedCam,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (visualBoost) 44.dp else 36.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                levelTitle(alert.level),
                color = Color.White,
                fontSize = if (visualBoost) 26.sp else 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (alert.level == AlertLevel.L3) {
                RowCenteredMetrics(
                    current = UnitConverter.formatSpeed(alert.currentSpeedKmh, useKmh),
                    limit = "${alert.camera.speedLimitKmh}",
                    unit = UnitConverter.speedUnitLabel(useKmh),
                    visualBoost = visualBoost,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "${alert.distanceM}m · 한도 ${alert.camera.speedLimitKmh} · ${UnitConverter.formatSpeed(alert.currentSpeedKmh, useKmh)}",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = if (visualBoost) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                alert.camera.roadName ?: alert.message,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = if (visualBoost) 15.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (visualBoost) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "시각 경보 · 무음·무진동 테스트",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SpeedCamOverlay(
    alert: SpeedCamAlert?,
    useKmh: Boolean = true,
    visualBoost: Boolean = false,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.96f),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        val current = alert ?: return@AnimatedVisibility
        SpeedCamVisualAlert(
            alert = current,
            useKmh = useKmh,
            visualBoost = visualBoost,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RowCenteredMetrics(
    current: String,
    limit: String,
    unit: String,
    visualBoost: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            current,
            color = Color.White,
            fontSize = if (visualBoost) 56.sp else 44.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "/ $limit $unit",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = if (visualBoost) 20.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun alertAccent(level: AlertLevel): Color = when (level) {
    AlertLevel.L1 -> Color(0xFFFFB020)
    AlertLevel.L2 -> Color(0xFFFF6A00)
    AlertLevel.L3 -> Color(0xFFE82127)
    AlertLevel.SECTION -> Color(0xFF64D2FF)
}

private fun levelTitle(level: AlertLevel): String = when (level) {
    AlertLevel.L1 -> "단속 예고"
    AlertLevel.L2 -> "단속 임박"
    AlertLevel.L3 -> "과속 주의"
    AlertLevel.SECTION -> "구간 단속"
}
