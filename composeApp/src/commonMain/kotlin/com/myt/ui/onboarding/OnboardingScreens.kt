package com.myt.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.VinValidator
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaHeroImage
import com.myt.ui.theme.TeslaScreen

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
    var vinError by remember { mutableStateOf<String?>(null) }
    val colors = GaugeTheme.colors

    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "MyT",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(18.dp))
            TeslaHeroImage(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "MODEL 3",
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
            )
            Text(
                "Tesla 계정으로 연결하면 바로 계기판을 쓸 수 있습니다.\n" +
                    "Bluetooth로 차량이 연결된 동안만 기기 GPS로 속도와 과속단속을 갱신해 API 사용량을 줄입니다.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            if (oauthConfigured) {
                Button(
                    onClick = onTeslaLogin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textPrimary,
                    ),
                ) {
                    Text(if (isAuthenticated) "Tesla 재연결" else "Tesla로 연결", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = if (isAuthenticated) "연결됨" else "한 번만 인증하면 됩니다",
                    color = if (isAuthenticated) colors.socGreen else colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                Text("앱 설정에 Client ID가 없습니다.", color = colors.socYellow)
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = vin,
                onValueChange = {
                    vin = VinValidator.normalize(it)
                    vinError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("VIN") },
                singleLine = true,
                isError = vinError != null,
                supportingText = vinError?.let { err -> { Text(err, color = colors.socRed) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.stroke,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val message = VinValidator.validationMessage(vin)
                    if (message != null) {
                        vinError = message
                        return@Button
                    }
                    onComplete(vin.trim())
                },
                enabled = isAuthenticated && VinValidator.isValid(vin),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceHigh,
                    contentColor = colors.textPrimary,
                    disabledContainerColor = colors.surface,
                    disabledContentColor = colors.textSecondary,
                ),
            ) {
                Text("시작하기", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun HomeScreen(
    isConnected: Boolean,
    onOpenGauge: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "MyT",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(20.dp))
            TeslaHeroImage(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).weight(1f, fill = false),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "MODEL 3",
                color = colors.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 5.sp,
            )
            TeslaCard(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isConnected) "CONNECTED" else "WAITING",
                        color = if (isConnected) colors.socGreen else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = if (isConnected) "차량이 준비되었습니다" else "연결을 기다리는 중",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onOpenGauge,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textPrimary,
                    disabledContainerColor = colors.surface,
                    disabledContentColor = colors.textSecondary,
                ),
            ) {
                Text("계기판 열기", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            ) {
                Text("히스토리")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            ) {
                Text("설정")
            }
        }
    }
}

@Composable
fun TeslaSplash(modifier: Modifier = Modifier) {
    val colors = GaugeTheme.colors
    TeslaScreen(modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TeslaHeroImage(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), overlay = true)
            Spacer(Modifier.height(24.dp))
            Text(
                "MyT",
                color = colors.textPrimary,
                fontSize = 18.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.size(8.dp))
            Text("MODEL 3", color = colors.textSecondary, letterSpacing = 4.sp, fontSize = 13.sp)
        }
    }
}
