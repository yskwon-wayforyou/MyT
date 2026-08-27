package com.myt.debug

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class DebugLogger(
    private val settings: Settings,
    private val fileSink: PersistentLogSink = NoOpPersistentLogSink,
    private val onErrorIssue: ((tag: String, message: String, throwable: Throwable?) -> Unit)? = null,
    private val clock: Clock = Clock.System,
    private val maxEntries: Int = 2_000,
) {
    private val mutex = Mutex()
    private val buffer = ArrayDeque<LogEntry>()
    private var nextId = 1L

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    var isEnabled: Boolean
        get() = settings.getBoolean(KEY_ENABLED, true)
        set(value) {
            settings.putBoolean(KEY_ENABLED, value)
            if (value) i(TAG, "Debug logging enabled") else i(TAG, "Debug logging disabled")
        }

    fun d(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.Debug, tag, message, throwable)

    fun i(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.Info, tag, message, throwable)

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.Warn, tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.Error, tag, message, throwable)

    suspend fun snapshot(): List<LogEntry> = mutex.withLock { buffer.toList() }

    fun snapshotBlocking(): List<LogEntry> = synchronized(buffer) { buffer.toList() }

    suspend fun clear() {
        mutex.withLock {
            buffer.clear()
            _entries.value = emptyList()
        }
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isEnabled) return
        val entry = LogEntry(
            id = nextId++,
            epochMs = clock.now().toEpochMilliseconds(),
            level = level,
            tag = tag,
            message = LogRedactor.redact(message),
            throwableType = throwable?.let { it::class.simpleName },
            throwableMessage = throwable?.message?.let(LogRedactor::redact),
        )
        synchronized(buffer) {
            buffer.addLast(entry)
            while (buffer.size > maxEntries) buffer.removeFirst()
            _entries.value = buffer.toList()
        }
        // Real-time durable log for WARN/ERROR (crash forensics)
        runCatching { fileSink.append(entry) }
        if (level == LogLevel.Error) {
            runCatching { onErrorIssue?.invoke(tag, message, throwable) }
        }
    }

    companion object {
        const val TAG = "DebugLogger"
        private const val KEY_ENABLED = "debug_log_enabled_v1"
    }
}
