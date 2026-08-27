package com.myt.domain.quota

enum class FleetCallCategory {
    Data,
    Command,
    Wake,
}

enum class QuotaMode {
    Normal,
    Conserve,
    Blocked,
}

data class QuotaDecision(
    val allowed: Boolean,
    val mode: QuotaMode,
    val reason: String? = null,
    val retryAfterMs: Long = 60_000L,
)

data class QuotaSnapshot(
    val month: String,
    val dataCount: Int,
    val commandCount: Int,
    val wakeCount: Int,
    val dataLimit: Int,
    val commandLimit: Int,
    val wakeLimit: Int,
    val dailyDataCount: Int,
    val dailyWakeCount: Int,
    val dailyDataLimit: Int,
    val dailyWakeLimit: Int,
    val estimatedUsd: Double,
    val creditUsd: Double,
    val usedRatio: Float,
    val mode: QuotaMode,
    val last7DaysUsd: List<Pair<String, Double>>,
    val recent: List<UsageEvent>,
    /** Last deny reason for banner copy (daily vs monthly). */
    val lastDenialReason: String? = null,
)

data class UsageEvent(
    val atEpochMs: Long,
    val category: FleetCallCategory,
    val ok: Boolean,
)

object FleetQuotaPolicy {
    const val MONTHLY_CREDIT_USD = 10.0
    const val CONSERVE_RATIO = 0.70f
    const val BLOCK_RATIO = 0.95f

    const val DATA_PER_USD = 500
    const val COMMAND_PER_USD = 1_000
    const val WAKE_PER_USD = 50

    const val MONTHLY_DATA = 3_000
    const val MONTHLY_COMMAND = 200
    const val MONTHLY_WAKE = 50
    /**
     * App-side daily guards (Tesla has no published daily Data=80).
     * Prior 80 was too tight for charging near-limit (45s) + UI refresh + regression
     * and falsely showed 「한도 보호」 while monthly $ credit was still low.
     */
    const val DAILY_DATA = 300
    const val DAILY_WAKE = 8
    const val DAILY_COMMAND = 30
    /** When daily Data is exhausted, still allow one poll this often (charging completion). */
    const val SOFT_DAILY_DATA_INTERVAL_MS = 15 * 60_000L

    const val DRIVING_INTERVAL_MS = 60_000L
    const val PARKED_INTERVAL_MS = 300_000L
    const val CHARGING_INTERVAL_MS = 180_000L
    const val BACKGROUND_IDLE_MS = 900_000L

    fun costUsd(data: Int, command: Int, wake: Int): Double =
        data / DATA_PER_USD.toDouble() +
            command / COMMAND_PER_USD.toDouble() +
            wake / WAKE_PER_USD.toDouble()

    fun mode(usedRatio: Float): QuotaMode = when {
        usedRatio >= BLOCK_RATIO -> QuotaMode.Blocked
        usedRatio >= CONSERVE_RATIO -> QuotaMode.Conserve
        else -> QuotaMode.Normal
    }
}

fun emptyQuotaSnapshot(): QuotaSnapshot = QuotaSnapshot(
    month = "",
    dataCount = 0,
    commandCount = 0,
    wakeCount = 0,
    dataLimit = FleetQuotaPolicy.MONTHLY_DATA,
    commandLimit = FleetQuotaPolicy.MONTHLY_COMMAND,
    wakeLimit = FleetQuotaPolicy.MONTHLY_WAKE,
    dailyDataCount = 0,
    dailyWakeCount = 0,
    dailyDataLimit = FleetQuotaPolicy.DAILY_DATA,
    dailyWakeLimit = FleetQuotaPolicy.DAILY_WAKE,
    estimatedUsd = 0.0,
    creditUsd = FleetQuotaPolicy.MONTHLY_CREDIT_USD,
    usedRatio = 0f,
    mode = QuotaMode.Normal,
    last7DaysUsd = emptyList(),
    recent = emptyList(),
)
