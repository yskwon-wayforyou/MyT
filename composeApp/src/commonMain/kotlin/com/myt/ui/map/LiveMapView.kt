package com.myt.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class LiveMapMarker(
    val latitude: Double,
    val longitude: Double,
    /** car | camera | dest | poi */
    val kind: String,
    val label: String? = null,
    val id: String? = null,
    val highlighted: Boolean = false,
)

@Composable
expect fun LiveMapView(
    latitude: Double?,
    longitude: Double?,
    headingDegrees: Float? = null,
    radiusMeters: Int = 750,
    markers: List<LiveMapMarker> = emptyList(),
    highlightPulse: Float = 1f,
    modifier: Modifier = Modifier,
)
