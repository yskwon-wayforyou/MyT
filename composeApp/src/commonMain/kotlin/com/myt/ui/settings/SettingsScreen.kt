package com.myt.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.config.HaIntegrationConfig
import com.myt.config.TeslaConfig
import com.myt.domain.model.DriveDensity
import com.myt.domain.model.GaugeDisplayPrefs
import com.myt.domain.model.GaugeField
import com.myt.domain.model.GaugeLayoutMode
import com.myt.domain.model.PressureUnit
import com.myt.domain.model.labelKo
import com.myt.domain.usecase.AuthUseCase
import com.myt.domain.usecase.VoiceCommandUseCase
import com.myt.platform.BatteryOptimizationPlatform
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaScreen
import com.myt.ui.theme.accentBlue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    teslaConfig: TeslaConfig,
    speedUnitKmh: Boolean,
    darkTheme: Boolean,
    gaugePrefs: GaugeDisplayPrefs,
    onSpeedUnitChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onGaugePrefsChange: (GaugeDisplayPrefs) -> Unit,
    onSaveTeslaConfig: (TeslaConfig) -> Unit,
    haConfig: HaIntegrationConfig = HaIntegrationConfig(),
    onSaveHaConfig: (HaIntegrationConfig) -> Unit = {},
    onOpenDebugLogs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authUseCase = koinInject<AuthUseCase>()
    val voiceCommandUseCase = koinInject<VoiceCommandUseCase>()
    val batteryOptimization = koinInject<BatteryOptimizationPlatform>()
    val localNotify = koinInject<com.myt.platform.LocalNotificationPlatform>()
    val pushNotifier = koinInject<com.myt.phase2.PushNotifier>()
    val scope = rememberCoroutineScope()
    var authTestMsg by remember { mutableStateOf<String?>(null) }
    var voiceTestMsg by remember { mutableStateOf<String?>(null) }
    var notifyHint by remember { mutableStateOf<String?>(null) }
    var fleetVehicles by remember { mutableStateOf<List<com.myt.data.fleet.TeslaVehicleSummary>>(emptyList()) }
    var fleetVehiclesMsg by remember { mutableStateOf<String?>(null) }
    var fleetVehiclesLoading by remember { mutableStateOf(false) }
    var appId by remember { mutableStateOf(teslaConfig.appId) }
    var clientId by remember { mutableStateOf(teslaConfig.clientId) }
    var clientSecret by remember { mutableStateOf(teslaConfig.clientSecret) }
    var redirectUri by remember { mutableStateOf(teslaConfig.redirectUri) }
    var partnerDomain by remember { mutableStateOf(teslaConfig.partnerDomain) }
    var fleetApiBase by remember { mutableStateOf(teslaConfig.fleetApiBase) }
    var authBaseUrl by remember { mutableStateOf(teslaConfig.authBaseUrl) }
    var scopes by remember { mutableStateOf(teslaConfig.scopes) }
    var vehicleVin by remember { mutableStateOf(teslaConfig.vehicleVin) }
    var poiOtaCsvUrl by remember { mutableStateOf(teslaConfig.poiOtaCsvUrl) }
    var telemetryWssUrl by remember { mutableStateOf(teslaConfig.telemetryWssUrl) }
    var haEnabled by remember { mutableStateOf(haConfig.enabled) }
    var haBaseUrl by remember { mutableStateOf(haConfig.baseUrl) }
    var haToken by remember { mutableStateOf(haConfig.accessToken) }
    var haTopicPrefix by remember { mutableStateOf(haConfig.topicPrefix) }
    var haVinSuffix by remember { mutableStateOf(haConfig.vinSuffix) }
    var showSecret by remember { mutableStateOf(false) }
    var savedHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(teslaConfig) {
        appId = teslaConfig.appId
        clientId = teslaConfig.clientId
        clientSecret = teslaConfig.clientSecret
        redirectUri = teslaConfig.redirectUri
        partnerDomain = teslaConfig.partnerDomain
        fleetApiBase = teslaConfig.fleetApiBase
        authBaseUrl = teslaConfig.authBaseUrl
        scopes = teslaConfig.scopes
        vehicleVin = teslaConfig.vehicleVin
        poiOtaCsvUrl = teslaConfig.poiOtaCsvUrl
        telemetryWssUrl = teslaConfig.telemetryWssUrl
    }
    LaunchedEffect(haConfig) {
        haEnabled = haConfig.enabled
        haBaseUrl = haConfig.baseUrl
        haToken = haConfig.accessToken
        haTopicPrefix = haConfig.topicPrefix
        haVinSuffix = haConfig.vinSuffix
    }

    val colors = GaugeTheme.colors
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.stroke,
        focusedLabelColor = colors.textSecondary,
        unfocusedLabelColor = colors.textSecondary,
        cursorColor = colors.accent,
    )

    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text("설정", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionTitle("계기판 레이아웃")
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("화면 배치", color = colors.textSecondary, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GaugeLayoutMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = gaugePrefs.layoutMode == mode,
                                    onClick = { onGaugePrefsChange(gaugePrefs.copy(layoutMode = mode)) },
                                    label = { Text(mode.labelKo()) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent,
                                        selectedLabelColor = colors.bg,
                                        containerColor = colors.surfaceHigh,
                                        labelColor = colors.textPrimary,
                                    ),
                                )
                            }
                        }
                        Text("Drive 밀도", color = colors.textSecondary, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DriveDensity.entries.forEach { density ->
                                FilterChip(
                                    selected = gaugePrefs.driveDensity == density,
                                    onClick = { onGaugePrefsChange(gaugePrefs.copy(driveDensity = density)) },
                                    label = {
                                        Text(
                                            when (density) {
                                                DriveDensity.Minimal -> "최소"
                                                DriveDensity.Standard -> "표준"
                                                DriveDensity.Pro -> "프로"
                                            },
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent,
                                        selectedLabelColor = colors.bg,
                                        containerColor = colors.surfaceHigh,
                                        labelColor = colors.textPrimary,
                                    ),
                                )
                            }
                        }
                        Text("정보 그리드 열", color = colors.textSecondary, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "자동", 2 to "2열", 3 to "3열", 4 to "4열").forEach { (value, label) ->
                                FilterChip(
                                    selected = gaugePrefs.gridColumns == value,
                                    onClick = { onGaugePrefsChange(gaugePrefs.copy(gridColumns = value)) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent,
                                        selectedLabelColor = colors.bg,
                                        containerColor = colors.surfaceHigh,
                                        labelColor = colors.textPrimary,
                                    ),
                                )
                            }
                        }
                    }
                }
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        PreferenceSwitch(
                            title = "단말 GPS 속도 우선",
                            checked = gaugePrefs.preferDeviceSpeed,
                            onCheckedChange = { on ->
                                onGaugePrefsChange(gaugePrefs.copy(preferDeviceSpeed = on))
                            },
                        )
                        Text(
                            "BT 연결 시에만 기기 GPS로 속도·단속 (미연결 시 사용 안 함)",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                        )
                    }
                }
                SectionTitle("알림 · 절전")
                TeslaCard(modifier = Modifier.fillMaxWidth(), accent = colors.accentBlue) {
                    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "제어·충전·자동화 알림 채널",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "로컬 시스템 알림을 사용합니다. FCM 원격 푸시는 Firebase 연동 후 같은 채널로 이어집니다. " +
                                "제조사 절전·Doze에 막히지 않으려면 알림 허용과 배터리 예외를 함께 켜 주세요.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                        Button(
                            onClick = {
                                localNotify.ensureChannels()
                                val opened = localNotify.openAppNotificationSettings()
                                notifyHint = if (opened) {
                                    "앱 알림 설정을 열었습니다. MyT 알림을 허용해 주세요."
                                } else {
                                    "알림 설정을 열 수 없습니다. 시스템 앱 정보 → 알림에서 허용해 주세요."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentBlue,
                                contentColor = colors.bg,
                            ),
                        ) {
                            Text("알림 설정 열기", fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                val opened = batteryOptimization.openBatteryOptimizationSettings()
                                notifyHint = if (opened) {
                                    "배터리 설정을 열었습니다. MyT를 최적화 예외로 허용해 주세요."
                                } else {
                                    "이 기기에서는 설정 화면을 열 수 없습니다. 앱 정보 → 배터리에서 직접 예외를 허용해 주세요."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceHigh,
                                contentColor = colors.textPrimary,
                            ),
                        ) {
                            Text("배터리 예외 설정 열기", fontSize = 13.sp)
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pushNotifier.notify("MyT 알림 테스트", "제어·충전·자동화 채널이 정상입니다.")
                                    notifyHint = "테스트 알림을 보냈습니다. 알림창을 확인해 주세요."
                                }
                            },
                        ) {
                            Text("테스트 알림 보내기", color = colors.accentBlue, fontSize = 13.sp)
                        }
                        notifyHint?.let {
                            Text(it, color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
                SectionTitle("차량 선택 (다중 VIN)")
                TeslaCard(modifier = Modifier.fillMaxWidth(), accent = colors.accent) {
                    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Fleet에 등록된 차량 중 활성 VIN을 고릅니다. 전환 후 계기판·제어가 해당 차량을 봅니다.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                        Text(
                            "현재 …${vehicleVin.takeLast(6).ifBlank { "미설정" }}",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = {
                                fleetVehiclesLoading = true
                                fleetVehiclesMsg = null
                                scope.launch {
                                    authUseCase.listFleetVehicles().fold(
                                        onSuccess = { list ->
                                            fleetVehicles = list
                                            fleetVehiclesMsg = if (list.isEmpty()) {
                                                "등록된 차량이 없습니다. Tesla 계정·권한을 확인해 주세요."
                                            } else {
                                                "${list.size}대 불러옴"
                                            }
                                        },
                                        onFailure = {
                                            fleetVehicles = emptyList()
                                            fleetVehiclesMsg = "목록 실패: ${it.message}"
                                        },
                                    )
                                    fleetVehiclesLoading = false
                                }
                            },
                            enabled = !fleetVehiclesLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.bg,
                            ),
                        ) {
                            Text(
                                if (fleetVehiclesLoading) "불러오는 중…" else "Fleet 차량 목록",
                                fontSize = 13.sp,
                            )
                        }
                        fleetVehiclesMsg?.let {
                            Text(it, color = colors.textSecondary, fontSize = 12.sp)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            fleetVehicles.forEach { v ->
                                val vin = v.vin?.trim().orEmpty()
                                if (vin.isBlank()) return@forEach
                                val selected = vin.equals(vehicleVin, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        vehicleVin = vin.uppercase()
                                        scope.launch {
                                            authUseCase.selectActiveVehicle(vin)
                                            onSaveTeslaConfig(
                                                teslaConfig.copy(vehicleVin = vin.uppercase()),
                                            )
                                            fleetVehiclesMsg = "활성 VIN …${vin.takeLast(6)}"
                                        }
                                    },
                                    label = {
                                        Text(
                                            buildString {
                                                append(v.displayName?.takeIf { it.isNotBlank() } ?: "Vehicle")
                                                append(" · …")
                                                append(vin.takeLast(6))
                                                v.state?.let { append(" ($it)") }
                                            },
                                            fontSize = 12.sp,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent.copy(alpha = 0.35f),
                                    ),
                                )
                            }
                        }
                    }
                }
                SectionTitle("계기판 표시 항목")
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        GaugeField.entries.forEach { field ->
                            PreferenceSwitch(
                                title = field.labelKo(),
                                checked = gaugePrefs.shows(field),
                                onCheckedChange = { on ->
                                    val next = if (on) {
                                        gaugePrefs.visibleFields + field
                                    } else {
                                        gaugePrefs.visibleFields - field
                                    }
                                    onGaugePrefsChange(gaugePrefs.copy(visibleFields = next))
                                },
                            )
                        }
                    }
                }
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("속도 단위", color = colors.textPrimary, fontSize = 16.sp)
                            Text(if (speedUnitKmh) "km/h" else "mph", color = colors.textSecondary, fontSize = 13.sp)
                        }
                        Switch(
                            checked = speedUnitKmh,
                            onCheckedChange = onSpeedUnitChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.textPrimary,
                                checkedTrackColor = colors.accent,
                            ),
                        )
                    }
                }
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("타이어 공기압 단위", color = colors.textPrimary, fontSize = 16.sp)
                            Text(
                                if (gaugePrefs.usePsi()) "psi (기본)" else "bar",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                            )
                        }
                        Switch(
                            checked = gaugePrefs.usePsi(),
                            onCheckedChange = { on ->
                                onGaugePrefsChange(
                                    gaugePrefs.copy(
                                        pressureUnit = if (on) PressureUnit.Psi else PressureUnit.Bar,
                                    ),
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.textPrimary,
                                checkedTrackColor = colors.accent,
                            ),
                        )
                    }
                }
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("다크 테마", color = colors.textPrimary, fontSize = 16.sp)
                            Text(if (darkTheme) "어두운 계기판" else "밝은 계기판", color = colors.textSecondary, fontSize = 13.sp)
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = onDarkThemeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.textPrimary,
                                checkedTrackColor = colors.accent,
                            ),
                        )
                    }
                }
                SectionTitle("Auth 테스트 (W1)")
                Text(
                    "토큰 갱신과 Virtual Key 공개키 URL을 확인합니다. 서명 명령은 VK 페어링 후 동작합니다.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    authTestMsg = authUseCase.runAuthSelfTest().summary()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceHigh,
                                contentColor = colors.textPrimary,
                            ),
                        ) {
                            Text("토큰·VK 안내 실행", color = colors.accentBlue)
                        }
                        authTestMsg?.let {
                            Text(it, color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
                SectionTitle("음성 예시 TTS 테스트")
                Text(
                    "예시 문구를 TTS로 재생한 뒤, 같은 텍스트를 STT 대신 주입해 명령 연동을 확인합니다. (마이크 미사용)",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    voiceTestMsg = "실행 중…"
                                    val results = voiceCommandUseCase.runAllExamplesAsTtsInject(speakFirst = true)
                                    voiceTestMsg = results.joinToString("\n") { (ex, r) ->
                                        val ok = when (r) {
                                            is com.myt.domain.usecase.VoiceCommandResult.Failed -> "FAIL ${r.message}"
                                            else -> "OK ${r::class.simpleName}"
                                        }
                                        "${ex.id}: $ok"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceHigh,
                                contentColor = colors.textPrimary,
                            ),
                        ) {
                            Text("TTS→명령 전체 예시 실행", color = colors.accentBlue)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val example = com.myt.domain.voice.VoiceCommandExamples.byId("ytm_lee_seunghwan")
                                        ?: return@launch
                                    voiceTestMsg = "YTM 예시 실행 중…"
                                    val r = voiceCommandUseCase.playExampleAndExecute(example, speakFirst = true)
                                    voiceTestMsg = when (r) {
                                        is com.myt.domain.usecase.VoiceCommandResult.Failed ->
                                            "YTM FAIL: ${r.message}"
                                        else -> "YTM OK: ${r::class.simpleName} · ${example.spokenText}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceHigh,
                                contentColor = colors.textPrimary,
                            ),
                        ) {
                            Text("유튜브 뮤직 예시만", color = colors.accentBlue)
                        }
                        voiceTestMsg?.let {
                            Text(it, color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
                SectionTitle("Tesla properties")
                Text(
                    "tesla.local.properties 값을 기기에서 확인하고 고칩니다. 저장하면 바로 반영됩니다.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConfigField("tesla.app.id", appId, { appId = it }, fieldColors)
                        ConfigField("tesla.client.id", clientId, { clientId = it }, fieldColors)
                        OutlinedTextField(
                            value = clientSecret,
                            onValueChange = { clientSecret = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("tesla.client.secret") },
                            singleLine = true,
                            visualTransformation = if (showSecret) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors,
                            trailingIcon = {
                                TextButton(onClick = { showSecret = !showSecret }) {
                                    Text(if (showSecret) "숨김" else "표시", color = colors.accent, fontSize = 12.sp)
                                }
                            },
                        )
                        ConfigField("tesla.oauth.redirect.uri", redirectUri, { redirectUri = it }, fieldColors)
                        ConfigField("tesla.partner.domain", partnerDomain, { partnerDomain = it }, fieldColors)
                        ConfigField("tesla.fleet.api.base", fleetApiBase, { fleetApiBase = it }, fieldColors)
                        ConfigField("tesla.auth.url", authBaseUrl, { authBaseUrl = it }, fieldColors)
                        ConfigField("tesla.oauth.scopes", scopes, { scopes = it }, fieldColors, singleLine = false)
                        ConfigField("tesla.vehicle.vin", vehicleVin, { vehicleVin = it.uppercase() }, fieldColors)
                        ConfigField("tesla.poi.ota.csv.url", poiOtaCsvUrl, { poiOtaCsvUrl = it }, fieldColors)
                        ConfigField("tesla.telemetry.wss.url", telemetryWssUrl, { telemetryWssUrl = it }, fieldColors)
                        savedHint?.let {
                            Text(it, color = colors.socGreen, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                onSaveTeslaConfig(
                                    teslaConfig.copy(
                                        appId = appId.trim(),
                                        clientId = clientId.trim(),
                                        clientSecret = clientSecret.trim(),
                                        redirectUri = redirectUri.trim(),
                                        partnerDomain = partnerDomain.trim(),
                                        fleetApiBase = fleetApiBase.trim(),
                                        authBaseUrl = authBaseUrl.trim(),
                                        scopes = scopes.trim().ifBlank { TeslaConfig.OAuthScopes },
                                        vehicleVin = vehicleVin.trim(),
                                        poiOtaCsvUrl = poiOtaCsvUrl.trim(),
                                        telemetryWssUrl = telemetryWssUrl.trim(),
                                    ),
                                )
                                savedHint = "저장했습니다"
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.bg,
                            ),
                        ) {
                            Text("properties 저장", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SectionTitle("Home Assistant (M39)")
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("REST 상태 게시", color = colors.textPrimary)
                            Switch(
                                checked = haEnabled,
                                onCheckedChange = { haEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent),
                            )
                        }
                        ConfigField("HA base URL", haBaseUrl, { haBaseUrl = it }, fieldColors)
                        ConfigField("Long-lived token", haToken, { haToken = it }, fieldColors)
                        ConfigField("MQTT topic prefix", haTopicPrefix, { haTopicPrefix = it }, fieldColors)
                        ConfigField("VIN suffix (entity id)", haVinSuffix, { haVinSuffix = it }, fieldColors)
                        Text(
                            "활성화 시 SOC·속도·주행거리를 Home Assistant sensor로 주기 게시합니다. MQTT discovery JSON은 브로커 수동 등록용입니다.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                        Button(
                            onClick = {
                                onSaveHaConfig(
                                    HaIntegrationConfig(
                                        enabled = haEnabled,
                                        baseUrl = haBaseUrl.trim(),
                                        accessToken = haToken.trim(),
                                        topicPrefix = haTopicPrefix.trim().ifBlank { "myt" },
                                        vinSuffix = haVinSuffix.trim(),
                                    ),
                                )
                                savedHint = "HA 설정을 저장했습니다"
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceHigh,
                                contentColor = colors.textPrimary,
                            ),
                        ) {
                            Text("HA 설정 저장", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SectionTitle("디버그")
                TeslaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "문제 발생 시 로그를 수집하고 Gmail로 보낼 수 있습니다. 토큰·비밀번호는 자동으로 가려집니다.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                        Button(
                            onClick = onOpenDebugLogs,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceHigh,
                                contentColor = colors.textPrimary,
                            ),
                        ) {
                            Text("디버그 로그 보기 / Gmail 전송")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceHigh,
                    contentColor = colors.textPrimary,
                ),
            ) {
                Text("뒤로", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = GaugeTheme.colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
    )
}

@Composable
private fun PreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = GaugeTheme.colors
    val authUseCase = koinInject<AuthUseCase>()
    val scope = rememberCoroutineScope()
    var authTestMsg by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = colors.textPrimary, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.textPrimary,
                checkedTrackColor = colors.accent,
            ),
        )
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        colors = colors,
    )
}
