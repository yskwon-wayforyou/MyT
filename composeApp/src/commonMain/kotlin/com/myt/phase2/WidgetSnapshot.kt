package com.myt.phase2

/**
 * M36 — home screen widget / Live Activity placeholders.
 * Platform-specific AppWidget / ActivityKit arrive with store packaging.
 */
data class WidgetSnapshot(
    val socPercent: Int,
    val rangeKm: Int,
    val locked: Boolean?,
    val updatedAtMs: Long,
)

interface WidgetSnapshotProvider {
    fun current(): WidgetSnapshot?
}

class GaugeWidgetSnapshotProvider(
    private val soc: () -> Float,
    private val rangeKm: () -> Float,
    private val locked: () -> Boolean?,
    private val clockMs: () -> Long,
) : WidgetSnapshotProvider {
    override fun current(): WidgetSnapshot = WidgetSnapshot(
        socPercent = soc().toInt(),
        rangeKm = rangeKm().toInt(),
        locked = locked(),
        updatedAtMs = clockMs(),
    )
}
