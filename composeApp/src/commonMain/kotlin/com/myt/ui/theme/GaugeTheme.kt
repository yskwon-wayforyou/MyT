package com.myt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GaugeColors(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val speed: Color,
    val speedWarn: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val warningL1: Color,
    val warningL2: Color,
    val warningL3: Color,
    val gearP: Color,
    val gearR: Color,
    val gearN: Color,
    val gearD: Color,
    val socGreen: Color,
    val socYellow: Color,
    val socRed: Color,
    val stroke: Color,
)

val LocalGaugeColors = staticCompositionLocalOf { GaugeDarkColors }

val GaugeDarkColors = GaugeColors(
    bg = Color(0xFF030305),
    surface = Color(0xFF16161C),
    surfaceHigh = Color(0xFF22222A),
    speed = Color(0xFFF8F8FA),
    speedWarn = Color(0xFFE82127),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF9A9AA3),
    accent = Color(0xFFE82127),
    warningL1 = Color(0xFFFFB020),
    warningL2 = Color(0xFFFF6A00),
    warningL3 = Color(0xFFE82127),
    gearP = Color(0xFF8E8E93),
    gearR = Color(0xFFFF9F0A),
    gearN = Color(0xFF64D2FF),
    gearD = Color(0xFF30D158),
    socGreen = Color(0xFF30D158),
    socYellow = Color(0xFFFFD60A),
    socRed = Color(0xFFE82127),
    stroke = Color(0x33FFFFFF),
)

val GaugeLightColors = GaugeColors(
    bg = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF2F2F7),
    speed = Color(0xFF1C1C1E),
    speedWarn = Color(0xFFE82127),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF6E6E73),
    accent = Color(0xFFE82127),
    warningL1 = Color(0xFFE65100),
    warningL2 = Color(0xFFBF360C),
    warningL3 = Color(0xFFB71C1C),
    gearP = Color(0xFF8E8E93),
    gearR = Color(0xFFFF9F0A),
    gearN = Color(0xFF8E8E93),
    gearD = Color(0xFF1C1C1E),
    socGreen = Color(0xFF248A3D),
    socYellow = Color(0xFFC9A227),
    socRed = Color(0xFFE82127),
    stroke = Color(0x14000000),
)

object GaugeTheme {
    val colors: GaugeColors
        @Composable get() = LocalGaugeColors.current
}

@Composable
fun MyTTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val gaugeColors = if (darkTheme) GaugeDarkColors else GaugeLightColors
    val materialColors = if (darkTheme) {
        darkColorScheme(
            background = gaugeColors.bg,
            surface = gaugeColors.surface,
            primary = gaugeColors.accent,
            onPrimary = Color.White,
            onBackground = gaugeColors.textPrimary,
            onSurface = gaugeColors.textPrimary,
            secondary = gaugeColors.surfaceHigh,
        )
    } else {
        lightColorScheme(
            background = gaugeColors.bg,
            surface = gaugeColors.surface,
            primary = gaugeColors.accent,
            onBackground = gaugeColors.textPrimary,
            onSurface = gaugeColors.textPrimary,
        )
    }

    CompositionLocalProvider(LocalGaugeColors provides gaugeColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = mytTypography(),
            content = content,
        )
    }
}
