package com.myt.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS map: canvas polyline fallback.
 * WKWebView Leaflet requires Xcode license acceptance for UIKit interop builds;
 * Android uses OSM WebView. Swift ContentView can host WKWebView overlay later.
 */
@Composable
actual fun MapRouteView(
    polylineEncoded: String?,
    modifier: Modifier,
) {
    RoutePolylineView(polylineEncoded = polylineEncoded, modifier = modifier)
}
