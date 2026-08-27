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
import com.myt.domain.automation.AutomationRepository
import com.myt.domain.automation.AutomationRule
import com.myt.domain.automation.AutomationTriggerKind
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.SectionHairline
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.accentPurple
import kotlinx.coroutines.launch

@Composable
fun AutomationRulesPanel(
    repository: AutomationRepository,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
) {
    val colors = GaugeTheme.colors
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<AutomationRule>>(emptyList()) }
    LaunchedEffect(Unit) {
        rules = repository.listRules()
    }
    TeslaCard(modifier = modifier.fillMaxWidth(), accent = colors.accentPurple, flat = flat) {
        Column(
            Modifier.padding(if (flat) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("자동화 (W3)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                "이벤트 · 스케줄 · 지오펜스 로컬 규칙. 켜고 끄거나 삭제할 수 있습니다.",
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
            rules.forEachIndexed { index, rule ->
                if (index > 0) SectionHairline()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(rule.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(ruleSubtitle(rule), color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                repository.saveRule(rule.copy(enabled = enabled))
                                rules = repository.listRules()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent),
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteRule(rule.id)
                            rules = repository.listRules()
                        }
                    },
                ) {
                    Text("삭제", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        val id = "auto-${clockSuffix()}"
                        repository.saveRule(
                            AutomationRule(
                                id = id,
                                name = "새 스케줄 규칙",
                                trigger = "schedule",
                                action = "push",
                                kind = AutomationTriggerKind.Schedule,
                                scheduleHour = 8,
                                scheduleMinute = 0,
                                enabled = true,
                            ),
                        )
                        rules = repository.listRules()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceHigh,
                    contentColor = colors.textPrimary,
                ),
            ) {
                Text("스케줄 규칙 추가", fontSize = 12.sp)
            }
        }
    }
}

private fun ruleSubtitle(rule: AutomationRule): String = when (rule.kind) {
    AutomationTriggerKind.Schedule -> {
        val h = rule.scheduleHour ?: 0
        val m = rule.scheduleMinute ?: 0
        "스케줄 ${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} → ${rule.action}"
    }
    AutomationTriggerKind.GeofenceEnter ->
        "지오펜스 진입 ${rule.geofenceRadiusM ?: 0}m → ${rule.action}"
    AutomationTriggerKind.GeofenceExit ->
        "지오펜스 이탈 ${rule.geofenceRadiusM ?: 0}m → ${rule.action}"
    AutomationTriggerKind.Event -> "${rule.trigger} → ${rule.action}"
}

private fun clockSuffix(): String =
    (kotlinx.datetime.Clock.System.now().toEpochMilliseconds() % 100_000).toString()
