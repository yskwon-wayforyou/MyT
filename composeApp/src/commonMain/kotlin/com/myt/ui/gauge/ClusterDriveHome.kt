package com.myt.ui.gauge

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.UnitConverter
import com.myt.domain.model.AlertLevel
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.SpeedCamAlert
import com.myt.domain.model.TelemetrySource
import com.myt.domain.usecase.UiFreshNeed
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import com.myt.ui.UiLabels
import com.myt.ui.map.LiveMapMarker
import com.myt.ui.map.LiveMapView
import com.myt.ui.speedcam.SpeedCamOverlay
import com.myt.ui.theme.GaugeTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class SecondaryPaneMode { Map, GMeter, Tires }

/**
 * Dual-gauge cluster: primary drive + secondary guidance/map.
 * Portrait → stacked; Landscape → side-by-side.
 */
@Composable
fun ClusterDriveHome(
    state: GaugeState,
    useKmh: Boolean,
    showSpeed: Boolean,
    showGear: Boolean,
    showTires: Boolean,
    chargingMode: Boolean,
    usePsi: Boolean = true,
    alert: SpeedCamAlert? = null,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onMore: () -> Unit,
    onExpandVehicle: () -> Unit,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    mapDisplayLatitude: Double? = null,
    mapDisplayLongitude: Double? = null,
    mapMarkers: List<LiveMapMarker> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A1220), colors.bg, Color(0xFF020208)),
                    radius = 1400f,
                ),
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompactStatusBar(
            state = state,
            showTires = showTires,
            usePsi = usePsi,
            onExpandVehicle = onExpandVehicle,
            onVoiceNav = onVoiceNav,
            onHistory = onHistory,
            onMore = onMore,
        )
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PrimaryDriveGauge(
                            state = state,
                            useKmh = useKmh,
                            showSpeed = showSpeed,
                            showGear = showGear,
                            chargingMode = chargingMode,
                            modifier = Modifier.fillMaxSize(),
                        )
                        SpeedCamOverlay(
                            alert = alert,
                            useKmh = useKmh,
                            visualBoost = state.isSimulated,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 10.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.18f)),
                    )
                    SecondaryGuidanceGauge(
                        state = state,
                        alert = alert,
                        useKmh = useKmh,
                        chargingMode = chargingMode,
                        usePsi = usePsi,
                        mapDisplayLatitude = mapDisplayLatitude,
                        mapDisplayLongitude = mapDisplayLongitude,
                        mapMarkers = mapMarkers,
                        onRequestFreshData = onRequestFreshData,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Box(modifier = Modifier.weight(0.48f).fillMaxWidth()) {
                        PrimaryDriveGauge(
                            state = state,
                            useKmh = useKmh,
                            showSpeed = showSpeed,
                            showGear = showGear,
                            chargingMode = chargingMode,
                            modifier = Modifier.fillMaxSize(),
                        )
                        SpeedCamOverlay(
                            alert = alert,
                            useKmh = useKmh,
                            visualBoost = state.isSimulated,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.18f)),
                    )
                    SecondaryGuidanceGauge(
                        state = state,
                        alert = alert,
                        useKmh = useKmh,
                        chargingMode = chargingMode,
                        usePsi = usePsi,
                        mapDisplayLatitude = mapDisplayLatitude,
                        mapDisplayLongitude = mapDisplayLongitude,
                        mapMarkers = mapMarkers,
                        onRequestFreshData = onRequestFreshData,
                        modifier = Modifier.weight(0.52f).fillMaxWidth(),
                    )
                }
            }
        }
        ClusterIconStrip(state = state)
    }
}

@Composable
private fun CompactStatusBar(
    state: GaugeState,
    showTires: Boolean,
    usePsi: Boolean,
    onExpandVehicle: () -> Unit,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = GaugeTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC121820))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onExpandVehicle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "MyT",
                color = colors.textPrimary,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            )
            StatusIconLabel(
                icon = if (state.charging?.isCharging == true) ClusterIcons.batteryCharging else ClusterIcons.battery,
                label = "${state.socPercent.toInt()}%",
                color = Color.White,
                iconSize = 18.dp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            StatusIconLabel(
                icon = ClusterIcons.range,
                label = "${state.rangeKm.toInt()} km",
                color = colors.textSecondary,
                iconSize = 15.dp,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (showTires) {
                val t = state.tires
                val fmt = { b: Float? -> b?.let { UnitConverter.formatPressure(it, usePsi) } ?: "-" }
                Text(
                    "FL ${fmt(t?.frontLeftBar)} · FR ${fmt(t?.frontRightBar)} · RL ${fmt(t?.rearLeftBar)} · RR ${fmt(t?.rearRightBar)} ${UnitConverter.pressureUnitLabel(usePsi)}",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        TinyAction("음성", onVoiceNav, ClusterIcons.voice)
        TinyAction("기록", onHistory, ClusterIcons.history)
        TinyAction("더보기", onMore, ClusterIcons.more)
    }
}

@Composable
private fun TinyAction(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C2433))
            .border(1.dp, Color(0xFF3D9EFF).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryDriveGauge(
    state: GaugeState,
    useKmh: Boolean,
    showSpeed: Boolean,
    showGear: Boolean,
    chargingMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val powerNorm = ((state.powerKw ?: 0f) / 200f).coerceIn(0f, 1f)
    val arcProgress by animateFloatAsState(
        targetValue = if (chargingMode) (state.socPercent / 100f).coerceIn(0f, 1f) else powerNorm,
        animationSpec = spring(stiffness = 90f),
        label = "arc",
    )
    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowPulse",
    )
    GlassPane(
        modifier = modifier,
        glow = if (chargingMode) Color(0xFF30D158) else Color(0xFF3D9EFF),
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                DualNeonArcs(
                    progress = arcProgress,
                    warn = if (chargingMode) false else (state.powerKw ?: 0f) > 120f,
                    glowAlpha = glowPulse,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
                if (chargingMode) {
                    ChargingPrimaryContent(state = state, colors = colors)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TurnSignalRow(state = state)
                        if (showGear) GearPill(state.gear)
                        if (showSpeed) {
                            Text(
                                UnitConverter.formatSpeed(state.speedKmh, useKmh),
                                color = Color.White,
                                fontSize = 84.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 84.sp,
                            )
                            Text(
                                UnitConverter.speedUnitLabel(useKmh),
                                color = Color(0xFF3D9EFF),
                                fontSize = 20.sp,
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        SourceCaption(state.speedSource)
                    }
                }
            }
        },
    )
}

@Composable
private fun ChargingPrimaryContent(
    state: GaugeState,
    colors: com.myt.ui.theme.GaugeColors,
) {
    val charge = state.charging
    // Dial-style center (like speed) so DualNeonArcs stay readable — metrics sit below the arc.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TurnSignalRow(state = state)
            Text(
                "${state.socPercent.toInt()}",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 72.sp,
            )
            Text(
                "%",
                color = Color(0xFF30D158),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                "CHARGING · ${state.rangeKm.toInt()} km",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompactChargeChip(
                charge?.chargeRateKw?.let { "${"%.1f".format(it)} kW" } ?: "--",
                Modifier.weight(1f),
            )
            CompactChargeChip(
                charge?.timeToFullMinutes?.let { "${it}분" } ?: "--",
                Modifier.weight(1f),
            )
            CompactChargeChip(
                charge?.chargeLimitPercent?.let { "한도 $it%" } ?: "--",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompactChargeChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0A1018))
            .padding(horizontal = 6.dp, vertical = 6.dp),
    )
}

@Composable
private fun ChargeMetricRow(
    left: Pair<String, String>,
    right: Pair<String, String>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChargeMetricChip(left.first, left.second, Modifier.weight(1f))
        ChargeMetricChip(right.first, right.second, Modifier.weight(1f))
    }
}

@Composable
private fun ChargeMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TurnSignalRow(state: GaugeState) {
    val blink by rememberInfiniteTransition(label = "sig").animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "sigBlink",
    )
    val leftOn = state.turnSignalLeft == true || state.hazardLightsOn == true
    val rightOn = state.turnSignalRight == true || state.hazardLightsOn == true
    val known = state.turnSignalLeft != null || state.turnSignalRight != null || state.hazardLightsOn != null
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        TurnArrow(
            pointingLeft = true,
            active = leftOn,
            alpha = if (leftOn) blink else if (known) 0.35f else 0.2f,
        )
        StatusIconLabel(
            icon = when {
                state.hazardLightsOn == true -> ClusterIcons.warning
                leftOn -> ClusterIcons.turnLeft
                rightOn -> ClusterIcons.turnRight
                else -> ClusterIcons.info
            },
            label = if (known) {
                when {
                    state.hazardLightsOn == true -> "HAZARD"
                    leftOn && rightOn -> "BOTH"
                    leftOn -> "LEFT"
                    rightOn -> "RIGHT"
                    else -> "OFF"
                }
            } else {
                "지시등 —"
            },
            color = Color.White.copy(alpha = if (known) 0.65f else 0.35f),
            iconSize = 12.dp,
            fontSize = 11.sp,
        )
        TurnArrow(
            pointingLeft = false,
            active = rightOn,
            alpha = if (rightOn) blink else if (known) 0.35f else 0.2f,
        )
    }
}

@Composable
private fun TurnArrow(pointingLeft: Boolean, active: Boolean, alpha: Float) {
    val color = if (active) Color(0xFF30D158).copy(alpha = alpha) else Color.White.copy(alpha = alpha)
    Canvas(modifier = Modifier.size(28.dp, 20.dp), onDraw = {
        val path = Path().apply {
            if (pointingLeft) {
                moveTo(size.width * 0.85f, size.height * 0.15f)
                lineTo(size.width * 0.15f, size.height * 0.5f)
                lineTo(size.width * 0.85f, size.height * 0.85f)
                close()
            } else {
                moveTo(size.width * 0.15f, size.height * 0.15f)
                lineTo(size.width * 0.85f, size.height * 0.5f)
                lineTo(size.width * 0.15f, size.height * 0.85f)
                close()
            }
        }
        drawPath(path, color)
    })
}

@Composable
private fun SecondaryGuidanceGauge(
    state: GaugeState,
    alert: SpeedCamAlert?,
    useKmh: Boolean,
    chargingMode: Boolean,
    usePsi: Boolean,
    mapDisplayLatitude: Double?,
    mapDisplayLongitude: Double?,
    mapMarkers: List<LiveMapMarker>,
    onRequestFreshData: (UiFreshNeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDriving = state.gear == Gear.DRIVE || state.gear == Gear.REVERSE || state.speedKmh >= 3f
    var mode by remember { mutableStateOf(SecondaryPaneMode.Map) }
    val effectiveMode = when {
        mode == SecondaryPaneMode.GMeter && !isDriving -> SecondaryPaneMode.Map
        mode == SecondaryPaneMode.Tires && isDriving -> SecondaryPaneMode.Map
        else -> mode
    }
    LaunchedEffect(effectiveMode, state.latitude, state.longitude, state.tires) {
        when (effectiveMode) {
            SecondaryPaneMode.Map -> {
                if (state.latitude == null || state.longitude == null) {
                    onRequestFreshData(UiFreshNeed.Location)
                }
            }
            SecondaryPaneMode.Tires -> {
                if (state.tires == null) {
                    onRequestFreshData(UiFreshNeed.Tires)
                }
            }
            SecondaryPaneMode.GMeter -> Unit
        }
    }
    val panelGlow = when {
        effectiveMode != SecondaryPaneMode.Map -> Color(0xFFBF5AF2)
        alert != null -> Color(0xFFFF8A3D)
        state.navigation?.isActive == true -> Color(0xFF30D158)
        chargingMode -> Color(0xFF30D158)
        else -> Color(0xFF5AC8FA)
    }
    val title = when (effectiveMode) {
        SecondaryPaneMode.Map -> when {
            alert != null -> "SPEED CAM"
            state.navigation?.isActive == true -> "NAVIGATION"
            chargingMode -> "CHARGING MAP"
            isDriving -> "DRIVE MAP"
            else -> "VEHICLE MAP"
        }
        SecondaryPaneMode.GMeter -> "G-METER"
        SecondaryPaneMode.Tires -> "TIRES"
    }
    GlassPane(
        modifier = modifier,
        glow = panelGlow,
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                SecondaryHeader(
                    title = title,
                    hint = if (isDriving) "탭: 지도 ↔ G-meter" else "탭: 지도 ↔ 타이어",
                    onCycle = {
                        mode = when {
                            isDriving -> if (effectiveMode == SecondaryPaneMode.Map) {
                                SecondaryPaneMode.GMeter
                            } else {
                                SecondaryPaneMode.Map
                            }
                            else -> if (effectiveMode == SecondaryPaneMode.Map) {
                                SecondaryPaneMode.Tires
                            } else {
                                SecondaryPaneMode.Map
                            }
                        }
                    },
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (effectiveMode) {
                        SecondaryPaneMode.Map -> MapGuidanceContent(
                            state = state,
                            alert = alert,
                            useKmh = useKmh,
                            chargingMode = chargingMode,
                            isDriving = isDriving,
                            mapDisplayLatitude = mapDisplayLatitude,
                            mapDisplayLongitude = mapDisplayLongitude,
                            mapMarkers = mapMarkers,
                        )
                        SecondaryPaneMode.GMeter -> Box(
                            Modifier.fillMaxSize().padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            GMeter(
                                longAccelG = state.longAccelG,
                                latAccelG = state.latAccelG,
                                compact = true,
                                embedded = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        SecondaryPaneMode.Tires -> Box(
                            Modifier.fillMaxSize().padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TireGrid(
                                tires = state.tires,
                                compact = true,
                                usePsi = usePsi,
                                embedded = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SecondaryHeader(
    title: String,
    hint: String,
    onCycle: () -> Unit,
) {
    val headerIcon = when {
        "CAM" in title -> ClusterIcons.speedCam
        "NAV" in title -> ClusterIcons.navigation
        "G-METER" in title -> ClusterIcons.gMeter
        "TIRE" in title -> ClusterIcons.tire
        "CHARGING" in title -> ClusterIcons.charging
        else -> ClusterIcons.map
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .background(Color(0xCC0A1018))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(headerIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(title, color = Color.White, fontSize = 13.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(hint, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MapGuidanceContent(
    state: GaugeState,
    alert: SpeedCamAlert?,
    useKmh: Boolean,
    chargingMode: Boolean,
    isDriving: Boolean,
    mapDisplayLatitude: Double?,
    mapDisplayLongitude: Double?,
    mapMarkers: List<LiveMapMarker>,
) {
    val radius = when {
        alert != null -> 500
        state.navigation?.isActive == true -> 800
        isDriving -> 750
        else -> 1_000
    }
    val mapLat = mapDisplayLatitude ?: state.latitude
    val mapLng = mapDisplayLongitude ?: state.longitude
    Box(modifier = Modifier.fillMaxSize()) {
        LiveMapView(
            latitude = mapLat,
            longitude = mapLng,
            headingDegrees = state.headingDegrees,
            radiusMeters = radius,
            markers = mapMarkers,
            modifier = Modifier.fillMaxSize(),
        )
        GuidanceOverlay(
            state = state,
            alert = null,
            useKmh = useKmh,
            chargingMode = chargingMode,
            radiusMeters = radius,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp),
        )
    }
}

@Composable
private fun GuidanceOverlay(
    state: GaugeState,
    alert: SpeedCamAlert?,
    useKmh: Boolean,
    chargingMode: Boolean,
    radiusMeters: Int,
    modifier: Modifier = Modifier,
) {
    val bg = Color(0xE6121820)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            state.navigation?.isActive == true -> {
                val nav = state.navigation
                StatusIconLabel(
                    icon = ClusterIcons.navigation,
                    label = nav?.destinationName ?: "목적지",
                    color = Color(0xFF30D158),
                    iconSize = 16.dp,
                    fontSize = 14.sp,
                )
                Text(
                    listOfNotNull(
                        nav?.etaMinutes?.let { "ETA ${it}분" },
                        nav?.distanceKm?.let { "${"%.1f".format(it)} km" },
                        UiLabels.laneTurnUnsupported,
                    ).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }
            chargingMode -> {
                val ch = state.charging
                StatusIconLabel(
                    icon = ClusterIcons.charging,
                    label = com.myt.domain.charge.ChargeStateNormalizer.uiChargeLabel(state.socPercent, ch),
                    color = Color(0xFF30D158),
                    iconSize = 16.dp,
                    fontSize = 14.sp,
                )
                StatusIconLabel(
                    icon = if (state.latitude != null) ClusterIcons.place else ClusterIcons.info,
                    label = listOfNotNull(
                        ch?.chargeRateKw?.let { "${"%.1f".format(it)} kW" },
                        ch?.timeToFullMinutes?.let { "완충 ${it}분" },
                        ch?.chargeLimitPercent?.let { "한도 ${it}%" },
                        "반경 ${radiusMeters}m",
                    ).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.75f),
                    iconSize = 13.dp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            else -> {
                StatusIconLabel(
                    icon = if (state.latitude != null) ClusterIcons.place else ClusterIcons.info,
                    label = if (state.latitude != null) "차량 위치" else UiLabels.mapLocationWaiting,
                    color = Color(0xFF5AC8FA),
                    iconSize = 16.dp,
                    fontSize = 14.sp,
                )
                Text(
                    "반경 ${radiusMeters}m · 지도↔타이어 탭",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GearPill(gear: Gear) {
    val colors = GaugeTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Gear.PARK, Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE).forEach { g ->
            val on = g == gear
            val active = when (g) {
                Gear.PARK -> colors.gearP
                Gear.REVERSE -> colors.gearR
                Gear.NEUTRAL -> colors.gearN
                Gear.DRIVE -> colors.gearD
            }
            Text(
                g.displayLabel(),
                color = if (on) active else colors.textSecondary.copy(alpha = 0.4f),
                fontSize = if (on) 22.sp else 14.sp,
                fontWeight = if (on) FontWeight.Black else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SourceCaption(source: TelemetrySource) {
    val (label, tint) = when (source) {
        TelemetrySource.Device -> UiLabels.telemetrySource(TelemetrySource.Device) to Color(0xFF30D158)
        TelemetrySource.Degraded -> UiLabels.telemetrySource(TelemetrySource.Degraded) to Color(0xFFFFB020)
        TelemetrySource.Fleet -> UiLabels.telemetrySource(TelemetrySource.Fleet) to Color(0xFFFF9F0A)
        TelemetrySource.Cache -> UiLabels.telemetrySource(TelemetrySource.Cache) to Color(0xFF9A9AA3)
        TelemetrySource.None -> return
    }
    StatusIconLabel(
        icon = telemetryIcon(source),
        label = label,
        color = tint,
        iconSize = 13.dp,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun DualNeonArcs(
    progress: Float,
    warn: Boolean,
    glowAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val cyan = Color(0xFF3D9EFF)
    val amber = Color(0xFFFF8A3D)
    Canvas(
        modifier = modifier,
        onDraw = {
            val strokeW = size.minDimension * 0.04f
            val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)
            val pad = size.minDimension * 0.07f
            val diameter = min(size.width, size.height) - pad * 2
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val glowTopLeft = Offset(topLeft.x - 6f, topLeft.y - 6f)
            drawArc(
                color = (if (warn) amber else cyan).copy(alpha = glowAlpha * 0.22f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = glowTopLeft,
                size = Size(diameter + 12f, diameter + 12f),
                style = Stroke(width = strokeW * 2f, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color(0x22FFFFFF),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(cyan, cyan, amber)),
                startAngle = 140f,
                sweepAngle = 260f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rOuter = diameter / 2f + 4f
            val rInner = rOuter - 12f
            for (i in 0..10) {
                val ang = (140.0 + 26.0 * i) * kotlin.math.PI / 180.0
                val c = cos(ang).toFloat()
                val s = sin(ang).toFloat()
                drawLine(
                    color = if (i >= 8) amber else Color.White.copy(alpha = 0.5f),
                    start = Offset(cx + c * rInner, cy + s * rInner),
                    end = Offset(cx + c * rOuter, cy + s * rOuter),
                    strokeWidth = if (i % 2 == 0) 3f else 1.8f,
                    cap = StrokeCap.Round,
                )
            }
        },
    )
}

@Composable
private fun GlassPane(
    modifier: Modifier = Modifier,
    glow: Color,
    content: @Composable () -> Unit,
) {
    // Frameless cluster pane — group separation uses mid hairlines, not stacked borders.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xCC121820), Color(0xF0080C12))))
            .border(width = 0.5.dp, color = glow.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun ClusterIconStrip(state: GaugeState, modifier: Modifier = Modifier) {
    val colors = GaugeTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(
            if (state.bluetoothPresent) "BT ON" else "BT OFF",
            state.bluetoothPresent,
            bluetoothIcon(state.bluetoothPresent),
        )
        StatusChip(
            if (state.locked == true) "잠김" else if (state.locked == false) "열림" else "잠금",
            state.locked == false,
            lockIcon(state.locked),
        )
        StatusChip("공조", state.climateOn == true, ClusterIcons.climate)
        StatusChip(
            if (state.charging?.isCharging == true) "충전" else "대기",
            state.charging?.isCharging == true,
            if (state.charging?.isCharging == true) ClusterIcons.charging else ClusterIcons.idle,
        )
        TelemetrySourceDot(state.speedSource)
        val connLabel = UiLabels.connectionShort(state.connection)
        StatusIconLabel(
            icon = connectionIcon(state.connection),
            label = connLabel,
            color = colors.textSecondary,
            iconSize = 13.dp,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    on: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val bg = if (on) Color(0xFF30D158).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f)
    val fg = if (on) Color(0xFF30D158) else Color.White.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(13.dp))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TelemetrySourceDot(source: TelemetrySource, modifier: Modifier = Modifier) {
    val tint = when (source) {
        TelemetrySource.Device -> Color(0xFF30D158)
        TelemetrySource.Degraded -> Color(0xFFFFB020)
        TelemetrySource.Fleet -> Color(0xFFFF9F0A)
        TelemetrySource.Cache -> Color(0xFF9A9AA3)
        TelemetrySource.None -> Color(0xFF9A9AA3).copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(tint)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
    )
}
