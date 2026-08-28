package com.myt.domain

import com.myt.domain.model.SpeedCamera
import com.myt.domain.simulation.bearingDegrees
import kotlin.math.abs

/**
 * Forward-cone speed camera matching — rejects opposite-lane and behind-vehicle cameras.
 * Target: ≥99% validity on synthetic + recorded drive fixtures.
 */
object SpeedCamMatcher {
    /** Max angle between vehicle heading and bearing-to-camera (forward cone). */
    const val FORWARD_CONE_DEG = 35f

    /** Max angle between vehicle heading and camera road direction (same lane). */
    const val ROAD_ALIGN_DEG = 30f

    /** Min distance to consider (ignore GPS jitter at same spot). */
    const val MIN_DISTANCE_M = 8.0

    fun bearingToCamera(
        vehicleLat: Double,
        vehicleLng: Double,
        camera: SpeedCamera,
    ): Float = bearingDegrees(vehicleLat, vehicleLng, camera.latitude, camera.longitude)

    /**
     * Returns true when [camera] is ahead on the driving lane relative to [heading].
     */
    fun isAheadOnRoute(
        vehicleLat: Double,
        vehicleLng: Double,
        heading: Float,
        camera: SpeedCamera,
        distanceM: Double,
    ): Boolean {
        if (distanceM < MIN_DISTANCE_M) return false
        val bearing = bearingToCamera(vehicleLat, vehicleLng, camera)
        if (angleDifference(bearing, heading) > FORWARD_CONE_DEG) return false
        val roadDir = camera.roadDirection ?: return true
        // Same travel direction as the camera lane (reject opposite-flow cameras).
        if (angleDifference(roadDir, heading) > ROAD_ALIGN_DEG) return false
        // Camera should face/oncoming traffic from our direction (road dir ≈ bearing to cam).
        if (angleDifference(roadDir, bearing) > ROAD_ALIGN_DEG + 15f) return false
        return true
    }

    fun angleDifference(a: Float, b: Float): Float {
        val diff = ((a - b + 540f) % 360f) - 180f
        return abs(diff)
    }
}
