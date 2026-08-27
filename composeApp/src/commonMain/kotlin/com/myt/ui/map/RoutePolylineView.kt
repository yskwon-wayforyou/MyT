package com.myt.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.geo.PolylineCodec
import com.myt.ui.theme.GaugeTheme
import com.myt.ui.theme.accentBlue
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class RoutePreviewStats(
    val pointCount: Int,
    val approxDistanceKm: Float,
    val latSpanMeters: Int,
    val lngSpanMeters: Int,
    val start: PolylineCodec.LatLng,
    val end: PolylineCodec.LatLng,
)

fun decodeRoutePoints(polylineEncoded: String?): List<PolylineCodec.LatLng> =
    polylineEncoded?.let { enc ->
        PolylineCodec.decode(enc).filter { !(it.lat.isNaN() || it.lng.isNaN()) }
    }.orEmpty()

fun buildRoutePreviewStats(points: List<PolylineCodec.LatLng>): RoutePreviewStats? {
    if (points.size < 2) return null
    val minLat = points.minOf { it.lat }
    val maxLat = points.maxOf { it.lat }
    val minLng = points.minOf { it.lng }
    val maxLng = points.maxOf { it.lng }
    val totalDistanceM = points.zipWithNext().sumOf { (a, b) -> haversineMeters(a, b) }
    val latSpanMeters = haversineMeters(
        PolylineCodec.LatLng(minLat, minLng),
        PolylineCodec.LatLng(maxLat, minLng),
    ).toInt()
    val lngSpanMeters = haversineMeters(
        PolylineCodec.LatLng(minLat, minLng),
        PolylineCodec.LatLng(minLat, maxLng),
    ).toInt()
    return RoutePreviewStats(
        pointCount = points.size,
        approxDistanceKm = (totalDistanceM / 1000.0).toFloat(),
        latSpanMeters = latSpanMeters,
        lngSpanMeters = lngSpanMeters,
        start = points.first(),
        end = points.last(),
    )
}

@Composable
fun RoutePolylineView(
    polylineEncoded: String?,
    modifier: Modifier = Modifier,
    strokeColor: Color = GaugeTheme.colors.accentBlue,
    strokeWidth: Dp = 3.dp,
    emptyText: String = "경로 데이터가 없습니다.",
) {
    val colors = GaugeTheme.colors
    val points = remember(polylineEncoded) { decodeRoutePoints(polylineEncoded) }

    if (polylineEncoded.isNullOrBlank() || points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = emptyText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GaugeTheme.colors.textSecondary)
        }
        return
    }

    val minLat = points.minOf { it.lat }
    val maxLat = points.maxOf { it.lat }
    val minLng = points.minOf { it.lng }
    val maxLng = points.maxOf { it.lng }

    Canvas(modifier = modifier) {
        val padding = 14.dp.toPx()
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val latSpan = (maxLat - minLat).takeIf { it != 0.0 } ?: 1e-9
        val lngSpan = (maxLng - minLng).takeIf { it != 0.0 } ?: 1e-9

        fun project(lat: Double, lng: Double): Offset {
            val x = padding + ((lng - minLng) / lngSpan).toFloat() * w
            val y = padding + (1f - ((lat - minLat) / latSpan).toFloat()) * h
            return Offset(x, y)
        }

        val gridColor = colors.textSecondary.copy(alpha = 0.12f)
        repeat(4) { index ->
            val x = padding + w * ((index + 1) / 5f)
            val y = padding + h * ((index + 1) / 5f)
            drawLine(
                color = gridColor,
                start = Offset(x, padding),
                end = Offset(x, size.height - padding),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val path = Path().apply {
            val first = points.first()
            moveTo(project(first.lat, first.lng).x, project(first.lat, first.lng).y)
            points.drop(1).forEach { p ->
                val o = project(p.lat, p.lng)
                lineTo(o.x, o.y)
            }
        }

        drawPath(
            path = path,
            color = strokeColor.copy(alpha = 0.25f),
            style = Stroke(width = strokeWidth.toPx() + 3f, cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
        )

        // Start / end markers
        val start = project(points.first().lat, points.first().lng)
        val end = project(points.last().lat, points.last().lng)
        drawCircle(
            color = strokeColor.copy(alpha = 0.9f),
            radius = 4.dp.toPx(),
            center = start,
        )
        drawCircle(
            color = colors.socGreen.copy(alpha = 0.9f),
            radius = 4.dp.toPx(),
            center = end,
        )
    }
}

private fun haversineMeters(a: PolylineCodec.LatLng, b: PolylineCodec.LatLng): Double {
    val dLat = (b.lat - a.lat) * PI / 180.0
    val dLng = (b.lng - a.lng) * PI / 180.0
    val lat1 = a.lat * PI / 180.0
    val lat2 = b.lat * PI / 180.0
    val x = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
    return 6_371_000.0 * 2 * atan2(sqrt(x), sqrt(1 - x))
}

