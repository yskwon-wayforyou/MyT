package com.myt.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myt.ui.theme.GaugeTheme

@Composable
fun SettingsScreen(
    speedUnitKmh: Boolean,
    onSpeedUnitChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var useKmh by remember(speedUnitKmh) { mutableStateOf(speedUnitKmh) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        Text("설정", color = GaugeTheme.colors.textPrimary)
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text("속도 단위: km/h", color = GaugeTheme.colors.textSecondary)
            Switch(
                checked = useKmh,
                onCheckedChange = {
                    useKmh = it
                    onSpeedUnitChange(it)
                },
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로")
        }
    }
}
