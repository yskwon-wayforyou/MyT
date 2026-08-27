package com.myt.domain.ledger

import com.myt.domain.history.ChargeHistoryItem
import kotlinx.serialization.Serializable

/** FR-CH10 — charger category for expense ledger. */
@Serializable
enum class ChargerKind {
    Supercharger,
    Home,
    PublicAc,
    Unknown,
}

data class ChargeLedgerRates(
    val superchargerKrwPerKwh: Float = 385f,
    val homeKrwPerKwh: Float = 120f,
    val publicAcKrwPerKwh: Float = 250f,
)

data class ChargeLedgerEntry(
    val session: ChargeHistoryItem,
    val kind: ChargerKind,
    val rateKrwPerKwh: Float,
    val estimatedCostKrw: Float?,
)

data class ChargeLedgerSummary(
    val periodLabel: String,
    val totalKwh: Float,
    val totalCostKrw: Float,
    val byKind: Map<ChargerKind, Pair<Float, Float>>, // kWh to KRW
)

object ChargeLedgerClassifier {
    fun classify(peakKw: Float?): ChargerKind = when {
        peakKw == null -> ChargerKind.Unknown
        peakKw >= 40f -> ChargerKind.Supercharger
        peakKw >= 11f -> ChargerKind.PublicAc
        else -> ChargerKind.Home
    }

    fun rateFor(kind: ChargerKind, rates: ChargeLedgerRates): Float = when (kind) {
        ChargerKind.Supercharger -> rates.superchargerKrwPerKwh
        ChargerKind.Home -> rates.homeKrwPerKwh
        ChargerKind.PublicAc -> rates.publicAcKrwPerKwh
        ChargerKind.Unknown -> rates.homeKrwPerKwh
    }

    fun toEntry(session: ChargeHistoryItem, rates: ChargeLedgerRates = ChargeLedgerRates()): ChargeLedgerEntry {
        val kind = classify(session.peakKw)
        val rate = rateFor(kind, rates)
        val cost = session.energyKwh?.let { it * rate }
        return ChargeLedgerEntry(session, kind, rate, cost)
    }

    fun summarize(
        sessions: List<ChargeHistoryItem>,
        periodLabel: String,
        rates: ChargeLedgerRates = ChargeLedgerRates(),
    ): ChargeLedgerSummary {
        val entries = sessions.map { toEntry(it, rates) }
        val byKind = mutableMapOf<ChargerKind, Pair<Float, Float>>()
        var totalKwh = 0f
        var totalCost = 0f
        entries.forEach { e ->
            val kwh = e.session.energyKwh ?: 0f
            val cost = e.estimatedCostKrw ?: 0f
            totalKwh += kwh
            totalCost += cost
            val prev = byKind[e.kind] ?: (0f to 0f)
            byKind[e.kind] = (prev.first + kwh) to (prev.second + cost)
        }
        return ChargeLedgerSummary(periodLabel, totalKwh, totalCost, byKind)
    }

    fun kindLabelKo(kind: ChargerKind): String = when (kind) {
        ChargerKind.Supercharger -> "슈퍼차저"
        ChargerKind.Home -> "홈/완속"
        ChargerKind.PublicAc -> "공용 급속"
        ChargerKind.Unknown -> "미분류"
    }
}
