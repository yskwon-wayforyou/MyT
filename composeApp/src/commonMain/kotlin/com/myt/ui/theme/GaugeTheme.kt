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
)

val LocalGaugeColors = staticCompositionLocalOf { GaugeDarkColors }

val GaugeDarkColors = GaugeColors(
    bg = Color(0xFF0A0A14),
    surface = Color(0xFF1A1A2E),
    speed = Color(0xFF00FF88),
    speedWarn = Color(0xFFFF4444),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8899AA),
    accent = Color(0xFF00B0FF),
    warningL1 = Color(0xFFE65100),
    warningL2 = Color(0xFFBF360C),
    warningL3 = Color(0xFFB71C1C),
    gearP = Color(0xFF888888),
    gearR = Color(0xFFFF9800),
    gearN = Color(0xFF888888),
    gearD = Color(0xFF00E676),
    socGreen = Color(0xFF00C853),
    socYellow = Color(0xFFFFD600),
    socRed = Color(0xFFFF1744),
)

val GaugeLightColors = GaugeColors(
    bg = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    speed = Color(0xFF1B5E20),
    speedWarn = Color(0xFFFF4444),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF757575),
    accent = Color(0xFF0277BD),
    warningL1 = Color(0xFFE65100),
    warningL2 = Color(0xFFBF360C),
    warningL3 = Color(0xFFB71C1C),
    gearP = Color(0xFF888888),
    gearR = Color(0xFFFF9800),
    gearN = Color(0xFF888888),
    gearD = Color(0xFF1B5E20),
    socGreen = Color(0xFF00C853),
    socYellow = Color(0xFFFFD600),
    socRed = Color(0xFFFF1744),
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
            onBackground = gaugeColors.textPrimary,
            onSurface = gaugeColors.textPrimary,
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
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}
