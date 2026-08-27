package com.myt.ui.map

import com.myt.domain.geo.PolylineCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoutePolylineViewTest {
    @Test
    fun buildsRoutePreviewStatsFromPolyline() {
        val encoded = PolylineCodec.encode(
            listOf(
                PolylineCodec.LatLng(37.4985, 127.0280),
                PolylineCodec.LatLng(37.4990, 127.0290),
                PolylineCodec.LatLng(37.5000, 127.0300),
            ),
        )

        val points = decodeRoutePoints(encoded)
        val stats = buildRoutePreviewStats(points)

        assertEquals(3, points.size)
        assertNotNull(stats)
        assertEquals(3, stats.pointCount)
        assertTrue(stats.approxDistanceKm > 0f)
        assertTrue(stats.latSpanMeters > 0)
        assertTrue(stats.lngSpanMeters > 0)
        assertEquals(37.4985, stats.start.lat)
        assertEquals(127.0300, stats.end.lng)
    }
}

