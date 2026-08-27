package com.myt.ui.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard
import com.myt.ui.theme.accentPurple
import kotlinx.coroutines.launch

@Composable
fun AutomationRulesPanel(
    repository: AutomationRepository,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<AutomationRule>>(emptyList()) }
    LaunchedEffect(Unit) {
        rules = repository.listRules()
    }
    TeslaCard(modifier = modifier.fillMaxWidth(), accent = colors.accentPurple) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("자동화 (M32)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text("로컬 데모 규칙 5개 · 서버 동기화는 Phase 2", color = colors.textSecondary, fontSize = 12.sp)
            rules.forEach { rule ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(rule.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${rule.trigger} → ${rule.action}", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                val updated = rule.copy(enabled = enabled)
                                repository.saveRule(updated)
                                rules = repository.listRules()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent),
                    )
                }
            }
        }
    }
}
