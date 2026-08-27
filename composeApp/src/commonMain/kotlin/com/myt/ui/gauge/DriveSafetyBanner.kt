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
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaGlassPanel
import com.myt.ui.theme.accentBlue

/** W2 — Play/운전 중 주의 고지 (Q-DRV-01). */
@Composable
fun DriveSafetyBanner(
    visible: Boolean,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val colors = GaugeTheme.colors
    TeslaGlassPanel(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        accent = colors.accentBlue,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "운전 중에는 조작을 최소화해 주세요",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "과속·단속 안내는 보조 정보입니다. 음성 명령을 우선하고, 전방 주시 의무는 운전자에게 있습니다.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = onAcknowledge) {
                Text("확인", color = colors.accentBlue)
            }
        }
    }
}
