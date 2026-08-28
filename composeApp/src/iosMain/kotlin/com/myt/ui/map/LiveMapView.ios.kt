package com.myt.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.myt.ui.UiLabels
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos

/** iOS schematic fallback — no OSM tiles yet. */
@Composable
actual fun LiveMapView(
    latitude: Double?,
    longitude: Double?,
    headingDegrees: Float?,
    radiusMeters: Int,
    markers: List<LiveMapMarker>,
    highlightPulse: Float,
    modifier: Modifier,
) {
    if (latitude == null || longitude == null) {
        Box(
            modifier = modifier.background(Color(0xFF0A0A10)).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${UiLabels.mapLocationWaiting}\n${UiLabels.mapLocationHint}",
                color = Color(0xFF9A9AA3),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }
    val heading = headingDegrees ?: 0f
    Box(modifier = modifier.background(Color(0xFF0A1220)).fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(size.width, size.height) * 0.38f
            drawCircle(Color(0xFF3D9EFF).copy(alpha = 0.1f), r)
            drawCircle(Color(0xFF3D9EFF).copy(alpha = 0.45f), r, style = Stroke(2f))
            val ppm = r / radiusMeters.coerceAtLeast(1).toFloat()
            markers.forEach { m ->
                val dLat = (m.latitude - latitude) * 111_320.0
                val dLng = (m.longitude - longitude) * 111_320.0 * cos(latitude * PI / 180.0)
                val mx = cx + (dLng * ppm).toFloat()
                val my = cy - (dLat * ppm).toFloat()
                val color = when (m.kind) {
                    "camera" -> Color(0xFFFF6A00)
                    "dest" -> Color(0xFF30D158)
                    else -> Color(0xFF64D2FF)
                }
                drawCircle(color, 8f, Offset(mx, my))
            }
            rotate(heading, Offset(cx, cy)) {
                val path = Path().apply {
                    moveTo(cx, cy - 16f)
                    lineTo(cx - 8f, cy + 10f)
                    lineTo(cx + 8f, cy + 10f)
                    close()
                }
                drawPath(path, Color(0xFF30D158))
            }
        })
        Text(
            "≈${radiusMeters}m · schematic",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )
    }
}
