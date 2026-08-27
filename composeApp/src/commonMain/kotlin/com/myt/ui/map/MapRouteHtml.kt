package com.myt.ui.map

import com.myt.domain.geo.PolylineCodec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

internal fun buildOsmMapHtml(points: List<PolylineCodec.LatLng>): String {
    if (points.isEmpty()) {
        return """
            <!DOCTYPE html><html><body style="margin:0;background:#0a0a10;color:#888;
            font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh">
            <p>경로 데이터 없음</p></body></html>
        """.trimIndent()
    }
    val latitudes = points.map { it.lat }
    val longitudes = points.map { it.lng }
    val centerLat = latitudes.average()
    val centerLng = longitudes.average()
    val latSpan = max(0.002, latitudes.max() - latitudes.min())
    val lngSpan = max(0.002, longitudes.max() - longitudes.min())
    val zoom = when {
        latSpan > 0.5 || lngSpan > 0.5 -> 10
        latSpan > 0.2 || lngSpan > 0.2 -> 12
        latSpan > 0.05 || lngSpan > 0.05 -> 13
        else -> 14
    }
    val coordsJson = points.joinToString(",") { "[${it.lat},${it.lng}]" }
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>html,body,#map{margin:0;height:100%;background:#0a0a10}</style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', { zoomControl: true }).setView([$centerLat, $centerLng], $zoom);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              attribution: '&copy; OpenStreetMap'
            }).addTo(map);
            var coords = [$coordsJson];
            var line = L.polyline(coords, { color: '#64D2FF', weight: 5, opacity: 0.9 }).addTo(map);
            L.circleMarker(coords[0], { radius: 7, color: '#30D158', fillColor: '#30D158', fillOpacity: 1 }).addTo(map);
            L.circleMarker(coords[coords.length-1], { radius: 7, color: '#E82127', fillColor: '#E82127', fillOpacity: 1 }).addTo(map);
            map.fitBounds(line.getBounds(), { padding: [24, 24] });
          </script>
        </body>
        </html>
    """.trimIndent()
}

/** Zoom for street-level OSM labels in the gauge map pane. */
internal fun zoomForRadiusMeters(latitude: Double, radiusMeters: Int): Int {
    val clamped = radiusMeters.coerceIn(200, 2_000)
    val metersPerPixelAtZoom0 = 156_543.03392 * cos(latitude * PI / 180.0).coerceAtLeast(0.01)
    val targetMetersPerPixel = (clamped * 2.0) / 360.0
    val zoom = (ln(metersPerPixelAtZoom0 / targetMetersPerPixel) / ln(2.0)).roundToInt()
    return zoom.coerceIn(16, 18)
}

internal fun buildLiveMapHtml(
    latitude: Double,
    longitude: Double,
    headingDegrees: Float?,
    radiusMeters: Int,
    markers: List<LiveMapMarker>,
): String {
    val zoom = zoomForRadiusMeters(latitude, radiusMeters)
    val centerPx = latLngToWorldPixel(latitude, longitude, zoom)
    val centerTileX = (centerPx.first / 256.0).toInt()
    val centerTileY = (centerPx.second / 256.0).toInt()
    val tileRadius = 2
    val tilesHtml = buildString {
        for (dx in -tileRadius..tileRadius) {
            for (dy in -tileRadius..tileRadius) {
                val tx = centerTileX + dx
                val ty = centerTileY + dy
                val left = tx * 256 - centerPx.first.toInt()
                val top = ty * 256 - centerPx.second.toInt()
                append(
                    """<img class="tile" src="https://tile.openstreetmap.org/$zoom/$tx/$ty.png" """
                        + """style="left:${left}px;top:${top}px" alt="" loading="eager" />""",
                )
            }
        }
    }
    val markersHtml = buildString {
        markers.forEach { m ->
            val px = latLngToWorldPixel(m.latitude, m.longitude, zoom)
            val x = px.first - centerPx.first
            val y = px.second - centerPx.second
            val color = when (m.kind) {
                "camera" -> "#FF6A00"
                "dest" -> "#30D158"
                else -> "#64D2FF"
            }
            append("""<div class="poi" style="left:calc(50% + ${x}px);top:calc(50% + ${y}px);background:$color"></div>""")
        }
    }
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html,body{margin:0;height:100%;width:100%;background:#0a1018;overflow:hidden}
            .viewport{position:relative;width:100%;height:100%;overflow:hidden;background:#0a1018}
            .world{position:absolute;left:50%;top:50%;width:0;height:0}
            .tile{position:absolute;width:256px;height:256px;image-rendering:auto}
            .car{position:absolute;left:50%;top:50%;width:14px;height:14px;margin:-7px 0 0 -7px;border-radius:50%;background:#30D158;border:2px solid #fff;box-shadow:0 0 8px #30D158;z-index:5}
            .poi{position:absolute;width:12px;height:12px;margin:-6px 0 0 -6px;border-radius:50%;border:2px solid #fff;z-index:4}
            .attr{position:absolute;right:6px;bottom:4px;font:9px sans-serif;color:#aaa;background:rgba(0,0,0,.45);padding:2px 4px;border-radius:4px;z-index:6}
          </style>
        </head>
        <body>
          <div class="viewport">
            <div class="world">$tilesHtml</div>
            $markersHtml
            <div class="car"></div>
            <div class="attr">&copy; OpenStreetMap</div>
          </div>
        </body>
        </html>
    """.trimIndent()
}

internal fun latLngToWorldPixel(lat: Double, lng: Double, zoom: Int): Pair<Double, Double> {
    val scale = 256.0 * (1 shl zoom)
    val x = (lng + 180.0) / 360.0 * scale
    val sinLat = sin(lat * PI / 180.0).coerceIn(-0.9999, 0.9999)
    val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * scale
    return x to y
}

internal fun buildEmptyLiveMapHtml(message: String): String = """
    <!DOCTYPE html><html><body style="margin:0;background:#0a0a10;color:#9A9AA3;
    font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;text-align:center;padding:16px">
    <p>$message</p></body></html>
""".trimIndent()
