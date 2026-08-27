package com.myt.domain.simulation

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Geographic initial bearing from (lat1,lng1) → (lat2,lng2), degrees [0, 360). */
fun bearingDegrees(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val dLat = lat2 - lat1
    val dLng = lng2 - lng1
    if (kotlin.math.abs(dLat) < 1e-12 && kotlin.math.abs(dLng) < 1e-12) return 0f
    val φ1 = lat1 * PI / 180.0
    val φ2 = lat2 * PI / 180.0
    val Δλ = (lng2 - lng1) * PI / 180.0
    val y = sin(Δλ) * cos(φ2)
    val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ)
    val deg = atan2(y, x) * 180.0 / PI
    return ((deg + 360.0) % 360.0).toFloat()
}
