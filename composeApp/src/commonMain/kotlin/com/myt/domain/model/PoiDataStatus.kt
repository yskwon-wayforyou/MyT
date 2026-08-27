package com.myt.domain.model

/**
 * Installed speed-camera POI bundle state vs bundled / online sources.
 */
data class PoiDataStatus(
    val installedCount: Int,
    val bundledVersion: String,
    val bundledCount: Int,
    val lastSyncEpochMs: Long? = null,
    val isDemoSubset: Boolean = false,
    val otaUrlConfigured: Boolean = false,
    /** Local DB matches latest remote fingerprint (after auto-sync). */
    val isLatest: Boolean = false,
    /** Show manual update banner — only when not latest or auto-sync failed. */
    val manualUpdateRequired: Boolean = false,
    val updateReason: String? = null,
    val autoSyncFailed: Boolean = false,
    /** Last sync success/failure detail for More hub (W2 SLA UX). */
    val lastSyncDetail: String? = null,
)
