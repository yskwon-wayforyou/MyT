package com.myt.domain.usecase

/**
 * UI-triggered freshness requests. Throttled so pane switches do not burn Fleet quota.
 */
enum class UiFreshNeed {
    /** Map / charging map needs coordinates. */
    Location,
    /** Tire pane needs TPMS. */
    Tires,
    /** Status panels that need a general live snapshot. */
    Status,
}
