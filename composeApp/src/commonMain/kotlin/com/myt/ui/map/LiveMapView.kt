package com.myt.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class LiveMapMarker(
    val latitude: Double,
    val longitude: Double,
    /** car | camera | dest | poi */
    val kind: String,
    val label: String? = null,
)

/**
 * Live vehicle-centered map (~radiusMeters visible).
 * Android: OSM Leaflet WebView (streets, buildings, labels).
 * iOS: canvas schematic fallback until MapKit/OSM is wired.
 */
@Composable
expect fun LiveMapView(
    latitude: Double?,
    longitude: Double?,
    headingDegrees: Float? = null,
    radiusMeters: Int = 750,
    markers: List<LiveMapMarker> = emptyList(),
    modifier: Modifier = Modifier,
)
