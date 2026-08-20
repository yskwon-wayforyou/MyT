package com.myt.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitConverterTest {
    @Test
    fun mphToKmh_convertsCorrectly() {
        assertEquals(96.5604f, UnitConverter.mphToKmh(60f), 0.001f)
    }

    @Test
    fun psiToBar_convertsCorrectly() {
        assertEquals(2.41345f, UnitConverter.psiToBar(35f), 0.001f)
    }

    @Test
    fun formatSpeed_usesKmhByDefault() {
        assertEquals("108", UnitConverter.formatSpeed(108f, useKmh = true))
        assertTrue(UnitConverter.speedUnitLabel(useKmh = true).contains("km"))
    }
}
