package com.myt.ui.speedcam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.PoiDataStatus
import com.myt.ui.UiLabels
import com.myt.ui.gauge.ClusterIcons
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.accentBlue
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SpeedCamDataPanel(
    status: PoiDataStatus,
    syncInProgress: Boolean,
    onUpdateClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val statusLabel = when {
        syncInProgress -> "업데이트 중…"
        status.isLatest -> "최신"
        status.autoSyncFailed -> "자동 동기화 실패"
        !status.otaUrlConfigured -> "번들 데이터 (자동)"
        else -> "업데이트 가능"
    }
    val statusColor = when {
        syncInProgress -> colors.accentBlue
        status.isLatest -> Color(0xFF30D158)
        status.autoSyncFailed -> Color(0xFFFF6A00)
        else -> colors.textSecondary
    }
    val lastSyncText = status.lastSyncEpochMs?.let { formatSyncTime(it) } ?: "동기화 기록 없음"
    TeslaCard(modifier = modifier.fillMaxWidth(), accent = Color(0xFF30D158)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    ClusterIcons.speedCam,
                    contentDescription = null,
                    tint = Color(0xFF30D158),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    UiLabels.speedCamDataTitle,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            Text(
                "v${status.bundledVersion} · 설치 ${status.installedCount}건 · $statusLabel",
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "마지막 동기화: $lastSyncText",
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            status.lastSyncDetail?.takeIf { !status.autoSyncFailed }?.let { detail ->
                Text(
                    detail,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            status.updateReason?.let { reason ->
                Text(
                    reason,
                    color = if (status.autoSyncFailed) Color(0xFFFF6A00) else colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (status.otaUrlConfigured) {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = !syncInProgress, onClick = onUpdateClick)
                            .background(Color(0x2230D158), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF30D158).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (syncInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF30D158),
                            )
                        }
                        Text(
                            if (syncInProgress) "동기화 중…" else UiLabels.speedCamUpdateNow,
                            color = Color(0xFF30D158),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(
                        "앱 시작 시 번들·자동 동기화 적용",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    UiLabels.speedCamOpenSettings,
                    color = colors.accentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onOpenSettings)
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

private fun formatSyncTime(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}.${dt.monthNumber.toString().padStart(2, '0')}.${dt.dayOfMonth.toString().padStart(2, '0')} " +
        "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}
