package com.myt.ui.map

import kotlin.test.Test
import kotlin.test.assertTrue

class MapRouteHtmlTest {
    @Test
    fun buildOsmMapHtml_includesLeafletAndCoords() {
        val html = buildOsmMapHtml(
            listOf(
                com.myt.domain.geo.PolylineCodec.LatLng(37.26, 127.02),
                com.myt.domain.geo.PolylineCodec.LatLng(37.27, 127.03),
            ),
        )
        assertTrue("leaflet" in html.lowercase())
        assertTrue("37.26" in html)
        assertTrue("127.03" in html)
    }

    @Test
    fun buildOsmMapHtml_emptyShowsPlaceholder() {
        val html = buildOsmMapHtml(emptyList())
        assertTrue("경로 데이터 없음" in html)
    }
}
