package com.myt.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myt.ui.theme.GaugeTheme

@Composable
fun VoiceNavDialog(
    destination: String?,
    isListening: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = if (isListening) "듣는 중…" else "목적지 확인",
                color = GaugeTheme.colors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = destination ?: "말씀해 주세요",
                    color = GaugeTheme.colors.accent,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !destination.isNullOrBlank()) {
                Text("전송")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("취소")
            }
        },
    )
}
