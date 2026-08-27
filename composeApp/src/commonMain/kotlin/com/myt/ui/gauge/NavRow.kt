package com.myt.ui.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.NavInfo
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard

@Composable
fun NavRow(
    navigation: NavInfo?,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    TeslaCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "NAVIGATION",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.6.sp,
            )
            Text(
                text = navigation?.destinationName ?: "목적지 없음",
                color = colors.textPrimary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val secondary = navigation?.let { nav ->
                buildString {
                    nav.etaMinutes?.let { append("${it}분") }
                    nav.distanceKm?.let {
                        if (isNotEmpty()) append("  ·  ")
                        append("${it.toInt()} km")
                    }
                }
            }.orEmpty()
            if (secondary.isNotEmpty()) {
                Text(secondary, color = colors.textSecondary, fontSize = 13.sp)
            }
        }
    }
}
