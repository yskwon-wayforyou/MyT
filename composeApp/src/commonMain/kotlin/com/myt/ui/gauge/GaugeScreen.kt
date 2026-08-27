package com.myt.ui.gauge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.myt.domain.control.VehicleControlGateway
import com.myt.ui.GaugeViewModel
import com.myt.ui.control.QuickControlsPanel
import com.myt.ui.gauge.layout.AdaptiveGaugeLayout
import com.myt.ui.usage.ApiUsageDetailScreen
import org.koin.mp.KoinPlatform

private enum class GaugeOverlay {
    None,
    Usage,
    More,
    VehicleDetail,
}

@Composable
fun GaugeScreen(
    viewModel: GaugeViewModel,
    onVoiceNav: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onDebug: () -> Unit = {},
    onAnalytics: () -> Unit = {},
    onCommercial: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val gaugeState by viewModel.gaugeState.collectAsState()
    val alert by viewModel.speedCamAlert.collectAsState()
    val quota by viewModel.quotaSnapshot.collectAsState()
    val prefs by viewModel.gaugePrefs.collectAsState()
    val useKmh by viewModel.speedUnitKmh.collectAsState()
    val poiDataStatus by viewModel.poiDataStatus.collectAsState()
    val poiSyncInProgress by viewModel.poiSyncInProgress.collectAsState()
    val mapLocation by viewModel.mapDisplayLocation.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val bluetoothPresent by viewModel.bluetoothPresent.collectAsState()
    var overlay by remember { mutableStateOf(GaugeOverlay.None) }

    LaunchedEffect(overlay) {
        if (overlay == GaugeOverlay.More) {
            viewModel.refreshPoiDataStatus()
        }
    }

    when (overlay) {
        GaugeOverlay.Usage -> ApiUsageDetailScreen(
            snapshot = quota,
            onBack = { overlay = GaugeOverlay.None },
            onRefresh = { viewModel.refreshVehicleState() },
            modifier = modifier,
        )
        GaugeOverlay.More -> MoreHubScreen(
            poiDataStatus = poiDataStatus,
            poiSyncInProgress = poiSyncInProgress,
            onPoiDataUpdate = { viewModel.syncPoiData() },
            onPoiDataSettings = {
                overlay = GaugeOverlay.None
                onSettings()
            },
            onSettings = {
                overlay = GaugeOverlay.None
                onSettings()
            },
            onUsage = { overlay = GaugeOverlay.Usage },
            onDebug = {
                overlay = GaugeOverlay.None
                onDebug()
            },
            onBack = { overlay = GaugeOverlay.None },
            onAnalytics = {
                overlay = GaugeOverlay.None
                onAnalytics()
            },
            onCommercial = {
                overlay = GaugeOverlay.None
                onCommercial()
            },
            modifier = modifier,
        )
        GaugeOverlay.VehicleDetail -> {
            val controlGateway = remember { KoinPlatform.getKoin().get<VehicleControlGateway>() }
            val vin by viewModel.configuredVin.collectAsState()
            VehicleDetailSheet(
                state = gaugeState,
                onBack = { overlay = GaugeOverlay.None },
                usePsi = prefs.usePsi(),
                onRequestFreshData = { viewModel.requestFreshData(it) },
                controlContent = {
                    QuickControlsPanel(
                        vin = vin,
                        gateway = controlGateway,
                    )
                },
                modifier = modifier,
            )
        }
        GaugeOverlay.None -> AdaptiveGaugeLayout(
            state = gaugeState,
            alert = alert,
            onVoiceNav = onVoiceNav,
            onHistory = onHistory,
            onSettings = onSettings,
            onMore = { overlay = GaugeOverlay.More },
            onExpandVehicle = { overlay = GaugeOverlay.VehicleDetail },
            quotaSnapshot = quota,
            onUsageClick = { overlay = GaugeOverlay.Usage },
            prefs = prefs,
            useKmh = useKmh,
            bluetoothPresent = bluetoothPresent || gaugeState.bluetoothPresent,
            onRetry = { viewModel.retryConnection() },
            onRequestFreshData = { viewModel.requestFreshData(it) },
            mapDisplayLatitude = mapLocation?.first,
            mapDisplayLongitude = mapLocation?.second,
            mapMarkers = mapMarkers,
            modifier = modifier,
        )
    }
}
