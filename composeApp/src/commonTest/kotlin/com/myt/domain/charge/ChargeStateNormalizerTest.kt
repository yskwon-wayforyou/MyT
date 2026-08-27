package com.myt.domain.charge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChargeStateNormalizerTest {
    @Test
    fun complete_isNotCharging() {
        assertFalse(
            ChargeStateNormalizer.isActivelyCharging(
                chargingState = "Complete",
                batteryLevel = 100,
                chargeLimitSoc = 100,
                chargerPowerKw = 0.0,
            ),
        )
    }

    @Test
    fun charging_atLimitWithNoPower_isNotCharging() {
        assertFalse(
            ChargeStateNormalizer.isActivelyCharging(
                chargingState = "Charging",
                batteryLevel = 94,
                chargeLimitSoc = 94,
                chargerPowerKw = 0.0,
            ),
        )
    }

    @Test
    fun charging_belowLimit_isActive() {
        assertTrue(
            ChargeStateNormalizer.isActivelyCharging(
                chargingState = "Charging",
                batteryLevel = 94,
                chargeLimitSoc = 100,
                chargerPowerKw = 11.0,
            ),
        )
    }

    @Test
    fun uiLabel_complete() {
        val info = ChargeStateNormalizer.toChargeInfo(
            chargingState = "Complete",
            batteryLevel = 100,
            chargeLimitSoc = 100,
            chargerPowerKw = 0.0,
            chargeRate = null,
            timeToFullHours = 0.0,
        )
        assertFalse(info.isCharging)
        assertEquals("충전 완료 100%", ChargeStateNormalizer.uiChargeLabel(100f, info))
    }
}
