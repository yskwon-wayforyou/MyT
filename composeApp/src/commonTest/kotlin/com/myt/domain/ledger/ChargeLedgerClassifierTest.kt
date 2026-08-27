package com.myt.domain.ledger

import kotlin.test.Test
import kotlin.test.assertEquals
import com.myt.domain.history.ChargeHistoryItem

class ChargeLedgerClassifierTest {
    @Test
    fun classifiesByPeakPower() {
        assertEquals(ChargerKind.Supercharger, ChargeLedgerClassifier.classify(120f))
        assertEquals(ChargerKind.PublicAc, ChargeLedgerClassifier.classify(22f))
        assertEquals(ChargerKind.Home, ChargeLedgerClassifier.classify(7f))
        assertEquals(ChargerKind.Unknown, ChargeLedgerClassifier.classify(null))
    }

    @Test
    fun estimatesCost() {
        val session = ChargeHistoryItem(
            id = "1",
            vin = "VIN",
            startedAtMs = 1L,
            endedAtMs = 2L,
            startSoc = 20f,
            endSoc = 80f,
            energyKwh = 40f,
            peakKw = 150f,
        )
        val entry = ChargeLedgerClassifier.toEntry(session)
        assertEquals(ChargerKind.Supercharger, entry.kind)
        assertEquals(40f * 385f, entry.estimatedCostKrw!!, 0.01f)
    }

    @Test
    fun summarizesByKind() {
        val sessions = listOf(
            ChargeHistoryItem("a", "V", 1, 2, 10f, 50f, 20f, 150f),
            ChargeHistoryItem("b", "V", 3, 4, 40f, 60f, 10f, 7f),
        )
        val summary = ChargeLedgerClassifier.summarize(sessions, "이번 달")
        assertEquals(30f, summary.totalKwh, 0.01f)
        assertEquals(20f * 385f + 10f * 120f, summary.totalCostKrw, 0.01f)
    }
}
