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

@Composable
actual fun LiveMapView(
    latitude: Double?,
    longitude: Double?,
    headingDegrees: Float?,
    radiusMeters: Int,
    markers: List<LiveMapMarker>,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(Color(0xFF0A1018))) {
        if (latitude != null && longitude != null) {
            val zoom = remember(latitude, radiusMeters) {
                zoomForRadiusMeters(latitude, radiusMeters).toDouble()
            }
            val markerSignature = remember(markers) {
                markers.joinToString("|") { "${it.kind}:${"%.5f".format(it.latitude)}:${"%.5f".format(it.longitude)}" }
            }
            val mapKey = remember(latitude, longitude, radiusMeters, markerSignature, headingDegrees) {
                "${"%.5f".format(latitude)}:${"%.5f".format(longitude)}:$radiusMeters:$markerSignature:${headingDegrees ?: 0f}"
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
                        controller.setCenter(GeoPoint(latitude, longitude))
                        tag = mapKey
                        onResume()
                    }
                },
                update = { mapView ->
                    mapView.controller.setZoom(zoom)
                    mapView.controller.setCenter(GeoPoint(latitude, longitude))
                    if (mapView.tag != mapKey) {
                        mapView.tag = mapKey
                        mapView.overlays.clear()
                    }
                    rebuildMarkers(
                        mapView = mapView,
                        latitude = latitude,
                        longitude = longitude,
                        headingDegrees = headingDegrees,
                        markers = markers,
                    )
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

private fun rebuildMarkers(
    mapView: MapView,
    latitude: Double,
    longitude: Double,
    headingDegrees: Float?,
    markers: List<LiveMapMarker>,
) {
    mapView.overlays.clear()
    mapView.overlays.add(
        Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = MapMarkerIcons.vehicleIcon(mapView.context, headingDegrees)
            rotation = headingDegrees ?: 0f
            title = "차량"
        },
    )
    markers.forEach { m ->
        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(m.latitude, m.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = when (m.kind) {
                    "camera" -> MapMarkerIcons.cameraIcon(mapView.context)
                    "dest" -> MapMarkerIcons.destIcon(mapView.context)
                    else -> MapMarkerIcons.cameraIcon(mapView.context)
                }
                title = m.label
            },
        )
    }
}
