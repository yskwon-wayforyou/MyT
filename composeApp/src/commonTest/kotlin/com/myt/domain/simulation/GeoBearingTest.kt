package com.myt.domain.simulation

import kotlin.test.Test
import kotlin.test.assertTrue

class GeoBearingTest {
    @Test
    fun southbound_near180() {
        val b = bearingDegrees(37.2900, 127.0532, 37.2890, 127.0532)
        assertTrue(b in 170f..190f, "expected ~180 got $b")
    }

    @Test
    fun northeast_around45() {
        val b = bearingDegrees(37.3372, 127.1023, 37.33755, 127.10275)
        assertTrue(b in 30f..60f, "expected ~45 got $b")
    }
}
