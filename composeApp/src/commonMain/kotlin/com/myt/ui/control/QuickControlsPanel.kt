package com.myt.ui.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.control.ControlRequest
import com.myt.domain.control.ControlResult
import com.myt.domain.control.VehicleCommand
import com.myt.domain.control.VehicleControlGateway
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.accentBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickControlsPanel(
    vin: String?,
    gateway: VehicleControlGateway,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    TeslaCard(modifier = modifier.fillMaxWidth(), accent = colors.accentBlue) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("퀵 컨트롤 (M30)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                "주행 중 안전 게이트가 적용됩니다. Fleet 명령 프록시는 Phase 2에서 연결됩니다.",
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleCommand.entries.forEach { cmd ->
                    Button(
                        onClick = {
                            val target = vin
                            if (target.isNullOrBlank()) {
                                message = "VIN이 없습니다"
                                return@Button
                            }
                            scope.launch {
                                when (val result = gateway.execute(ControlRequest(cmd, target))) {
                                    is ControlResult.Accepted -> message = "${cmd.name} 수락"
                                    is ControlResult.Rejected -> message = result.reason
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceHigh,
                            contentColor = colors.textPrimary,
                        ),
                    ) {
                        Text(cmd.name, fontSize = 12.sp)
                    }
                }
            }
            message?.let { Text(it, color = colors.textSecondary, fontSize = 12.sp) }
        }
    }
}
