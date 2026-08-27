package com.myt.ui.commercial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.phase2.BillingGateway
import com.myt.phase2.SubscriptionFeatures
import com.myt.phase2.SubscriptionPlan
import com.myt.phase2.WidgetSnapshotProvider
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import kotlinx.coroutines.launch

@Composable
fun CommercialHubScreen(
    billing: BillingGateway,
    widgetProvider: WidgetSnapshotProvider,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val scope = rememberCoroutineScope()
    var plan by remember { mutableStateOf(SubscriptionPlan.Free) }
    var statusText by remember { mutableStateOf("") }
    val widget = remember { widgetProvider.current() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        plan = billing.currentStatus().plan
    }

    TeslaScreen(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("구독 · 위젯", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("닫기", color = colors.accentBlue) }
            }

            TeslaCard(accent = colors.accentBlue) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("샌드박스 구독 (M37)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Play Billing 전 로컬 플랜입니다. 스토어 연동 시 동일 게이트가 적용됩니다.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubscriptionPlan.entries.forEach { p ->
                            FilterChip(
                                selected = plan == p,
                                onClick = {
                                    scope.launch {
                                        billing.startCheckout(p)
                                        plan = billing.currentStatus().plan
                                        statusText = "${plan.name} 적용됨"
                                    }
                                },
                                label = { Text(p.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accent.copy(alpha = 0.35f),
                                ),
                            )
                        }
                    }
                    if (statusText.isNotBlank()) {
                        Text(statusText, color = Color(0xFF30D158), fontSize = 12.sp)
                    }
                    Text(
                        "카메라 데모: ${if (SubscriptionFeatures.liveCameraDemo(plan)) "ON" else "잠김"}",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            TeslaCard(accent = Color(0xFFBF5AF2)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("홈 위젯 (Glance)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(Color(0xFF121820))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("MyT", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "SOC ${widget?.socPercent ?: 0}% · ${widget?.rangeKm ?: 0} km",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                        Text(
                            if (widget?.locked == true) "잠김" else "열림",
                            color = Color(0xFF30D158),
                            fontSize = 13.sp,
                        )
                    }
                    Text(
                        "런처에 MyT 위젯을 추가하면 SOC·주행거리가 갱신됩니다.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
