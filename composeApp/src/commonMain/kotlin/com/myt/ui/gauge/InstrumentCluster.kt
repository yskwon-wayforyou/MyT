package com.myt.ui.gauge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.UnitConverter
import com.myt.domain.model.Gear
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.clusterAmbient
import com.myt.ui.theme.clusterArcActive
import com.myt.ui.theme.clusterArcDanger
import com.myt.ui.theme.clusterArcSafe
import com.myt.ui.theme.clusterArcTrack
import com.myt.ui.theme.clusterArcWarn
import com.myt.ui.theme.clusterBezelInner
import com.myt.ui.theme.clusterBezelMid
import com.myt.ui.theme.clusterBezelOuter
import com.myt.ui.theme.clusterFace
import com.myt.ui.theme.clusterFaceDeep
import com.myt.ui.theme.clusterGlow
import com.myt.ui.theme.clusterHud
import com.myt.ui.theme.clusterHudBorder
import com.myt.ui.theme.clusterNeedle
import com.myt.ui.theme.clusterNeedleGlow
import com.myt.ui.theme.clusterScanline
import com.myt.ui.theme.clusterTickLabel
import com.myt.ui.theme.clusterTickMajor
import com.myt.ui.theme.clusterTickMinor
import com.myt.ui.theme.metallicHighlight
import com.myt.ui.theme.metallicShadow
import com.myt.ui.theme.panelGlass
import com.myt.ui.theme.panelGlassBorder
import com.myt.ui.theme.powerDrive
import com.myt.ui.theme.powerRegen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val MAX_SPEED_KMH = 160f

@Composable
fun InstrumentCluster(
    speedKmh: Float,
    gear: Gear,
    socPercent: Float,
    rangeKm: Float,
    useKmh: Boolean = true,
    compact: Boolean = false,
    showSpeed: Boolean = true,
    showGear: Boolean = true,
    powerKw: Float? = null,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val dialSize = if (compact) 188.dp else 278.dp
    val bezel = if (compact) 12.dp else 16.dp
    val totalSize = dialSize + bezel * 2 + if (compact) 24.dp else 36.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Ambient under-glow
        Box(
            modifier = Modifier
                .size(totalSize * 1.08f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.clusterAmbient,
                            colors.clusterGlow.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showSpeed) {
                Box(
                    modifier = Modifier.size(totalSize),
                    contentAlignment = Alignment.Center,
                ) {
                    SpeedGaugeDial(
                        speedKmh = speedKmh,
                        useKmh = useKmh,
                        diameter = dialSize,
                        bezel = bezel,
                        compact = compact,
                    )

                    // Frosted HUD readout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(if (compact) 18.dp else 24.dp))
                            .background(colors.clusterHud)
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        colors.clusterHudBorder.copy(alpha = 0.3f),
                                        colors.clusterHudBorder,
                                        colors.clusterHudBorder.copy(alpha = 0.3f),
                                    ),
                                ),
                                RoundedCornerShape(if (compact) 18.dp else 24.dp),
                            )
                            .padding(
                                horizontal = if (compact) 20.dp else 28.dp,
                                vertical = if (compact) 10.dp else 14.dp,
                            ),
                    ) {
                        Text(
                            text = UnitConverter.formatSpeed(speedKmh, useKmh),
                            color = colors.speed,
                            fontSize = if (compact) 48.sp else 76.sp,
                            fontWeight = FontWeight.ExtraLight,
                            letterSpacing = (-3).sp,
                            lineHeight = if (compact) 48.sp else 76.sp,
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = UnitConverter.speedUnitLabel(useKmh).uppercase(),
                                color = colors.clusterArcActive,
                                fontSize = if (compact) 10.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                            )
                            if (rangeKm > 0f) {
                                Text(
                                    text = "· ${rangeKm.toInt()} km",
                                    color = colors.textSecondary,
                                    fontSize = if (compact) 10.sp else 11.sp,
                                )
                            }
                        }
                        powerKw?.let { kw ->
                            PowerBar(kw = kw, compact = compact, modifier = Modifier.padding(top = 6.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = if (compact) (-4).dp else (-2).dp, y = if (compact) 8.dp else 12.dp),
                    ) {
                        SocRing(
                            socPercent = socPercent,
                            size = if (compact) 52.dp else 68.dp,
                            stroke = if (compact) 5.dp else 6.dp,
                        )
                    }
                }
            }

            if (showGear) {
                ClusterGearStrip(
                    gear = gear,
                    compact = compact,
                    modifier = Modifier.offset(y = if (compact) (-8).dp else (-12).dp),
                )
            }
        }
    }
}

@Composable
private fun PowerBar(kw: Float, compact: Boolean, modifier: Modifier = Modifier) {
    val colors = GaugeTheme.colors
    val width = if (compact) 100.dp else 130.dp
    val height = if (compact) 4.dp else 5.dp
    val ratio = (kw / 120f).coerceIn(-1f, 1f)
    val barColor = if (ratio >= 0f) colors.powerDrive else colors.powerRegen

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(colors.clusterArcTrack),
    ) {
        Box(
            modifier = Modifier
                .align(if (ratio >= 0f) Alignment.CenterStart else Alignment.CenterEnd)
                .width(width * kotlin.math.abs(ratio))
                .height(height)
                .background(
                    Brush.horizontalGradient(
                        listOf(barColor.copy(alpha = 0.4f), barColor),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1.dp)
                .height(height * 2)
                .background(colors.textSecondary.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun SpeedGaugeDial(
    speedKmh: Float,
    useKmh: Boolean,
    diameter: Dp,
    bezel: Dp,
    compact: Boolean,
) {
    val colors = GaugeTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val maxSpeed = if (useKmh) MAX_SPEED_KMH else UnitConverter.kmhToMph(MAX_SPEED_KMH)
    val displaySpeed = if (useKmh) speedKmh else UnitConverter.kmhToMph(speedKmh)
    val ratio = (displaySpeed / maxSpeed).coerceIn(0f, 1f)
    val startAngle = 135f
    val sweepTotal = 270f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val outer = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val bezelPx = bezel.toPx()
        val faceRadius = outer / 2f - bezelPx

        // Outer metallic rings
        drawCircle(
            brush = Brush.radialGradient(
                0f to colors.clusterBezelOuter,
                0.55f to colors.clusterBezelMid,
                0.85f to colors.clusterBezelInner,
                1f to Color(0xFF020204),
                center = center,
                radius = outer / 2f,
            ),
            radius = outer / 2f,
            center = center,
        )
        drawCircle(
            color = colors.metallicHighlight,
            radius = outer / 2f - 1f,
            center = center,
            style = Stroke(width = 1.5f),
        )
        drawCircle(
            color = colors.metallicShadow,
            radius = faceRadius + bezelPx * 0.45f,
            center = center,
            style = Stroke(width = bezelPx * 0.5f),
        )

        // Deep face + vignette
        drawCircle(color = colors.clusterFaceDeep, radius = faceRadius, center = center)
        drawCircle(
            brush = Brush.radialGradient(
                0f to colors.clusterFace.copy(alpha = 0.95f),
                0.65f to colors.clusterFace,
                1f to colors.clusterFaceDeep,
                center = center,
                radius = faceRadius * 0.98f,
            ),
            radius = faceRadius * 0.98f,
            center = center,
        )

        // Scanlines
        val lineGap = if (compact) 5f else 4f
        var y = center.y - faceRadius
        while (y < center.y + faceRadius) {
            drawLine(
                color = colors.clusterScanline,
                start = Offset(center.x - faceRadius, y),
                end = Offset(center.x + faceRadius, y),
                strokeWidth = 1f,
            )
            y += lineGap
        }

        val arcStroke = if (compact) 8f else 11f
        val arcRadius = faceRadius - arcStroke * 2.2f

        // Track ring
        drawArc(
            color = colors.clusterArcTrack,
            startAngle = startAngle,
            sweepAngle = sweepTotal,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = arcStroke, cap = StrokeCap.Round),
        )

        // Zone coloring (static reference)
        fun zone(startRatio: Float, endRatio: Float, color: Color) {
            drawArc(
                color = color.copy(alpha = 0.35f),
                startAngle = startAngle + sweepTotal * startRatio,
                sweepAngle = sweepTotal * (endRatio - startRatio),
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = arcStroke - 2f, cap = StrokeCap.Butt),
            )
        }
        zone(0f, 0.45f, colors.clusterArcSafe)
        zone(0.45f, 0.72f, colors.clusterArcWarn)
        zone(0.72f, 1f, colors.clusterArcDanger)

        // Active glowing arc (follows speed)
        if (ratio > 0.01f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to colors.clusterArcActive.copy(alpha = 0.2f),
                    0.5f to colors.clusterArcActive,
                    1f to Color.White.copy(alpha = 0.85f),
                    center = center,
                ),
                startAngle = startAngle,
                sweepAngle = sweepTotal * ratio,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = arcStroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = colors.clusterArcActive.copy(alpha = 0.25f),
                startAngle = startAngle,
                sweepAngle = sweepTotal * ratio,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius - 4f, center.y - arcRadius - 4f),
                size = Size((arcRadius + 4f) * 2, (arcRadius + 4f) * 2),
                style = Stroke(width = arcStroke + 6f, cap = StrokeCap.Round),
            )
        }

        // Ticks + numeric labels
        val majorEvery = 20f
        val majorCount = (maxSpeed / majorEvery).toInt()
        val labelStyle = TextStyle(
            color = colors.clusterTickLabel,
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Medium,
        )
        for (i in 0..majorCount) {
            val r = i / majorCount.toFloat()
            val angleDeg = startAngle + sweepTotal * r
            val angleRad = angleDeg * PI.toFloat() / 180f
            val isMajor = i % 2 == 0
            val tickLen = if (isMajor) (if (compact) 12f else 16f) else (if (compact) 7f else 9f)
            val innerR = arcRadius - tickLen - 6f
            val outerR = arcRadius + 4f
            val tickColor = if (isMajor) colors.clusterTickMajor else colors.clusterTickMinor
            val start = Offset(center.x + cos(angleRad) * innerR, center.y + sin(angleRad) * innerR)
            val end = Offset(center.x + cos(angleRad) * outerR, center.y + sin(angleRad) * outerR)
            drawLine(color = tickColor, start = start, end = end, strokeWidth = if (isMajor) 2.5f else 1f)

            if (isMajor) {
                val speedLabel = (i * majorEvery).toInt().toString()
                val labelR = arcRadius - tickLen - (if (compact) 16f else 20f)
                val lx = center.x + cos(angleRad) * labelR
                val ly = center.y + sin(angleRad) * labelR
                val textLayout = textMeasurer.measure(speedLabel, labelStyle)
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(lx - textLayout.size.width / 2f, ly - textLayout.size.height / 2f),
                )
            }
        }

        // Needle with glow
        val needleAngle = startAngle + sweepTotal * ratio
        rotate(needleAngle, pivot = center) {
            val needleLen = arcRadius - arcStroke
            val path = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x - 4f, center.y - needleLen * 0.15f)
                lineTo(center.x, center.y - needleLen)
                lineTo(center.x + 4f, center.y - needleLen * 0.15f)
                close()
            }
            drawPath(path = path, color = colors.clusterNeedleGlow.copy(alpha = 0.35f))
            drawPath(path = path, color = colors.clusterNeedle)
            drawLine(
                color = colors.clusterNeedleGlow,
                start = center,
                end = Offset(center.x, center.y - needleLen + 8f),
                strokeWidth = if (compact) 6f else 8f,
                cap = StrokeCap.Round,
            )
        }

        // Center hub
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF3A3A42), colors.clusterBezelInner, Color(0xFF050508)),
                center = center,
                radius = if (compact) 10f else 14f,
            ),
            radius = if (compact) 10f else 14f,
            center = center,
        )
        drawCircle(color = colors.accent.copy(alpha = 0.6f), radius = if (compact) 4f else 5f, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = if (compact) 2f else 2.5f, center = center)
    }
}

@Composable
private fun ClusterGearStrip(
    gear: Gear,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GaugeTheme.colors
    val shape = RoundedCornerShape(if (compact) 18.dp else 26.dp)

    Row(
        modifier = modifier
            .shadow(if (compact) 6.dp else 10.dp, shape, ambientColor = colors.clusterAmbient)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(colors.panelGlass.copy(alpha = 0.98f), colors.clusterFaceDeep.copy(alpha = 0.92f)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        colors.panelGlassBorder.copy(alpha = 0.25f),
                        colors.clusterArcActive.copy(alpha = 0.5f),
                        colors.panelGlassBorder.copy(alpha = 0.25f),
                    ),
                ),
                shape,
            )
            .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Gear.entries.forEach { item ->
            val selected = item == gear
            val gearColor = when (item) {
                Gear.PARK -> colors.gearP
                Gear.REVERSE -> colors.gearR
                Gear.NEUTRAL -> colors.gearN
                Gear.DRIVE -> colors.gearD
            }
            val segShape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
            Box(
                modifier = Modifier
                    .clip(segShape)
                    .then(
                        if (selected) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(gearColor.copy(alpha = 0.35f), gearColor.copy(alpha = 0.12f)),
                                ),
                            ).border(1.dp, gearColor.copy(alpha = 0.75f), segShape)
                        } else {
                            Modifier.background(Color.White.copy(alpha = 0.03f))
                        },
                    )
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 4.dp else 5.dp)
                                .clip(CircleShape)
                                .background(gearColor),
                        )
                    }
                    Text(
                        text = item.displayLabel(),
                        color = if (selected) gearColor else colors.textSecondary.copy(alpha = 0.4f),
                        fontSize = if (selected) {
                            if (compact) 18.sp else 24.sp
                        } else {
                            if (compact) 13.sp else 16.sp
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Light,
                        letterSpacing = if (selected) 1.sp else 0.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedDisplay(
    speedKmh: Float,
    gear: Gear,
    socPercent: Float = 0f,
    rangeKm: Float = 0f,
    useKmh: Boolean = true,
    compact: Boolean = false,
    showSpeed: Boolean = true,
    showGear: Boolean = true,
    powerKw: Float? = null,
    modifier: Modifier = Modifier,
) {
    InstrumentCluster(
        speedKmh = speedKmh,
        gear = gear,
        socPercent = socPercent,
        rangeKm = rangeKm,
        useKmh = useKmh,
        compact = compact,
        showSpeed = showSpeed,
        showGear = showGear,
        powerKw = powerKw,
        modifier = modifier,
    )
}

@Composable
fun GearPill(gear: Gear, modifier: Modifier = Modifier) {
    ClusterGearStrip(gear = gear, compact = false, modifier = modifier)
}

@Composable
fun GearSelector(
    gear: Gear,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ClusterGearStrip(gear = gear, compact = compact, modifier = modifier)
}
