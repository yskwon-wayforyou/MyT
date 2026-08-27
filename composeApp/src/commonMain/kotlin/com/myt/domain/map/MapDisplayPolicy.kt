package com.myt.domain.map

import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear

/**
 * When to project the vehicle onto the road network for map display.
 * Parked / Neutral / slow indoor crawl / simulation keep raw coordinates.
 */
object MapDisplayPolicy {
    fun shouldApplyRoadSnap(state: GaugeState): Boolean {
        if (state.isSimulated) return false
        if (state.gear == Gear.PARK || state.gear == Gear.NEUTRAL) return false
        if (state.speedKmh < 5f) return false
        return state.gear == Gear.DRIVE || state.gear == Gear.REVERSE
    }
}
