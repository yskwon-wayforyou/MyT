package com.myt.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myt.ui.theme.GaugeTheme

@Composable
fun OnboardingScreen(
    isAuthenticated: Boolean,
    configuredVin: String?,
    oauthConfigured: Boolean,
    onTeslaLogin: () -> Unit,
    onComplete: (vin: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var vin by remember(configuredVin) { mutableStateOf(configuredVin.orEmpty()) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MyT 설정", color = GaugeTheme.colors.textPrimary)
        Text(
            "Tesla Fleet API 로그인 후 VIN을 확인하세요.",
            color = GaugeTheme.colors.textSecondary,
        )

        if (oauthConfigured) {
            OutlinedButton(onClick = onTeslaLogin, modifier = Modifier.fillMaxWidth()) {
                Text(if (isAuthenticated) "Tesla 재로그인" else "Tesla로 로그인")
            }
            Text(
                text = if (isAuthenticated) "OAuth 연결됨" else "Tesla 계정 연결 필요",
                color = if (isAuthenticated) GaugeTheme.colors.socGreen else GaugeTheme.colors.socYellow,
            )
        } else {
            Text(
                "tesla.local.properties 에 Client ID/Secret을 입력하세요.",
                color = GaugeTheme.colors.socYellow,
            )
        }

        OutlinedTextField(
            value = vin,
            onValueChange = { vin = it.uppercase() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("VIN") },
            singleLine = true,
        )
        Button(
            onClick = { onComplete(vin.trim()) },
            enabled = vin.length >= 11 && isAuthenticated,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("시작하기")
        }
    }
}

@Composable
fun HomeScreen(
    isConnected: Boolean,
    onOpenGauge: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MyT", color = GaugeTheme.colors.textPrimary)
        Text(
            text = if (isConnected) "차량 연결됨 — Gauge로 이동" else "Bluetooth 연결 대기 중",
            color = GaugeTheme.colors.textSecondary,
        )
        Button(onClick = onOpenGauge, enabled = isConnected, modifier = Modifier.fillMaxWidth()) {
            Text("Gauge 열기")
        }
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("설정")
        }
    }
}
