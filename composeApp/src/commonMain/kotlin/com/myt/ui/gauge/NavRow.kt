package com.myt.ui.gauge

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myt.domain.model.NavInfo
import com.myt.ui.theme.GaugeTheme

@Composable
fun NavRow(
    navigation: NavInfo?,
    modifier: Modifier = Modifier,
) {
    RowText(
        primary = navigation?.destinationName ?: "목적지 없음",
        secondary = navigation?.let { nav ->
            buildString {
                nav.etaMinutes?.let { append("${it}분") }
                nav.distanceKm?.let {
                    if (isNotEmpty()) append(" · ")
                    append("${it.toInt()} km")
                }
            }
        } ?: "",
        modifier = modifier,
    )
}

@Composable
private fun RowText(
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = primary,
            color = GaugeTheme.colors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (secondary.isNotEmpty()) {
            Text(
                text = secondary,
                color = GaugeTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}
