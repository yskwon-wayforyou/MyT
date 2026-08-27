package com.myt.domain.usecase

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoiDataStatusUseCaseTest {
    @Test
    fun monthlySla_marksStaleAfter35Days() {
        val last = 1_000_000L
        val fresh = last + PoiDataStatusUseCase.MONTHLY_SLA_MS
        val stale = fresh + 1
        assertFalse(PoiDataStatusUseCase.isMonthlySlaStale(last, fresh))
        assertTrue(PoiDataStatusUseCase.isMonthlySlaStale(last, stale))
        assertFalse(PoiDataStatusUseCase.isMonthlySlaStale(null, stale))
    }
}
