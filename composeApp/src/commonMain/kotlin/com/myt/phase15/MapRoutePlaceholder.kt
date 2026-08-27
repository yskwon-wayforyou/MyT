package com.myt.phase15

import com.myt.domain.geo.PolylineCodec

/** M21 — Phase 1.5: Map route display placeholder (offline polyline rendering). */
data class MapRoutePlaceholder(
    val polylineEncoded: String? = null,
    val message: String = "Map view — Phase 1.5 (offline polyline rendering)",
) {
    fun decodedPoints(): List<PolylineCodec.LatLng> =
        PolylineCodec.decode(polylineEncoded.orEmpty())
}
