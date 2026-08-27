package com.myt.domain.usecase

import com.myt.debug.DebugLogger
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.quota.FleetQuotaPolicy
import com.myt.domain.quota.FleetUsageRepository
import com.myt.domain.quota.PersistedDayUsage
import com.myt.domain.quota.PersistedFleetUsage
import com.myt.domain.quota.PersistedUsageEvent
import com.myt.domain.quota.QuotaDecision
import com.myt.domain.quota.QuotaMode
import com.myt.domain.quota.QuotaSnapshot
import com.myt.domain.quota.UsageEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class FleetQuotaUseCase(
    private val usageRepository: FleetUsageRepository,
    private val historyRepository: com.myt.domain.repository.HistoryRepository,
    private val debugLogger: DebugLogger,
    private val clock: Clock = Clock.System,
) {
    @Volatile
    var appInForeground: Boolean = true

    private val mutex = Mutex()
    private var cached = PersistedFleetUsage()
    private var lastSoftDailyDataAllowMs: Long = 0L
    private var lastDenialReason: String? = null
    private val _snapshot = MutableStateFlow(emptySnapshot())
    val snapshot: StateFlow<QuotaSnapshot> = _snapshot.asStateFlow()

    suspend fun hydrate() {
        mutex.withLock {
            cached = rollMonth(usageRepository.load())
            _snapshot.value = toSnapshot(cached)
        }
    }

    suspend fun evaluate(category: FleetCallCategory): QuotaDecision = mutex.withLock {
        cached = rollMonth(cached)
        val snap = toSnapshot(cached)
        _snapshot.value = snap
        val decision = when {
            snap.mode == QuotaMode.Blocked ->
                QuotaDecision(false, QuotaMode.Blocked, "월 무료 크레딧 95% 도달", 30 * 60_000L)
            category == FleetCallCategory.Data && snap.dataCount >= FleetQuotaPolicy.MONTHLY_DATA ->
                QuotaDecision(false, snap.mode, "이번 달 Data 한도(${FleetQuotaPolicy.MONTHLY_DATA})", 6 * 60 * 60_000L)
            category == FleetCallCategory.Command && snap.commandCount >= FleetQuotaPolicy.MONTHLY_COMMAND ->
                QuotaDecision(false, snap.mode, "이번 달 Command 한도", 6 * 60 * 60_000L)
            category == FleetCallCategory.Wake && snap.wakeCount >= FleetQuotaPolicy.MONTHLY_WAKE ->
                QuotaDecision(false, snap.mode, "이번 달 Wake 한도", 6 * 60 * 60_000L)
            category == FleetCallCategory.Data && snap.dailyDataCount >= FleetQuotaPolicy.DAILY_DATA ->
                softDailyDataDecision(snap)
            category == FleetCallCategory.Wake && snap.dailyWakeCount >= FleetQuotaPolicy.DAILY_WAKE ->
                QuotaDecision(false, QuotaMode.Conserve, "오늘 Wake 한도(${FleetQuotaPolicy.DAILY_WAKE})", 6 * 60 * 60_000L)
            category == FleetCallCategory.Command && day(cached).command >= FleetQuotaPolicy.DAILY_COMMAND ->
                QuotaDecision(false, QuotaMode.Conserve, "오늘 Command 한도", 60 * 60_000L)
            category == FleetCallCategory.Wake && snap.mode == QuotaMode.Conserve ->
                QuotaDecision(false, QuotaMode.Conserve, "절약 모드에서 웨이크 금지", 6 * 60 * 60_000L)
            else -> QuotaDecision(true, snap.mode)
        }
        if (!decision.allowed) {
            lastDenialReason = decision.reason
            _snapshot.value = snap.copy(lastDenialReason = decision.reason)
            debugLogger.w("Quota", "Denied $category: ${decision.reason}")
        } else if (decision.reason != null) {
            debugLogger.i("Quota", "Soft-allow $category: ${decision.reason}")
        }
        decision
    }

    /**
     * Daily Data exhausted → still allow one poll every [SOFT_DAILY_DATA_INTERVAL_MS]
     * so charging Complete / SOC can catch up without waiting until midnight.
     */
    private fun softDailyDataDecision(snap: QuotaSnapshot): QuotaDecision {
        val now = clock.now().toEpochMilliseconds()
        val elapsed = now - lastSoftDailyDataAllowMs
        return if (elapsed >= FleetQuotaPolicy.SOFT_DAILY_DATA_INTERVAL_MS) {
            lastSoftDailyDataAllowMs = now
            QuotaDecision(
                allowed = true,
                mode = QuotaMode.Conserve,
                reason = "오늘 Data soft 허용(${snap.dailyDataCount}/${FleetQuotaPolicy.DAILY_DATA})",
            )
        } else {
            val retry = (FleetQuotaPolicy.SOFT_DAILY_DATA_INTERVAL_MS - elapsed).coerceAtLeast(60_000L)
            QuotaDecision(
                false,
                QuotaMode.Conserve,
                "오늘 Data 한도 ${snap.dailyDataCount}/${FleetQuotaPolicy.DAILY_DATA} · 약 ${(retry / 60_000L).coerceAtLeast(1)}분 후 재시도",
                retry,
            )
        }
    }

    suspend fun record(category: FleetCallCategory, ok: Boolean) {
        mutex.withLock {
            cached = rollMonth(cached)
            val today = todayKey()
            val day = cached.daily[today] ?: PersistedDayUsage()
            val updatedDay = when (category) {
                FleetCallCategory.Data -> day.copy(data = day.data + 1)
                FleetCallCategory.Command -> day.copy(command = day.command + 1)
                FleetCallCategory.Wake -> day.copy(wake = day.wake + 1)
            }
            val event = PersistedUsageEvent(
                atEpochMs = clock.now().toEpochMilliseconds(),
                category = category.name,
                ok = ok,
            )
            cached = cached.copy(
                dataCount = cached.dataCount + if (category == FleetCallCategory.Data) 1 else 0,
                commandCount = cached.commandCount + if (category == FleetCallCategory.Command) 1 else 0,
                wakeCount = cached.wakeCount + if (category == FleetCallCategory.Wake) 1 else 0,
                daily = (cached.daily + (today to updatedDay)).trimDays(),
                recent = (listOf(event) + cached.recent).take(40),
            )
            usageRepository.save(cached)
            _snapshot.value = toSnapshot(cached)
            historyRepository.recordFleetEvent(category, ok)
            debugLogger.d("Quota", "Recorded $category ok=$ok data=${cached.dataCount}")
        }
    }

    fun intervalMultiplier(mode: QuotaMode): Long = if (mode == QuotaMode.Conserve) 2L else 1L

    private fun day(state: PersistedFleetUsage): PersistedDayUsage =
        state.daily[todayKey()] ?: PersistedDayUsage()

    private fun rollMonth(state: PersistedFleetUsage): PersistedFleetUsage {
        val month = monthKey()
        if (state.month == month) return state
        return PersistedFleetUsage(month = month, daily = state.daily.trimDays())
    }

    private fun Map<String, PersistedDayUsage>.trimDays(): Map<String, PersistedDayUsage> {
        val keep = (0..30).map { ago ->
            clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                .minus(DatePeriod(days = ago)).toString()
        }.toSet()
        return filterKeys { it in keep }
    }

    private fun todayKey(): String =
        clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    private fun monthKey(): String {
        val date = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${date.year}-${date.monthNumber.toString().padStart(2, '0')}"
    }

    private fun toSnapshot(state: PersistedFleetUsage): QuotaSnapshot {
        val usd = FleetQuotaPolicy.costUsd(state.dataCount, state.commandCount, state.wakeCount)
        val ratio = (usd / FleetQuotaPolicy.MONTHLY_CREDIT_USD).toFloat().coerceIn(0f, 2f)
        val today = day(state)
        val last7 = (6 downTo 0).map { ago ->
            val key = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                .minus(DatePeriod(days = ago)).toString()
            val d = state.daily[key] ?: PersistedDayUsage()
            key.takeLast(5) to FleetQuotaPolicy.costUsd(d.data, d.command, d.wake)
        }
        return QuotaSnapshot(
            month = state.month.ifBlank { monthKey() },
            dataCount = state.dataCount,
            commandCount = state.commandCount,
            wakeCount = state.wakeCount,
            dataLimit = FleetQuotaPolicy.MONTHLY_DATA,
            commandLimit = FleetQuotaPolicy.MONTHLY_COMMAND,
            wakeLimit = FleetQuotaPolicy.MONTHLY_WAKE,
            dailyDataCount = today.data,
            dailyWakeCount = today.wake,
            dailyDataLimit = FleetQuotaPolicy.DAILY_DATA,
            dailyWakeLimit = FleetQuotaPolicy.DAILY_WAKE,
            estimatedUsd = usd,
            creditUsd = FleetQuotaPolicy.MONTHLY_CREDIT_USD,
            usedRatio = ratio,
            mode = FleetQuotaPolicy.mode(ratio),
            last7DaysUsd = last7,
            recent = state.recent.map {
                UsageEvent(
                    atEpochMs = it.atEpochMs,
                    category = runCatching { FleetCallCategory.valueOf(it.category) }
                        .getOrDefault(FleetCallCategory.Data),
                    ok = it.ok,
                )
            },
            lastDenialReason = lastDenialReason,
        )
    }

    private fun emptySnapshot(): QuotaSnapshot = toSnapshot(PersistedFleetUsage(month = monthKey()))
}

class QuotaExceededException(message: String) : IllegalStateException(message)
