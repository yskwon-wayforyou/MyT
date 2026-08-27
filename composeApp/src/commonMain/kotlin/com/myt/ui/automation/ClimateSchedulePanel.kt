package com.myt.ui.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.automation.ClimateRepeat
import com.myt.domain.automation.ClimateSchedule
import com.myt.domain.automation.ClimateScheduleMatcher
import com.myt.domain.automation.ClimateScheduleRepository
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.accentBlue
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ClimateSchedulePanel(
    repository: ClimateScheduleRepository,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
) {
    val colors = GaugeTheme.colors
    val scope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<ClimateSchedule>>(emptyList()) }
    LaunchedEffect(Unit) {
        schedules = repository.list()
    }
    fun refresh() {
        scope.launch { schedules = repository.list() }
    }
    TeslaCard(modifier = modifier.fillMaxWidth(), accent = colors.accentBlue, flat = flat) {
        Column(modifier.padding(if (flat) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("공조 세밀 예약 (W3)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                "시각·온도·열선·해동·반복 · Plus(W9) 예고 · 지금은 Free 포함",
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
            schedules.forEach { schedule ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(schedule.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            ClimateScheduleMatcher.summarize(schedule),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = schedule.enabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                repository.save(schedule.copy(enabled = enabled))
                                schedules = repository.list()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val id = "clim-${Random.nextInt(1000, 9999)}"
                            repository.save(
                                ClimateSchedule(
                                    id = id,
                                    name = "저녁 프리컨디션",
                                    hour = 18,
                                    minute = 30,
                                    targetTempC = 22f,
                                    driverSeatHeat = 1,
                                    steeringHeat = false,
                                    defrost = false,
                                    repeat = ClimateRepeat.Daily,
                                    enabled = true,
                                ),
                            )
                            schedules = repository.list()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentBlue,
                        contentColor = colors.bg,
                    ),
                ) {
                    Text("저녁 예약 추가", fontSize = 12.sp)
                }
                TextButton(onClick = { refresh() }) {
                    Text("새로고침", color = colors.accentBlue, fontSize = 12.sp)
                }
            }
        }
    }
}
