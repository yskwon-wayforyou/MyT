package com.myt.ui.gauge.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeDisplayPrefs
import com.myt.domain.model.GaugeField
import com.myt.domain.model.GaugeState
import com.myt.domain.model.LayoutConfig
import com.myt.domain.model.SpeedCamAlert
import com.myt.ui.map.LiveMapMarker
import com.myt.domain.model.WindowHeightSizeClass
import com.myt.domain.model.WindowWidthSizeClass
import com.myt.domain.quota.QuotaSnapshot
import com.myt.domain.quota.emptyQuotaSnapshot
import com.myt.domain.usecase.AdaptiveLayoutUseCase
import com.myt.domain.usecase.UiFreshNeed
import com.myt.ui.UiLabels
import com.myt.ui.gauge.ChargePanel
import com.myt.ui.gauge.ClusterDriveHome
import com.myt.ui.gauge.ConnectionErrorBanner
import com.myt.ui.simulation.SimulationTestBannerOverlay
import com.myt.ui.gauge.GMeter
import com.myt.ui.gauge.SpeedDisplay
import com.myt.ui.gauge.StatusIconLabel
import com.myt.ui.gauge.TelemetrySourceDot
import com.myt.ui.gauge.TireGrid
import com.myt.ui.gauge.VehicleStatusGrid
import com.myt.ui.gauge.connectionIcon
import com.myt.ui.usage.ApiUsageChip
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaGlassPanel
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import com.myt.ui.ConnectionErrorKind
import com.myt.ui.gauge.DriveSafetyBanner
import com.myt.ui.gauge.connectionErrorKind
import com.myt.domain.model.Gear

@Composable
fun AdaptiveGaugeLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    quotaSnapshot: QuotaSnapshot = emptyQuotaSnapshot(),
    onUsageClick: () -> Unit = {},
    prefs: GaugeDisplayPrefs = GaugeDisplayPrefs(),
    useKmh: Boolean = true,
    bluetoothPresent: Boolean = false,
    onRetry: () -> Unit = {},
    onHome: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onExpandVehicle: (() -> Unit)? = null,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    mapDisplayLatitude: Double? = null,
    mapDisplayLongitude: Double? = null,
    mapMarkers: List<LiveMapMarker> = emptyList(),
    showDriveSafetyBanner: Boolean = false,
    onAcknowledgeDriveSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val layoutUseCase = remember { AdaptiveLayoutUseCase() }
    val more = onMore ?: onSettings
    val expand = onExpandVehicle ?: {}
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.Compact
            maxWidth < 840.dp -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
        val heightClass = when {
            maxHeight < 480.dp -> WindowHeightSizeClass.Compact
            maxHeight < 900.dp -> WindowHeightSizeClass.Medium
            else -> WindowHeightSizeClass.Expanded
        }
        val config = remember(maxWidth, maxHeight, prefs.layoutMode) {
            layoutUseCase.computeLayout(
                widthClass = widthClass,
                heightClass = heightClass,
                mode = prefs.layoutMode,
                isLandscapeAspect = maxWidth > maxHeight,
            )
        }
        val camAlert = alert.takeIf { prefs.showsOnDriveHome(GaugeField.SpeedCam) }
        val errorKind = connectionErrorKind(state.connection, bluetoothPresent)
        when (config) {
            LayoutConfig.Landscape -> GaugeLandscapeLayout(
                state = state,
                alert = camAlert,
                errorKind = errorKind,
                onVoiceNav = onVoiceNav,
                onHistory = onHistory,
                onSettings = onSettings,
                onMore = more,
                onExpandVehicle = expand,
                onRetry = onRetry,
                onHome = onHome,
                quotaSnapshot = quotaSnapshot,
                onUsageClick = onUsageClick,
                prefs = prefs,
                useKmh = useKmh,
                onRequestFreshData = onRequestFreshData,
                mapDisplayLatitude = mapDisplayLatitude,
                mapDisplayLongitude = mapDisplayLongitude,
                mapMarkers = mapMarkers,
                showDriveSafetyBanner = showDriveSafetyBanner,
                onAcknowledgeDriveSafety = onAcknowledgeDriveSafety,
            )
            LayoutConfig.SinglePane -> GaugeSinglePaneLayout(
                state = state,
                alert = camAlert,
                errorKind = errorKind,
                onVoiceNav = onVoiceNav,
                onHistory = onHistory,
                onSettings = onSettings,
                onMore = more,
                onExpandVehicle = expand,
                onRetry = onRetry,
                onHome = onHome,
                quotaSnapshot = quotaSnapshot,
                onUsageClick = onUsageClick,
                prefs = prefs,
                useKmh = useKmh,
                onRequestFreshData = onRequestFreshData,
                mapDisplayLatitude = mapDisplayLatitude,
                mapDisplayLongitude = mapDisplayLongitude,
                mapMarkers = mapMarkers,
                showDriveSafetyBanner = showDriveSafetyBanner,
                onAcknowledgeDriveSafety = onAcknowledgeDriveSafety,
            )
            LayoutConfig.TwoPane -> GaugeTwoPaneLayout(
                state = state,
                alert = camAlert,
                errorKind = errorKind,
                onVoiceNav = onVoiceNav,
                onHistory = onHistory,
                onSettings = onSettings,
                onMore = more,
                onExpandVehicle = expand,
                onRetry = onRetry,
                onHome = onHome,
                quotaSnapshot = quotaSnapshot,
                onUsageClick = onUsageClick,
                prefs = prefs,
                useKmh = useKmh,
                onRequestFreshData = onRequestFreshData,
                showDriveSafetyBanner = showDriveSafetyBanner,
                onAcknowledgeDriveSafety = onAcknowledgeDriveSafety,
            )
            LayoutConfig.ThreePane -> GaugeThreePaneLayout(
                state = state,
                alert = camAlert,
                errorKind = errorKind,
                onVoiceNav = onVoiceNav,
                onHistory = onHistory,
                onSettings = onSettings,
                onMore = more,
                onExpandVehicle = expand,
                onRetry = onRetry,
                onHome = onHome,
                quotaSnapshot = quotaSnapshot,
                onUsageClick = onUsageClick,
                prefs = prefs,
                useKmh = useKmh,
                onRequestFreshData = onRequestFreshData,
                showDriveSafetyBanner = showDriveSafetyBanner,
                onAcknowledgeDriveSafety = onAcknowledgeDriveSafety,
            )
        }
    }
}

@Composable
fun GaugeSinglePaneLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    errorKind: ConnectionErrorKind = ConnectionErrorKind.None,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit = onSettings,
    onExpandVehicle: () -> Unit = {},
    onRetry: () -> Unit = {},
    onHome: (() -> Unit)? = null,
    quotaSnapshot: QuotaSnapshot = emptyQuotaSnapshot(),
    onUsageClick: () -> Unit = {},
    prefs: GaugeDisplayPrefs = GaugeDisplayPrefs(),
    useKmh: Boolean = true,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    mapDisplayLatitude: Double? = null,
    mapDisplayLongitude: Double? = null,
    mapMarkers: List<LiveMapMarker> = emptyList(),
    showDriveSafetyBanner: Boolean = false,
    onAcknowledgeDriveSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val charging = state.charging?.isCharging == true
    val parked = state.gear == Gear.PARK && !charging
    TeslaScreen(modifier, showCar = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ConnectionErrorBanner(kind = errorKind, onRetry = onRetry, onDismissHome = onHome)
                DriveSafetyBanner(
                    visible = showDriveSafetyBanner,
                    onAcknowledge = onAcknowledgeDriveSafety,
                )
                ClusterDriveHome(
                    state = state,
                    useKmh = useKmh,
                    showSpeed = prefs.showsOnDriveHome(GaugeField.Speed) && !parked,
                    showGear = prefs.showsOnDriveHome(GaugeField.Gear),
                    showTires = prefs.showsOnDriveHome(GaugeField.Tires),
                    chargingMode = charging,
                    usePsi = prefs.usePsi(),
                    alert = alert,
                    onVoiceNav = onVoiceNav,
                    onHistory = onHistory,
                    onMore = onMore,
                    onExpandVehicle = onExpandVehicle,
                    onRequestFreshData = onRequestFreshData,
                    mapDisplayLatitude = mapDisplayLatitude,
                    mapDisplayLongitude = mapDisplayLongitude,
                    mapMarkers = mapMarkers,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            SimulationTestBannerOverlay(
                isSimulated = state.isSimulated,
                scenarioLabel = state.simulationLabel,
                compactStatusBar = false,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
fun GaugeLandscapeLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    errorKind: ConnectionErrorKind = ConnectionErrorKind.None,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit = onSettings,
    onExpandVehicle: () -> Unit = {},
    onRetry: () -> Unit = {},
    onHome: (() -> Unit)? = null,
    quotaSnapshot: QuotaSnapshot = emptyQuotaSnapshot(),
    onUsageClick: () -> Unit = {},
    prefs: GaugeDisplayPrefs = GaugeDisplayPrefs(),
    useKmh: Boolean = true,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    mapDisplayLatitude: Double? = null,
    mapDisplayLongitude: Double? = null,
    mapMarkers: List<LiveMapMarker> = emptyList(),
    showDriveSafetyBanner: Boolean = false,
    onAcknowledgeDriveSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val charging = state.charging?.isCharging == true
    val parked = state.gear == Gear.PARK && !charging
    TeslaScreen(modifier, showCar = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ConnectionErrorBanner(kind = errorKind, onRetry = onRetry, onDismissHome = onHome)
                DriveSafetyBanner(
                    visible = showDriveSafetyBanner,
                    onAcknowledge = onAcknowledgeDriveSafety,
                )
                ClusterDriveHome(
                    state = state,
                    useKmh = useKmh,
                    showSpeed = prefs.showsOnDriveHome(GaugeField.Speed) && !parked,
                    showGear = prefs.showsOnDriveHome(GaugeField.Gear),
                    showTires = prefs.showsOnDriveHome(GaugeField.Tires),
                    chargingMode = charging,
                    usePsi = prefs.usePsi(),
                    alert = alert,
                    onVoiceNav = onVoiceNav,
                    onHistory = onHistory,
                    onMore = onMore,
                    onExpandVehicle = onExpandVehicle,
                    onRequestFreshData = onRequestFreshData,
                    mapDisplayLatitude = mapDisplayLatitude,
                    mapDisplayLongitude = mapDisplayLongitude,
                    mapMarkers = mapMarkers,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            SimulationTestBannerOverlay(
                isSimulated = state.isSimulated,
                scenarioLabel = state.simulationLabel,
                compactStatusBar = true,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
fun GaugeTwoPaneLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    errorKind: ConnectionErrorKind = ConnectionErrorKind.None,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit = onSettings,
    onExpandVehicle: () -> Unit = {},
    onRetry: () -> Unit = {},
    onHome: (() -> Unit)? = null,
    quotaSnapshot: QuotaSnapshot = emptyQuotaSnapshot(),
    onUsageClick: () -> Unit = {},
    prefs: GaugeDisplayPrefs = GaugeDisplayPrefs(),
    useKmh: Boolean = true,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    showDriveSafetyBanner: Boolean = false,
    onAcknowledgeDriveSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GaugeLandscapeLayout(
        state = state,
        alert = alert,
        errorKind = errorKind,
        onVoiceNav = onVoiceNav,
        onHistory = onHistory,
        onSettings = onSettings,
        onMore = onMore,
        onExpandVehicle = onExpandVehicle,
        onRetry = onRetry,
        onHome = onHome,
        quotaSnapshot = quotaSnapshot,
        onUsageClick = onUsageClick,
        prefs = prefs,
        useKmh = useKmh,
        onRequestFreshData = onRequestFreshData,
        showDriveSafetyBanner = showDriveSafetyBanner,
        onAcknowledgeDriveSafety = onAcknowledgeDriveSafety,
        modifier = modifier,
    )
}

@Composable
fun GaugeThreePaneLayout(
    state: GaugeState,
    alert: SpeedCamAlert?,
    errorKind: ConnectionErrorKind = ConnectionErrorKind.None,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit = onSettings,
    onExpandVehicle: () -> Unit = {},
    onRetry: () -> Unit = {},
    onHome: (() -> Unit)? = null,
    quotaSnapshot: QuotaSnapshot = emptyQuotaSnapshot(),
    onUsageClick: () -> Unit = {},
    prefs: GaugeDisplayPrefs = GaugeDisplayPrefs(),
    useKmh: Boolean = true,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    showDriveSafetyBanner: Boolean = false,
    onAcknowledgeDriveSafety: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GaugeLandscapeLayout(
        state = state,
        alert = alert,
        errorKind = errorKind,
        onVoiceNav = onVoiceNav,
        onHistory = onHistory,
        onSettings = onSettings,
        onMore = onMore,
        onExpandVehicle = onExpandVehicle,
        onRetry = onRetry,
        onHome = onHome,
        quotaSnapshot = quotaSnapshot,
        onUsageClick = onUsageClick,
        prefs = prefs,
        useKmh = useKmh,
        onRequestFreshData = onRequestFreshData,
        showDriveSafetyBanner = showDriveSafetyBanner,
        onAcknowledgeDriveSafety = onAcknowledgeDriveSafety,
        modifier = modifier,
    )
}

@Composable
private fun GaugeDetailWidgets(
    state: GaugeState,
    prefs: GaugeDisplayPrefs,
    compact: Boolean = false,
) {
    if (prefs.shows(GaugeField.Charge)) {
        ChargePanel(
            charge = state.charging,
            socPercent = state.socPercent,
            compact = compact,
        )
    }
    if (prefs.shows(GaugeField.Tires)) {
        TireGrid(tires = state.tires, compact = compact, usePsi = prefs.usePsi())
    }
    if (prefs.shows(GaugeField.Power)) {
        GMeter(
            longAccelG = state.longAccelG,
            latAccelG = state.latAccelG,
            compact = compact,
        )
    }
}

@Composable
private fun StatusBar(
    state: GaugeState,
    quotaSnapshot: QuotaSnapshot,
    onUsageClick: () -> Unit,
    compact: Boolean = false,
    showUsageChip: Boolean = true,
) {
    val colors = GaugeTheme.colors
    val (label, connected) = when (state.connection) {
        ConnectionStatus.Disconnected -> UiLabels.connection(ConnectionStatus.Disconnected) to false
        ConnectionStatus.BluetoothOnly -> UiLabels.connection(ConnectionStatus.BluetoothOnly) to true
        ConnectionStatus.FleetConnected -> UiLabels.connection(ConnectionStatus.FleetConnected) to true
        ConnectionStatus.Sleeping -> UiLabels.connection(ConnectionStatus.Sleeping) to false
        ConnectionStatus.Error -> UiLabels.connection(ConnectionStatus.Error) to false
        ConnectionStatus.QuotaHold -> UiLabels.connection(ConnectionStatus.QuotaHold) to false
    }
    val extras = buildList {
        if (state.bluetoothPresent) add("BT")
        state.locked?.let { add(if (it) "잠김" else "열림") }
        if (state.charging?.isCharging == true) add("충전")
        if (state.isSleeping) add("대기")
        if (state.connection == ConnectionStatus.QuotaHold) add("한도")
        if (state.isSimulated) add("SIM")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeslaGlassPanel(
            modifier = Modifier.weight(1f),
            accent = colors.accentBlue,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "MyT",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (connected) colors.socGreen else colors.socRed.copy(alpha = 0.7f))
                            .padding(5.dp),
                    )
                    TelemetrySourceDot(state.speedSource)
                    StatusIconLabel(
                        icon = connectionIcon(state.connection),
                        label = (listOf(label) + extras).joinToString(" · "),
                        color = colors.textSecondary,
                        iconSize = 12.dp,
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showUsageChip) {
                        ApiUsageChip(
                            snapshot = quotaSnapshot,
                            onClick = onUsageClick,
                            compact = compact,
                        )
                    }
                }
            }
        }
    }
}


