package com.myt.ui.gauge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.UnitConverter
import com.myt.domain.model.Gear
import com.myt.ui.theme.GaugeTheme

@Composable
fun SpeedDisplay(
    speedKmh: Float,
    gear: Gear,
    useKmh: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = UnitConverter.formatSpeed(speedKmh, useKmh),
            color = GaugeTheme.colors.speed,
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 120.sp,
        )
        Text(
            text = UnitConverter.speedUnitLabel(useKmh),
            color = GaugeTheme.colors.textSecondary,
            fontSize = 24.sp,
        )
        GearPill(gear = gear, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun GearPill(
    gear: Gear,
    modifier: Modifier = Modifier,
) {
    val color = when (gear) {
        Gear.PARK -> GaugeTheme.colors.gearP
        Gear.REVERSE -> GaugeTheme.colors.gearR
        Gear.NEUTRAL -> GaugeTheme.colors.gearN
        Gear.DRIVE -> GaugeTheme.colors.gearD
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GaugeTheme.colors.surface)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = gear.displayLabel(),
            color = color,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
