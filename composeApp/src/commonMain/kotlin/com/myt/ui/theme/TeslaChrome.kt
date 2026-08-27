package com.myt.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import myt.composeapp.generated.resources.Res
import myt.composeapp.generated.resources.model3_hero
import org.jetbrains.compose.resources.painterResource

val TeslaCardShape = RoundedCornerShape(22.dp)
val GlassPanelShape = RoundedCornerShape(20.dp)

@Composable
fun TeslaScreen(
    modifier: Modifier = Modifier,
    showCar: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GaugeTheme.colors.bg),
    ) {
        if (showCar) {
            TeslaBackdrop()
        }
        content()
    }
}

@Composable
fun TeslaBackdrop(modifier: Modifier = Modifier) {
    val colors = GaugeTheme.colors
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.model3_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.28f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xAA000000),
                        0.28f to Color(0xCC050508),
                        0.55f to Color(0xEE08080C),
                        1f to Color(0xFF000000),
                    ),
                ),
        )
        // Cockpit grid overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = 32.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.025f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += grid
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.025f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += grid
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to colors.panelScrim,
                    ),
                ),
        )
    }
}

@Composable
fun TeslaHeroImage(
    modifier: Modifier = Modifier,
    overlay: Boolean = true,
) {
    Box(modifier = modifier.clip(TeslaCardShape)) {
        Image(
            painter = painterResource(Res.drawable.model3_hero),
            contentDescription = "Tesla Model 3",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (overlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color(0x66000000),
                            1f to Color(0xCC000000),
                        ),
                    ),
            )
        }
    }
}

@Composable
fun TeslaGlassPanel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = GaugeTheme.colors
    Box(
        modifier = modifier
            .shadow(8.dp, GlassPanelShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(GlassPanelShape)
            .background(colors.panelGlass)
            .border(1.dp, colors.panelGlassBorder, GlassPanelShape),
    ) {
        if (accent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.2f), accent, accent.copy(alpha = 0.2f)),
                        ),
                    ),
            )
        }
        Box(modifier = Modifier.padding(top = if (accent != null) 3.dp else 0.dp)) {
            content()
        }
    }
}

@Composable
fun TeslaCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = GaugeTheme.colors
    Box(
        modifier = modifier
            .shadow(4.dp, TeslaCardShape, ambientColor = Color.Black.copy(0.4f))
            .clip(TeslaCardShape)
            .background(
                Brush.verticalGradient(
                    listOf(colors.surfaceHigh.copy(alpha = 0.95f), colors.surface),
                ),
            )
            .border(1.dp, colors.panelGlassBorder.copy(alpha = 0.55f), TeslaCardShape),
    ) {
        if (accent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accent.copy(alpha = 0.85f)),
            )
        }
        Box(modifier = Modifier.padding(top = if (accent != null) 2.dp else 0.dp)) {
            content()
        }
    }
}

@Composable
fun TeslaHairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GaugeTheme.colors.stroke),
    )
}
