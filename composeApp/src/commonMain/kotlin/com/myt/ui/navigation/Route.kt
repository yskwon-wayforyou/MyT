package com.myt.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Gauge : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object VoiceNav : Route

    /** History hub — driving, charging, Fleet API */
    @Serializable
    data object History : Route

    /** @deprecated Use [History] */
    @Serializable
    data object TripHistory : Route

    /** Debug log viewer + Gmail export */
    @Serializable
    data object DebugLogs : Route

    /** Phase 3 — analytics, carbon badge, export. */
    @Serializable
    data object Analytics : Route

    /** Phase 2 — subscription / watch / widget demos. */
    @Serializable
    data object Commercial : Route

    /** Phase 1.5 — full-screen trip route (M21/M22). */
    @Serializable
    data class TripRoute(val tripId: String) : Route
}
