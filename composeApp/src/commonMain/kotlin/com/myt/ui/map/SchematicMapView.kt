package com.myt.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Offline-friendly mini map (grid + vehicle + radius + markers).
 * Always drawn above the WebView so simulation/offline tests still show map context.
 */
@Composable
fun SchematicMapView(
    latitude: Double,
    longitude: Double,
    headingDegrees: Float?,
    radiusMeters: Int,
    markers: List<LiveMapMarker>,
    modifier: Modifier = Modifier,
) {
    val heading = headingDegrees ?: 0f
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "지도 · ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}"
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bg = Color(0xFF0A1018).copy(alpha = 0.82f)
            drawRect(bg)
            val grid = Color(0xFF1E2838)
            val step = size.minDimension / 8f
            var x = 0f
            while (x <= size.width) {
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            // Main cross streets
            val road = Color(0xFF2A3548)
            drawLine(road, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 3f)
            drawLine(road, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 3f)

            val cx = size.width / 2f
            val cy = size.height / 2f
            val radiusPx = size.minDimension * 0.38f
            drawCircle(
                color = Color(0xFF3D9EFF).copy(alpha = 0.12f),
                radius = radiusPx,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color(0xFF3D9EFF).copy(alpha = 0.55f),
                radius = radiusPx,
                center = Offset(cx, cy),
                style = Stroke(width = 2f),
            )

            val ppm = radiusPx / radiusMeters.coerceAtLeast(1).toFloat()
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
                drawCircle(color = color, radius = 8f, center = Offset(mx, my))
            }

            rotate(heading, Offset(cx, cy)) {
                val carPath = Path().apply {
                    moveTo(cx, cy - 14f)
                    lineTo(cx + 10f, cy + 10f)
                    lineTo(cx - 10f, cy + 10f)
                    close()
                }
                drawPath(path = carPath, color = Color(0xFF30D158))
            }
            val rad = Math.toRadians(heading.toDouble() - 90.0)
            val tipX = cx + cos(rad).toFloat() * 22f
            val tipY = cy + sin(rad).toFloat() * 22f
            drawLine(Color(0xFF30D158), Offset(cx, cy), Offset(tipX, tipY), strokeWidth = 3f)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        ) {
            Text(
                "${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
            )
            Text(
                "반경 ${radiusMeters}m · schematic",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
            )
        }
    }
}
