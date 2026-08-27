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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.PoiDataStatus
import com.myt.ui.UiLabels
import com.myt.ui.gauge.ClusterIcons

@Composable
fun SpeedCamDataUpdateBanner(
    status: PoiDataStatus,
    onUpdateClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!status.manualUpdateRequired) return
    val reason = status.updateReason ?: UiLabels.speedCamUpdateDefault
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x332FFB74), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF30D158).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                ClusterIcons.warning,
                contentDescription = null,
                tint = Color(0xFF30D158),
                modifier = Modifier.size(16.dp),
            )
            Text(
                UiLabels.speedCamDataTitle,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "v${status.bundledVersion} · 설치 ${status.installedCount}건 · $reason",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp,
            textAlign = TextAlign.Start,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (status.otaUrlConfigured) {
                Text(
                    UiLabels.speedCamUpdateNow,
                    color = Color(0xFF30D158),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onUpdateClick)
                        .padding(vertical = 2.dp),
                )
            }
            Text(
                UiLabels.speedCamOpenSettings,
                color = Color(0xFF64D2FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onOpenSettings)
                    .padding(vertical = 2.dp),
            )
        }
    }
}
