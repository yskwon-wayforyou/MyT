package com.myt.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.debug.LogEntry
import com.myt.debug.LogLevel
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.TeslaScreen

@Composable
fun DebugLogScreen(
    viewModel: DebugLogViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries by viewModel.entries.collectAsState()
    val enabled by viewModel.enabled.collectAsState()
    val status by viewModel.status.collectAsState()
    val colors = GaugeTheme.colors

    TeslaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("디버그 로그", color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Light)
                OutlinedButton(onClick = onBack) { Text("닫기", color = colors.textPrimary) }
            }
            TeslaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("로그 수집", color = colors.textPrimary, fontSize = 15.sp)
                        Text("${entries.size}건", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = viewModel::setEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.textPrimary,
                            checkedTrackColor = colors.accent,
                        ),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = viewModel::exportViaGmail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.bg),
                ) {
                    Text("Gmail로 보내기", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = viewModel::clearLogs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("비우기", color = colors.textPrimary, fontSize = 13.sp)
                }
            }
            status?.let {
                Text(it, color = colors.socGreen, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(entries.asReversed(), key = { it.id }) { entry ->
                    LogLine(entry)
                }
            }
        }
    }
}

@Composable
private fun LogLine(entry: LogEntry) {
    val colors = GaugeTheme.colors
    val color = when (entry.level) {
        LogLevel.Debug -> colors.textSecondary
        LogLevel.Info -> colors.textPrimary
        LogLevel.Warn -> colors.socYellow
        LogLevel.Error -> colors.accent
    }
    Text(
        text = entry.formattedLine(),
        color = color,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth(),
    )
}
