package com.myt.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.phase3.BatteryHealthPoint
import com.myt.phase3.FsdAnalytics
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import kotlin.math.max

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    configuredVin: String?,
    onBack: () -> Unit,
    onShareText: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val battery by viewModel.batteryReport.collectAsState()
    val badge by viewModel.carbonBadgeState.collectAsState()
    val co2 by viewModel.co2Summary.collectAsState()
    val exportMsg by viewModel.exportMessage.collectAsState()
    val liveCamera by viewModel.liveCamera.collectAsState()
    val cameraFrames by viewModel.cameraFrames.collectAsState()
    val fsd = FsdAnalytics.summary()

    LaunchedEffect(configuredVin) {
        viewModel.loadLiveCameraStatus(configuredVin)
    }

    TeslaScreen(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("고급 분석", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("닫기", color = colors.accentBlue) }
            }

            TeslaCard(accent = Color(0xFF30D158)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("탄소 배지 · ${badge.tier.label}", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        "CO₂ 절감 ${"%.1f".format(badge.co2SavedKg)} kg · 나무 ${"%.1f".format(co2.equivalentTrees)}그루 상당",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                    badge.nextTier?.let { next ->
                        LinearProgressIndicator(
                            progress = { badge.progressToNext },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF30D158),
                            trackColor = Color.White.copy(alpha = 0.12f),
                        )
                        Text(
                            "다음 ${next.label}까지 ${"%.1f".format(next.minCo2SavedKg - badge.co2SavedKg)} kg",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            TeslaCard(accent = colors.accentBlue) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("배터리 건강 (충전 기반 추정)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    battery.trendPctPerYear?.let { trend ->
                        Text(
                            "연간 추세 ${"%.2f".format(trend)}% / 년",
                            color = if (trend < 0f) Color(0xFFFF6A00) else Color(0xFF30D158),
                            fontSize = 13.sp,
                        )
                    } ?: Text("충전 기록이 쌓이면 추세 그래프가 표시됩니다.", color = colors.textSecondary, fontSize = 13.sp)
                    if (battery.points.isNotEmpty()) {
                        BatteryHealthChart(
                            points = battery.points,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                        )
                    }
                }
            }

            TeslaCard(accent = Color(0xFFBF5AF2)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("데이터 내보내기 (M43)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { viewModel.exportTrips(onShareText) }) {
                            Text("주행 CSV", color = colors.accentBlue)
                        }
                        TextButton(onClick = { viewModel.exportCharges(onShareText) }) {
                            Text("충전 CSV", color = colors.accentBlue)
                        }
                        TextButton(
                            onClick = {
                                val sample = """
                                    id,start,distance_km,start_soc,end_soc
                                    import-demo,1700000000000,8.2,70,64
                                """.trimIndent()
                                viewModel.importTessieCsv(sample, configuredVin ?: "DEMOVIN")
                            },
                        ) {
                            Text("샘플 Import", color = Color(0xFF30D158))
                        }
                    }
                    exportMsg?.let { Text(it, color = colors.textSecondary, fontSize = 12.sp) }
                }
            }

            TeslaCard(accent = Color(0xFF8E8E93)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FSD / Autopilot (M42)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(fsd.message, color = colors.textSecondary, fontSize = 12.sp)
                    if (fsd.autopilotMinutes > 0) {
                        Text(
                            "추정 ${fsd.autopilotMinutes}분 · 개입 ${fsd.interventions}회",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            TeslaCard(accent = Color(0xFF64D2FF)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live Camera (M44)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(liveCamera.message, color = colors.textSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.startDemoCamera(configuredVin) }) {
                            Text("데모 스트림", color = colors.accentBlue)
                        }
                        TextButton(onClick = { viewModel.stopDemoCamera(configuredVin) }) {
                            Text("중지", color = colors.textSecondary)
                        }
                    }
                    if (cameraFrames.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().height(72.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            cameraFrames.forEach { frame ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(frame.colorArgb)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(frame.label, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryHealthChart(points: List<BatteryHealthPoint>, modifier: Modifier = Modifier) {
    val minY = points.minOf { it.capacityPct }
    val maxY = max(100f, points.maxOf { it.capacityPct })
    val span = (maxY - minY).coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val barW = size.width / max(points.size, 1)
        points.forEachIndexed { i, p ->
            val h = ((p.capacityPct - minY) / span) * size.height
            drawRoundRect(
                color = Color(0xFF64D2FF),
                topLeft = Offset(i * barW + 2f, size.height - h),
                size = Size(barW - 4f, h),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}
