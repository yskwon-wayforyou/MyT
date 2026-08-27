package com.myt.ui.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.AppStateMachine
import com.myt.ui.ConnectionErrorKind
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaGlassPanel

@Composable
fun ConnectionErrorBanner(
    kind: ConnectionErrorKind,
    onRetry: () -> Unit,
    onDismissHome: (() -> Unit)? = null,
    detail: String? = null,
    modifier: Modifier = Modifier,
) {
    if (kind == ConnectionErrorKind.None) return
    val colors = GaugeTheme.colors
    val (title, message, accent) = when (kind) {
        ConnectionErrorKind.Sleeping -> Triple(
            "차량이 절전 중입니다",
            "테슬라API 호출을 줄이고 캐시 데이터를 표시합니다.",
            colors.socYellow,
        )
        ConnectionErrorKind.BluetoothLost -> Triple(
            "블루투스 연결이 끊어졌습니다",
            "휴대폰과 차량 BT를 확인해 주세요.",
            colors.socRed,
        )
        ConnectionErrorKind.ApiError -> Triple(
            "테슬라API 오류",
            "네트워크 또는 인증 상태를 확인해 주세요.",
            colors.socRed,
        )
        ConnectionErrorKind.QuotaHold -> Triple(
            "테슬라API 한도 보호",
            detail?.takeIf { it.isNotBlank() }
                ?: "앱 일일/월간 호출 상한으로 갱신이 잠시 제한됩니다. 더보기 → API 사용량에서 확인하세요.",
            colors.socYellow,
        )
        ConnectionErrorKind.None -> return
    }

    TeslaGlassPanel(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        accent = accent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(message, color = colors.textSecondary, fontSize = 11.sp)
            }
            Row {
                TextButton(onClick = onRetry) { Text("재시도", color = accent, fontSize = 12.sp) }
                if (kind == ConnectionErrorKind.BluetoothLost && onDismissHome != null) {
                    TextButton(onClick = onDismissHome) { Text("홈", color = colors.textSecondary, fontSize = 12.sp) }
                }
            }
        }
    }
}

fun connectionErrorKind(
    status: com.myt.domain.model.ConnectionStatus,
    bluetoothPresent: Boolean,
): ConnectionErrorKind = AppStateMachine.connectionErrorKind(status, bluetoothPresent)
