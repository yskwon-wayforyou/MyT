package com.myt.debug

/**
 * Real-time persistent log sink for WARN/ERROR (and crash dumps).
 * Survives process death so we can inspect / auto-file GitHub issues after restart.
 */
interface PersistentLogSink {
    fun append(entry: LogEntry)
    fun appendRaw(line: String)
    fun flush()
    /** Absolute or platform path of the current runtime log, if available. */
    fun currentLogFilePath(): String?
    fun readTail(maxChars: Int = 48_000): String
    fun listLogFiles(): List<String>
}

object NoOpPersistentLogSink : PersistentLogSink {
    override fun append(entry: LogEntry) = Unit
    override fun appendRaw(line: String) = Unit
    override fun flush() = Unit
    override fun currentLogFilePath(): String? = null
    override fun readTail(maxChars: Int): String = ""
    override fun listLogFiles(): List<String> = emptyList()
}
