package com.myt.ui.gauge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.UnitConverter
import com.myt.domain.model.TirePressures
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.TeslaCard

@Composable
fun TireGrid(
    tires: TirePressures?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    usePsi: Boolean = true,
    embedded: Boolean = false,
) {
    val colors = GaugeTheme.colors
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (embedded) 4.dp else if (compact) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (embedded) 4.dp else 8.dp),
        ) {
            if (!embedded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = ClusterIcons.tire,
                        contentDescription = null,
                        tint = Color(0xFF3D9EFF),
                        modifier = Modifier.height(18.dp).width(18.dp),
                    )
                    Text(
                        "타이어 · ${UnitConverter.pressureUnitLabel(usePsi)}",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 168.dp else 210.dp),
                contentAlignment = Alignment.Center,
            ) {
                val sideW = maxWidth * 0.28f
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(
                        modifier = Modifier.width(sideW).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TireValue("FL", tires?.frontLeftBar, usePsi, compact)
                        TireValue("RL", tires?.rearLeftBar, usePsi, compact)
                    }
                    CarTopDownSilhouette(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        flWarn = UnitConverter.pressureWarn(tires?.frontLeftBar),
                        frWarn = UnitConverter.pressureWarn(tires?.frontRightBar),
                        rlWarn = UnitConverter.pressureWarn(tires?.rearLeftBar),
                        rrWarn = UnitConverter.pressureWarn(tires?.rearRightBar),
                        hasData = tires != null,
                    )
                    Column(
                        modifier = Modifier.width(sideW).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TireValue("FR", tires?.frontRightBar, usePsi, compact)
                        TireValue("RR", tires?.rearRightBar, usePsi, compact)
                    }
                }
            }
            if (tires == null) {
                Text(
                    "공기압 데이터 대기",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    if (embedded) {
        Box(modifier.fillMaxWidth()) { body() }
    } else {
        TeslaCard(modifier = modifier.fillMaxWidth(), accent = Color(0xFF3D9EFF)) { body() }
    }
}

@Composable
private fun TireValue(
    label: String,
    bar: Float?,
    usePsi: Boolean,
    compact: Boolean,
) {
    val colors = GaugeTheme.colors
    val value = bar?.let { UnitConverter.formatPressure(it, usePsi) } ?: "--"
    val warn = UnitConverter.pressureWarn(bar)
    val accent = when {
        bar == null -> colors.textSecondary
        warn -> colors.socRed
        else -> colors.socGreen
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = accent,
            fontSize = if (compact) 20.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            UnitConverter.pressureUnitLabel(usePsi),
            color = colors.textSecondary,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun CarTopDownSilhouette(
    modifier: Modifier = Modifier,
    flWarn: Boolean,
    frWarn: Boolean,
    rlWarn: Boolean,
    rrWarn: Boolean,
    hasData: Boolean,
) {
    val body = Color(0xFF2A3344)
    val outline = Color(0xFF3D9EFF).copy(alpha = 0.75f)
    val glass = Color(0xFF64D2FF).copy(alpha = 0.35f)
    val ok = Color(0xFF30D158)
    val warn = Color(0xFFE82127)
    val unknown = Color.White.copy(alpha = 0.35f)
    Canvas(modifier = modifier.fillMaxSize(), onDraw = {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        // Body
        val bodyLeft = w * 0.28f
        val bodyTop = h * 0.12f
        val bodyW = w * 0.44f
        val bodyH = h * 0.76f
        drawRoundRect(
            color = body,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.28f, bodyW * 0.28f),
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.28f, bodyW * 0.28f),
            style = Stroke(width = 3f),
        )
        // Cabin glass
        drawRoundRect(
            color = glass,
            topLeft = Offset(bodyLeft + bodyW * 0.18f, bodyTop + bodyH * 0.22f),
            size = Size(bodyW * 0.64f, bodyH * 0.28f),
            cornerRadius = CornerRadius(10f, 10f),
        )
        // Hood line
        drawLine(
            color = outline.copy(alpha = 0.45f),
            start = Offset(bodyLeft + bodyW * 0.15f, bodyTop + bodyH * 0.18f),
            end = Offset(bodyLeft + bodyW * 0.85f, bodyTop + bodyH * 0.18f),
            strokeWidth = 2f,
        )
        // Direction arrow (front)
        val arrow = Path().apply {
            moveTo(cx, bodyTop + 8f)
            lineTo(cx - 10f, bodyTop + 22f)
            lineTo(cx + 10f, bodyTop + 22f)
            close()
        }
        drawPath(arrow, Color(0xFF30D158))

        fun tireColor(warnFlag: Boolean): Color = when {
            !hasData -> unknown
            warnFlag -> warn
            else -> ok
        }
        val tireW = w * 0.16f
        val tireH = h * 0.14f
        val leftX = w * 0.08f
        val rightX = w * 0.76f
        val frontY = h * 0.22f
        val rearY = h * 0.64f
        listOf(
            Offset(leftX, frontY) to tireColor(flWarn),
            Offset(rightX, frontY) to tireColor(frWarn),
            Offset(leftX, rearY) to tireColor(rlWarn),
            Offset(rightX, rearY) to tireColor(rrWarn),
        ).forEach { (origin, color) ->
            drawRoundRect(
                color = color,
                topLeft = origin,
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
    })
}
