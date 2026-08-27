package com.myt.ui.gauge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.GaugeState
import com.myt.domain.model.PoiDataStatus
import com.myt.domain.automation.AutomationRepository
import com.myt.ui.automation.AutomationRulesPanel
import com.myt.ui.automation.ClimateSchedulePanel
import com.myt.ui.speedcam.SpeedCamDataPanel
import com.myt.domain.usecase.UiFreshNeed
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import com.myt.ui.theme.accentPurple
import org.koin.mp.KoinPlatform

@Composable
fun MoreHubScreen(
    onSettings: () -> Unit,
    onUsage: () -> Unit,
    onDebug: () -> Unit = {},
    onBack: () -> Unit,
    poiDataStatus: PoiDataStatus? = null,
    poiSyncInProgress: Boolean = false,
    onPoiDataUpdate: () -> Unit = {},
    onPoiDataSettings: () -> Unit = {},
    onAnalytics: () -> Unit = {},
    onCommercial: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val automationRepo = remember { KoinPlatform.getKoin().get<AutomationRepository>() }
    TeslaScreen(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("더보기", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) {
                    Text("닫기", color = colors.accentBlue)
                }
            }
            HubRow("설정", "표시·테마·Tesla 연결", colors.accentBlue, onSettings)
            poiDataStatus?.let { status ->
                SpeedCamDataPanel(
                    status = status,
                    syncInProgress = poiSyncInProgress,
                    onUpdateClick = onPoiDataUpdate,
                    onOpenSettings = onPoiDataSettings,
                )
            }
            HubRow("고급 분석", "배터리 · CO₂ 배지 · CSV 내보내기", colors.accentBlue, onAnalytics)
            // Keep commercial entry above the tall automation list so it stays on first screen.
            HubRow("구독 / Watch", "샌드박스 플랜 · Watch·위젯 미리보기", colors.accentPurple, onCommercial)
            HubRow("API 사용량", "테슬라API 쿼터 · \$10 크레딧", colors.accentPurple, onUsage)
            HubRow("디버그 로그", "런타임 · 크래시", colors.textSecondary, onDebug)
            val billing = remember { KoinPlatform.getKoin().get<com.myt.phase2.BillingGateway>() }
            var planLabel by remember { mutableStateOf("확인 중…") }
            LaunchedEffect(Unit) {
                planLabel = "플랜 ${billing.currentStatus().plan} · sandbox"
            }
            Text(
                "구독: $planLabel",
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            AutomationRulesPanel(repository = automationRepo)
            val climateRepo = remember { KoinPlatform.getKoin().get<com.myt.domain.automation.ClimateScheduleRepository>() }
            ClimateSchedulePanel(repository = climateRepo)
            Text(
                "차량 제어·위젯 미리보기는 구독 / Watch에서 확인할 수 있습니다.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun HubRow(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    val colors = GaugeTheme.colors
    TeslaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accent = accent,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun VehicleDetailSheet(
    state: GaugeState,
    onBack: () -> Unit,
    usePsi: Boolean = true,
    onRequestFreshData: (UiFreshNeed) -> Unit = {},
    controlContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    LaunchedEffect(Unit) {
        when {
            state.tires == null -> onRequestFreshData(UiFreshNeed.Tires)
            state.latitude == null || state.longitude == null -> onRequestFreshData(UiFreshNeed.Location)
            else -> onRequestFreshData(UiFreshNeed.Status)
        }
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
                Text("차량 상세", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("닫기", color = colors.accentBlue) }
            }
            TireGrid(tires = state.tires, compact = false, usePsi = usePsi)
            GMeter(longAccelG = state.longAccelG, latAccelG = state.latAccelG, compact = false)
            ChargePanel(charge = state.charging, socPercent = state.socPercent, compact = false)
            VehicleStatusGrid(state = state, columns = 2)
            controlContent?.invoke()
        }
    }
}
