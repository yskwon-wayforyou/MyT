package com.myt.ui.voice

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.myt.ui.theme.GaugeTheme

@Composable
fun VoiceAssistantDialog(
    transcript: String?,
    status: String,
    isListening: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = GaugeTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isListening) "음성 명령" else "음성 결과", color = colors.textPrimary) },
        text = {
            Text(
                text = when {
                    isListening -> "전화 · 문자 · 카카오 · 내비 · 히스토리 · 읽어줘"
                    !transcript.isNullOrBlank() -> transcript
                    else -> status
                },
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = colors.accent)
            }
        },
    )
}
