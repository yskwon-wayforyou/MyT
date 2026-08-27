package com.myt.ui.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.myt.ui.UiLabels
import com.myt.ui.gauge.ClusterIcons

/** Floats over gauge content — does not consume layout space. */
@Composable
fun SimulationTestBannerOverlay(
    isSimulated: Boolean,
    scenarioLabel: String?,
    compactStatusBar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!isSimulated || scenarioLabel.isNullOrBlank()) return
    val topInset = if (compactStatusBar) 40.dp else 48.dp
    SimulationTestBanner(
        scenarioLabel = scenarioLabel,
        overlay = true,
        modifier = modifier
            .zIndex(20f)
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = topInset),
    )
}

@Composable
fun SimulationTestBanner(
    scenarioLabel: String?,
    overlay: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (scenarioLabel.isNullOrBlank()) return
    Column(
        modifier = modifier
            .then(if (!overlay) Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp) else Modifier)
            .background(
                if (overlay) Color(0xCC1A0808) else Color(0x44E82127),
                RoundedCornerShape(10.dp),
            )
            .border(1.dp, Color(0xFFE82127).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(
                horizontal = 12.dp,
                vertical = if (overlay) 7.dp else 10.dp,
            )
            .semantics {
                contentDescription = "${UiLabels.simulationTesting} $scenarioLabel"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = ClusterIcons.simulation,
                contentDescription = null,
                tint = Color(0xFFFF6A6A),
                modifier = Modifier.size(18.dp),
            )
            Text(
                UiLabels.simulationTesting,
                color = Color(0xFFFFECEC),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Text(
            scenarioLabel,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
