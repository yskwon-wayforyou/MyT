package com.myt.domain.automation

import kotlin.test.Test
import kotlin.test.assertTrue

class GeofenceMathTest {
    @Test
    fun nearbyPoints_within300m() {
        // ~111m per 0.001 deg latitude
        val d = GeofenceMath.distanceMeters(37.2886, 127.0515, 37.2890, 127.0515)
        assertTrue(d < 100.0, "expected ~44m got $d")
    }

    @Test
    fun farPoints_outside1km() {
        val d = GeofenceMath.distanceMeters(37.2886, 127.0515, 37.3000, 127.0515)
        assertTrue(d > 1000.0, "expected >1km got $d")
    }
}
