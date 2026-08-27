package com.myt.ui.voice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.theme.GaugeTheme
import kotlin.math.PI
import kotlin.math.sin

private val voiceCapabilities = listOf(
    "전화 걸기",
    "문자 보내기",
    "카카오톡",
    "내비 목적지",
    "히스토리",
    "읽어줘",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantDialog(
    transcript: String?,
    status: String,
    isListening: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = GaugeTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121820),
        title = {
            Text(
                if (isListening) "음성 명령" else "음성 결과",
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ListeningOrb(active = isListening)
                Text(
                    text = when {
                        isListening -> "듣고 있어요 · 짧게 말씀해 주세요"
                        !transcript.isNullOrBlank() -> transcript
                        else -> status.ifBlank { "인식된 내용이 없습니다" }
                    },
                    color = if (isListening) Color(0xFF64D2FF) else colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (isListening) {
                    ListeningWaveform(active = true)
                    Text("할 수 있는 일", color = colors.textSecondary, fontSize = 12.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        voiceCapabilities.forEach { label ->
                            CapabilityChip(label)
                        }
                    }
                    Text(
                        "예) 「강남역으로 안내」 · 「엄마에게 전화」 · 「최근 주행 읽어줘」",
                        color = colors.textSecondary.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = colors.accent)
            }
        },
    )
}

@Composable
private fun CapabilityChip(label: String) {
    Text(
        label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1C2433))
            .border(1.dp, Color(0xFF3D9EFF).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun ListeningOrb(active: Boolean) {
    val pulse = rememberInfiniteTransition(label = "orb")
    val scale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "orbScale",
    )
    val ring by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "orbRing",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFF3D9EFF).copy(alpha = 0.18f * ring)),
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (active) Color(0xFF64D2FF) else Color(0xFF3D4658),
                            if (active) Color(0xFF1B6BFF) else Color(0xFF1C2433),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (active) "MIC" else "OK",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun ListeningWaveform(active: Boolean) {
    val wave = rememberInfiniteTransition(label = "wave")
    val t by wave.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "waveT",
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A1018)),
    ) {
        if (!active) return@Canvas
        val bars = 24
        val gap = 3.dp.toPx()
        val barW = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val phase = t + i * 0.35f
            val h = (0.25f + 0.7f * (0.5f + 0.5f * sin(phase.toDouble()).toFloat())) * size.height
            val left = i * (barW + gap)
            drawRoundRect(
                color = Color(0xFF64D2FF).copy(alpha = 0.55f + 0.35f * (h / size.height)),
                topLeft = Offset(left, (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
