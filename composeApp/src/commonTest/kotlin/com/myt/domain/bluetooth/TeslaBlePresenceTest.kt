package com.myt.domain.bluetooth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeslaBlePresenceTest {
    @Test
    fun matchesPhoneKeyLocalName() {
        assertTrue(TeslaBlePresence.matchesAdvertisementName("S1a87a5a7C"))
        assertTrue(TeslaBlePresence.matchesAdvertisementName("SaabbccddC"))
        assertTrue(TeslaBlePresence.matchesAdvertisementName("Tesla Model 3"))
    }

    @Test
    fun rejectsUnrelated() {
        assertFalse(TeslaBlePresence.matchesAdvertisementName("Galaxy Buds"))
        assertFalse(TeslaBlePresence.matchesAdvertisementName(""))
        assertFalse(TeslaBlePresence.matchesAdvertisementName(null))
    }
}
