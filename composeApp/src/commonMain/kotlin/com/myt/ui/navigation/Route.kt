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

    /** Phase 1.5 — M22 */
    @Serializable
    data object TripHistory : Route

    /** Phase 2 — subscription (M37) */
    @Serializable
    data object Subscription : Route
}
