package com.myt.ui.usage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.quota.QuotaMode
import com.myt.domain.quota.QuotaSnapshot
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaGlassPanel
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ApiUsageChip(
    snapshot: QuotaSnapshot,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val fill = usageColor(snapshot.mode, colors.socGreen, colors.socYellow, colors.accent)
    val width = if (compact) 64.dp else 72.dp
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceHigh.copy(alpha = 0.65f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = "${(snapshot.usedRatio * 100).toInt()}%",
            color = fill,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            val h = size.height
            drawRoundRect(color = colors.stroke, size = size, cornerRadius = CornerRadius(h))
            drawRoundRect(
                color = fill,
                size = Size(size.width * snapshot.usedRatio.coerceIn(0f, 1f), h),
                cornerRadius = CornerRadius(h),
            )
        }
    }
}

@Composable
fun ApiUsageDetailScreen(
    snapshot: QuotaSnapshot,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("API 사용량", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Row {
                    TextButton(onClick = onRefresh) { Text("새로고침", color = colors.accent) }
                    TextButton(onClick = onBack) { Text("닫기", color = colors.textSecondary) }
                }
            }
            Text(
                text = when (snapshot.mode) {
                    QuotaMode.Normal -> "무료 크레딧 안에서 동작 중"
                    QuotaMode.Conserve -> "절약 모드 · 호출 간격을 늘렸습니다"
                    QuotaMode.Blocked -> "한도 보호 · 이번 달 테슬라API 호출을 멈췄습니다"
                },
                color = usageColor(snapshot.mode, colors.socGreen, colors.socYellow, colors.accent),
                fontSize = 13.sp,
            )
            TeslaGlassPanel(modifier = Modifier.fillMaxWidth(), accent = colors.accentBlue) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("이번 달 추정", color = colors.textSecondary, fontSize = 12.sp)
                    Text(
                        "$${snapshot.estimatedUsd.format2()}  /  $${snapshot.creditUsd.format2()} 크레딧",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Text("${snapshot.month} · Tesla 개인 월 $10 할인 기준", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
            CategoryRow("Data 폴링", snapshot.dataCount, snapshot.dataLimit, colors.accent)
            CategoryRow("Command", snapshot.commandCount, snapshot.commandLimit, colors.socYellow)
            CategoryRow("Wake", snapshot.wakeCount, snapshot.wakeLimit, colors.socRed)
            TeslaCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Text("최근 7일 추정 비용", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    WeekChart(days = snapshot.last7DaysUsd, barColor = colors.accent)
                }
            }
            TeslaCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("오늘", color = colors.textSecondary, fontSize = 12.sp)
                    Text(
                        "Data ${snapshot.dailyDataCount}/${snapshot.dailyDataLimit}  ·  Wake ${snapshot.dailyWakeCount}/${snapshot.dailyWakeLimit}",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                    )
                    Text(
                        "주차 5분 · 주행 60초 · 백그라운드 정지 · 자동 웨이크 하루 2회",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            TeslaCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("최근 호출", color = colors.textSecondary, fontSize = 12.sp)
                    if (snapshot.recent.isEmpty()) {
                        Text("아직 기록이 없습니다", color = colors.textSecondary, fontSize = 13.sp)
                    } else {
                        snapshot.recent.take(12).forEach { event ->
                            val label = when (event.category) {
                                FleetCallCategory.Data -> "Data"
                                FleetCallCategory.Command -> "Cmd"
                                FleetCallCategory.Wake -> "Wake"
                            }
                            Text(
                                "$label  ${if (event.ok) "OK" else "FAIL"}",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, used: Int, limit: Int, color: Color) {
    val colors = GaugeTheme.colors
    val ratio = if (limit == 0) 0f else used.toFloat() / limit
    TeslaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = colors.textSecondary, fontSize = 12.sp)
                Text("$used / $limit", color = colors.textPrimary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                drawRoundRect(colors.stroke, size = size, cornerRadius = CornerRadius(6f))
                drawRoundRect(
                    color,
                    size = Size(size.width * ratio.coerceIn(0f, 1f), size.height),
                    cornerRadius = CornerRadius(6f),
                )
            }
        }
    }
}

@Composable
private fun WeekChart(
    days: List<Pair<String, Double>>,
    barColor: Color,
) {
    val maxUsd = max(0.01, days.maxOfOrNull { it.second } ?: 0.01)
    val colors = GaugeTheme.colors
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(88.dp)) {
            val n = days.size.coerceAtLeast(1)
            val gap = 8.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(4f)
            days.forEachIndexed { index, (_, usd) ->
                val h = (usd / maxUsd).toFloat().coerceIn(0.04f, 1f) * size.height
                val x = index * (barW + gap)
                drawRect(
                    color = barColor.copy(alpha = 0.85f),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { (label, _) ->
                Text(label.takeLast(2), color = colors.textSecondary, fontSize = 10.sp)
            }
        }
    }
}

private fun usageColor(mode: QuotaMode, ok: Color, warn: Color, bad: Color): Color = when (mode) {
    QuotaMode.Normal -> ok
    QuotaMode.Conserve -> warn
    QuotaMode.Blocked -> bad
}

private fun Double.format2(): String {
    val cents = (this * 100).roundToInt()
    return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}
