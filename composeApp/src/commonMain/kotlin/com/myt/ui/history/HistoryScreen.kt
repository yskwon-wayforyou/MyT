package com.myt.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.DailyAggregate
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.HistoryPeriodFilter
import com.myt.domain.history.HistorySortOrder
import com.myt.domain.history.HistoryTab
import com.myt.domain.history.TripHistoryItem
import com.myt.ui.map.RoutePolylineView
import com.myt.ui.map.buildRoutePreviewStats
import com.myt.ui.map.decodeRoutePoints
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaGlassPanel
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import kotlin.math.max
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenTripRoute: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val filter by viewModel.filter.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val charges by viewModel.charges.collectAsState()
    val fleet by viewModel.fleetEvents.collectAsState()
    val chart by viewModel.chart.collectAsState()
    val colors = GaugeTheme.colors
    var selectedDayLabel by remember { mutableStateOf<String?>(null) }
    var selectedTrip by remember { mutableStateOf<TripHistoryItem?>(null) }
    var selectedCharge by remember { mutableStateOf<ChargeHistoryItem?>(null) }
    var selectedFleet by remember { mutableStateOf<FleetApiHistoryItem?>(null) }

    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("히스토리", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHigh)) {
                    Text("닫기", color = colors.textPrimary)
                }
            }
            TabRow(selectedTabIndex = filter.tab.ordinal, containerColor = Color.Transparent) {
                Tab(selected = filter.tab == HistoryTab.Driving, onClick = { viewModel.setTab(HistoryTab.Driving) }, text = { Text("주행") })
                Tab(selected = filter.tab == HistoryTab.Charging, onClick = { viewModel.setTab(HistoryTab.Charging) }, text = { Text("충전") })
                Tab(selected = filter.tab == HistoryTab.FleetApi, onClick = { viewModel.setTab(HistoryTab.FleetApi) }, text = { Text("테슬라API") })
            }
            LaunchedEffect(filter.tab, filter.period) {
                selectedDayLabel = null
                selectedTrip = null
                selectedCharge = null
                selectedFleet = null
            }
            Spacer(Modifier.height(6.dp))
            FilterRow(
                period = filter.period,
                sort = filter.sort,
                tab = filter.tab,
                onlyFailures = filter.onlyFailures,
                onPeriod = viewModel::setPeriod,
                onSort = viewModel::setSort,
                onToggleFailures = viewModel::toggleFailuresOnly,
            )
            TeslaGlassPanel(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), accent = colors.accentBlue, flat = true) {
                Column(Modifier.padding(10.dp)) {
                    Text("추이", color = colors.textSecondary, fontSize = 11.sp)
                    HistoryBarChart(data = chart, barColor = colors.accent)
                    if (chart.isNotEmpty()) {
                        val total = chart.sumOf { it.value.toDouble() }.toFloat()
                        Text(
                            when (filter.tab) {
                                HistoryTab.Driving -> "기간 합계 ${"%.1f".format(total)} km · ${chart.size}일"
                                HistoryTab.Charging -> "기간 합계 ${"%.1f".format(total)} kWh · ${chart.size}일"
                                HistoryTab.FleetApi -> "호출 ${total.toInt()}건 · ${chart.size}일"
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (chart.isNotEmpty()) {
                DayCalendarRow(
                    days = chart.takeLast(7),
                    selected = selectedDayLabel,
                    onSelect = { next -> selectedDayLabel = if (next == selectedDayLabel) null else next },
                    onClear = { selectedDayLabel = null },
                )
            }

            val filteredTrips = if (selectedDayLabel == null) trips else trips.filter { dayLabel(it.startedAtMs) == selectedDayLabel }
            val filteredCharges = if (selectedDayLabel == null) charges else charges.filter { dayLabel(it.startedAtMs) == selectedDayLabel }
            val filteredFleet = if (selectedDayLabel == null) fleet else fleet.filter { dayLabel(it.atMs) == selectedDayLabel }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (filter.tab) {
                    HistoryTab.Driving -> items(filteredTrips, key = { it.id }) { TripRow(it, onClick = { selectedTrip = it }) }
                    HistoryTab.Charging -> items(filteredCharges, key = { it.id }) { ChargeRow(it, onClick = { selectedCharge = it }) }
                    HistoryTab.FleetApi -> items(filteredFleet, key = { it.id }) { FleetRow(it, onClick = { selectedFleet = it }) }
                }
            }

            selectedTrip?.let { trip ->
                TripDetailDialog(
                    item = trip,
                    onDismiss = { selectedTrip = null },
                    onOpenFullRoute = {
                        selectedTrip = null
                        onOpenTripRoute(trip.id)
                    },
                )
            }
            selectedCharge?.let { ChargeDetailDialog(item = it, onDismiss = { selectedCharge = null }) }
            selectedFleet?.let { FleetDetailDialog(item = it, onDismiss = { selectedFleet = null }) }
        }
    }
}

@Composable
private fun FilterRow(
    period: HistoryPeriodFilter,
    sort: HistorySortOrder,
    tab: HistoryTab,
    onlyFailures: Boolean,
    onPeriod: (HistoryPeriodFilter) -> Unit,
    onSort: (HistorySortOrder) -> Unit,
    onToggleFailures: () -> Unit,
) {
    val colors = GaugeTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                HistoryPeriodFilter.Days7 to "7일",
                HistoryPeriodFilter.Days30 to "30일",
                HistoryPeriodFilter.All to "전체",
            ).forEach { (value, label) ->
                FilterChip(
                    selected = period == value,
                    onClick = { onPeriod(value) },
                    label = { Text(label) },
                    colors = chipColors(colors),
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val sorts = when (tab) {
                HistoryTab.Driving -> listOf(HistorySortOrder.Newest to "최신", HistorySortOrder.DistanceDesc to "거리")
                HistoryTab.Charging -> listOf(HistorySortOrder.Newest to "최신", HistorySortOrder.EnergyDesc to "충전량")
                HistoryTab.FleetApi -> listOf(HistorySortOrder.Newest to "최신", HistorySortOrder.Category to "종류")
            }
            sorts.forEach { (value, label) ->
                FilterChip(
                    selected = sort == value,
                    onClick = { onSort(value) },
                    label = { Text(label) },
                    colors = chipColors(colors),
                )
            }
            if (tab == HistoryTab.FleetApi) {
                FilterChip(
                    selected = onlyFailures,
                    onClick = onToggleFailures,
                    label = { Text("실패만") },
                    colors = chipColors(colors),
                )
            }
        }
    }
}

@Composable
private fun chipColors(colors: com.myt.ui.theme.GaugeColors) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = colors.accent,
    selectedLabelColor = colors.bg,
    containerColor = colors.surfaceHigh,
    labelColor = colors.textPrimary,
)

@Composable
private fun HistoryBarChart(data: List<DailyAggregate>, barColor: Color) {
    val colors = GaugeTheme.colors
    if (data.isEmpty()) {
        Text("표시할 데이터가 없습니다", color = colors.textSecondary, fontSize = 12.sp)
        return
    }
    val maxV = max(1f, data.maxOf { it.value })
    Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
        val gap = 6.dp.toPx()
        val barW = ((size.width - gap * (data.size - 1)) / data.size).coerceAtLeast(4f)
        data.forEachIndexed { i, item ->
            val h = (item.value / maxV).coerceIn(0.05f, 1f) * size.height
            drawRoundRect(
                barColor,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(4f),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        data.takeLast(7).forEach { Text(it.dayLabel, color = colors.textSecondary, fontSize = 9.sp) }
    }
}

@Composable
private fun TripRow(item: TripHistoryItem, onClick: () -> Unit) {
    val durationMin = item.endedAtMs?.let { ((it - item.startedAtMs) / 60_000L).toInt() }
    val efficiencyText = item.efficiencyKmPerKwh?.let { v ->
        "${"%.1f".format(v)} km/kWh"
    }
    HistoryTableCard(
        title = formatTime(item.startedAtMs),
        value = "${"%.1f".format(item.distanceKm)} km",
        metrics = listOfNotNull(
            "최고 ${item.maxSpeedKmh?.toInt() ?: "--"}",
            item.avgSpeedKmh?.let { "평균 ${it.toInt()}" },
            durationMin?.let { "${it}분" },
            "SOC ${item.startSoc?.toInt() ?: "--"}→${item.endSoc?.toInt() ?: "--"}",
            efficiencyText,
        ),
        accent = GaugeTheme.colors.socGreen,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ChargeRow(item: ChargeHistoryItem, onClick: () -> Unit) {
    val durationMin = item.endedAtMs?.let { ((it - item.startedAtMs) / 60_000L).toInt() }
    val deltaSoc = item.endSoc?.let { it - item.startSoc }
    HistoryTableCard(
        title = formatTime(item.startedAtMs),
        value = "${"%.1f".format(item.energyKwh ?: 0f)} kWh",
        metrics = listOfNotNull(
            "SOC ${item.startSoc.toInt()}→${item.endSoc?.toInt() ?: "--"}",
            deltaSoc?.let { "+${"%.0f".format(it)}%p" },
            "Peak ${item.peakKw?.toInt() ?: "--"} kW",
            durationMin?.let { "${it}분" },
        ),
        accent = GaugeTheme.colors.socYellow,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun FleetRow(item: FleetApiHistoryItem, onClick: () -> Unit) {
    HistoryTableCard(
        title = formatTime(item.atMs),
        value = item.category,
        metrics = listOf(
            if (item.ok) "성공" else "실패",
            item.detail.orEmpty().ifBlank { "상세 없음" },
        ),
        accent = if (item.ok) GaugeTheme.colors.accentBlue else GaugeTheme.colors.accent,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun HistoryTableCard(
    title: String,
    value: String,
    metrics: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    TeslaCard(modifier = modifier.fillMaxWidth(), accent = accent, flat = true) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = colors.textSecondary, fontSize = 11.sp)
                Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                metrics.filter { it.isNotBlank() }.forEach { m ->
                    Text(
                        m,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val d = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        .toString()
    return d.take(16).replace('T', ' ')
}

@Composable
private fun DayCalendarRow(
    days: List<DailyAggregate>,
    selected: String?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = GaugeTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = onClear,
            label = { Text("전체") },
            colors = chipColors(colors),
        )
        days.forEach { d ->
            FilterChip(
                selected = selected == d.dayLabel,
                onClick = { onSelect(d.dayLabel) },
                label = { Text(d.dayLabel) },
                colors = chipColors(colors),
            )
        }
    }
}

private fun dayLabel(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.monthNumber}/${date.dayOfMonth}"
}

@Composable
private fun TripDetailDialog(
    item: TripHistoryItem,
    onDismiss: () -> Unit,
    onOpenFullRoute: () -> Unit,
) {
    val colors = GaugeTheme.colors
    val routeStats = remember(item.polylineEncoded) {
        buildRoutePreviewStats(decodeRoutePoints(item.polylineEncoded))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주행 상세 · ${formatTime(item.startedAtMs)}", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${"%.1f".format(item.distanceKm)} km · 최고 ${item.maxSpeedKmh?.toInt() ?: "--"} km/h",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = "SOC ${item.startSoc?.toInt() ?: "--"} → ${item.endSoc?.toInt() ?: "--"}" +
                        item.efficiencyKmPerKwh?.let { v -> " · 효율 ${"%.2f".format(v)} km/kWh" }.orEmpty(),
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                )
                routeStats?.let { stats ->
                    TeslaGlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = colors.accentBlue,
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("경로 미리보기", color = colors.textSecondary, fontSize = 11.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("거리 ${"%.1f".format(stats.approxDistanceKm)} km", color = colors.textPrimary, fontSize = 12.sp)
                                Text("포인트 ${stats.pointCount}", color = colors.textSecondary, fontSize = 12.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("남북 폭 ${stats.latSpanMeters} m", color = colors.textSecondary, fontSize = 11.sp)
                                Text("동서 폭 ${stats.lngSpanMeters} m", color = colors.textSecondary, fontSize = 11.sp)
                            }
                            Text(
                                "시작 ${formatCoord(stats.start.lat)}, ${formatCoord(stats.start.lng)}",
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                            )
                            Text(
                                "종료 ${formatCoord(stats.end.lat)}, ${formatCoord(stats.end.lng)}",
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
                RoutePolylineView(
                    polylineEncoded = item.polylineEncoded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        },
        confirmButton = {
            Row {
                if (item.polylineEncoded != null) {
                    TextButton(onClick = onOpenFullRoute) { Text("전체 경로") }
                }
                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        },
    )
}

@Composable
private fun ChargeDetailDialog(item: ChargeHistoryItem, onDismiss: () -> Unit) {
    val colors = GaugeTheme.colors
    val durationMin = item.endedAtMs?.let { end ->
        ((end - item.startedAtMs) / 60_000).coerceAtLeast(1)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("충전 상세 · ${formatTime(item.startedAtMs)}", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("에너지 ${"%.1f".format(item.energyKwh ?: 0f)} kWh", color = colors.textPrimary, fontSize = 12.sp)
                Text("SOC ${item.startSoc.toInt()} → ${item.endSoc?.toInt() ?: "--"}", color = colors.textSecondary, fontSize = 12.sp)
                Text("Peak ${item.peakKw?.toInt() ?: "--"} kW", color = colors.textSecondary, fontSize = 12.sp)
                durationMin?.let { Text("소요 ${it}분", color = colors.textSecondary, fontSize = 12.sp) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun FleetDetailDialog(item: FleetApiHistoryItem, onDismiss: () -> Unit) {
    val colors = GaugeTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("테슬라API · ${item.category}", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatTime(item.atMs), color = colors.textSecondary, fontSize = 12.sp)
                Text(if (item.ok) "성공" else "실패", color = if (item.ok) colors.socGreen else colors.socRed, fontSize = 12.sp)
                item.detail?.let { Text(it, color = colors.textPrimary, fontSize = 12.sp) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

private fun formatCoord(value: Double): String = "%.5f".format(value)
