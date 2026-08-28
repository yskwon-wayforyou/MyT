package com.myt.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.myt.ui.UiLabels
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.cos
import kotlin.math.sin

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
    Box(modifier = modifier.background(Color(0xFF0A1018))) {
        if (latitude != null && longitude != null) {
            val heading = headingDegrees ?: 0f
            val zoom = remember(latitude, radiusMeters) {
                zoomForRadiusMeters(latitude, radiusMeters).toDouble()
            }
            val offsetCenter = remember(latitude, longitude, heading, radiusMeters) {
                offsetCenterPoint(latitude, longitude, heading, radiusMeters * 0.22)
            }
            val markerSignature = remember(markers, highlightPulse) {
                markers.joinToString("|") {
                    "${it.kind}:${it.id}:${it.highlighted}:${"%.5f".format(it.latitude)}:${"%.5f".format(it.longitude)}"
                } + ":p$highlightPulse"
            }
            val mapKey = remember(offsetCenter, radiusMeters, markerSignature, heading) {
                "${"%.5f".format(offsetCenter.first)}:${"%.5f".format(offsetCenter.second)}:$radiusMeters:$markerSignature:$heading"
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        setMultiTouchControls(false)
                        isHorizontalMapRepetitionEnabled = false
                        isVerticalMapRepetitionEnabled = false
                        setBuiltInZoomControls(false)
                        setTilesScaledToDpi(true)
                        controller.setZoom(zoom)
                        tag = mapKey
                        onResume()
                    }
                },
                update = { mapView ->
                    mapView.controller.setZoom(zoom)
                    mapView.mapOrientation = -heading
                    mapView.controller.setCenter(GeoPoint(offsetCenter.first, offsetCenter.second))
                    if (mapView.tag != mapKey) {
                        mapView.tag = mapKey
                        mapView.overlays.clear()
                    }
                    rebuildMarkers(
                        mapView = mapView,
                        latitude = latitude,
                        longitude = longitude,
                        headingDegrees = heading,
                        mapRotated = true,
                        markers = markers,
                        highlightPulse = highlightPulse,
                    )
                    mapView.invalidate()
                },
                onRelease = { mapView ->
                    mapView.onPause()
                },
            )
            Text(
                "© OpenStreetMap",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
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
        }
    }
}

/** Shift map center backward so the vehicle sits ~2/3 from the top (more road ahead visible). */
internal fun offsetCenterPoint(
    lat: Double,
    lng: Double,
    headingDegrees: Float,
    offsetMeters: Double,
): Pair<Double, Double> {
    val backHeadingRad = Math.toRadians((headingDegrees + 180f).toDouble())
    val dNorth = offsetMeters * cos(backHeadingRad)
    val dEast = offsetMeters * sin(backHeadingRad)
    val dLat = dNorth / 111_320.0
    val dLng = dEast / (111_320.0 * cos(Math.toRadians(lat)))
    return lat + dLat to lng + dLng
}

private fun rebuildMarkers(
    mapView: MapView,
    latitude: Double,
    longitude: Double,
    headingDegrees: Float?,
    mapRotated: Boolean,
    markers: List<LiveMapMarker>,
    highlightPulse: Float,
) {
    mapView.overlays.clear()
    mapView.overlays.add(
        Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setFlat(true)
            icon = MapMarkerIcons.vehicleIcon(mapView.context, if (mapRotated) null else headingDegrees)
            rotation = if (mapRotated) 0f else headingDegrees ?: 0f
            title = "차량"
        },
    )
    markers.forEach { m ->
        val scale = if (m.highlighted) 1.15f + 0.2f * highlightPulse else 1f
        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(m.latitude, m.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = when (m.kind) {
                    "camera" -> MapMarkerIcons.cameraIcon(mapView.context, highlighted = m.highlighted, scale = scale)
                    "dest" -> MapMarkerIcons.destIcon(mapView.context)
                    else -> MapMarkerIcons.cameraIcon(mapView.context, highlighted = m.highlighted, scale = scale)
                }
                title = m.label
            },
        )
    }
}
