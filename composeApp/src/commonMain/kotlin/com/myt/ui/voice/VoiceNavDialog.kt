package com.myt.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.ui.theme.GaugeTheme

@Composable
fun VoiceNavDialog(
    destination: String?,
    isListening: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF121820),
        title = {
            Text(
                text = if (isListening) "내비 목적지" else "목적지 확인",
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ListeningOrb(active = isListening)
                if (isListening) {
                    ListeningWaveform(active = true)
                    Text(
                        "목적지를 말씀해 주세요",
                        color = Color(0xFF64D2FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "예) 「광교중앙역」 · 「집」 · 「근처 슈퍼차저」",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                    )
                } else {
                    Text(
                        text = destination ?: "인식된 목적지가 없습니다",
                        color = colors.accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        "차량 내비로 전송할까요?",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !destination.isNullOrBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D9EFF)),
            ) {
                Text("차량으로 전송")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("취소", color = colors.textSecondary)
            }
        },
    )
}
