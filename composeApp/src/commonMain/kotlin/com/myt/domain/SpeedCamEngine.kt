package com.myt.domain

import com.myt.domain.model.AlertLevel
import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamAlert
import com.myt.domain.model.SpeedCamera
import com.myt.domain.repository.PoiRepository
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SpeedCamEngine(
    private val poiRepository: PoiRepository,
) {
    private var activeSectionId: String? = null
    private val sectionSpeedSamples = mutableListOf<Float>()

    fun evaluate(
        lat: Double,
        lng: Double,
        heading: Float,
        speedKmh: Float,
    ): SpeedCamAlert? {
        val nearby = poiRepository.findNearbyCameras(lat, lng, QUERY_RADIUS_M)
        if (nearby.isEmpty()) {
            resetSectionTracking()
            return null
        }

        val candidates = filterByDirection(nearby, heading)
            .map { camera -> camera to haversineDistanceM(lat, lng, camera.latitude, camera.longitude) }
            .filter { (_, distance) -> distance <= QUERY_RADIUS_M }
            .sortedBy { (_, distance) -> distance }

        val (camera, distance) = candidates.firstOrNull() ?: run {
            resetSectionTracking()
            return null
        }

        if (camera.cameraType == CameraType.SECTION_START) {
            activeSectionId = camera.id
            sectionSpeedSamples.clear()
        }

        if (camera.cameraType == CameraType.SECTION_END && activeSectionId != null) {
            sectionSpeedSamples.add(speedKmh)
            val average = sectionSpeedSamples.average().toFloat()
            val level = if (average > camera.speedLimitKmh) AlertLevel.L3 else AlertLevel.SECTION
            resetSectionTracking()
            return SpeedCamAlert(
                camera = camera,
                distanceM = distance.toInt(),
                currentSpeedKmh = speedKmh,
                level = level,
                message = "구간 ${average.toInt()}/${camera.speedLimitKmh} avg",
                sectionAverageKmh = average,
            )
        }

        if (activeSectionId != null) {
            sectionSpeedSamples.add(speedKmh)
            val average = sectionSpeedSamples.average().toFloat()
            return SpeedCamAlert(
                camera = camera,
                distanceM = distance.toInt(),
                currentSpeedKmh = speedKmh,
                level = AlertLevel.SECTION,
                message = "구간 ${average.toInt()}/${camera.speedLimitKmh} avg",
                sectionAverageKmh = average,
            )
        }

        val level = calculateAlertLevel(camera, distance, speedKmh) ?: return null

        return SpeedCamAlert(
            camera = camera,
            distanceM = distance.toInt(),
            currentSpeedKmh = speedKmh,
            level = level,
            message = buildMessage(level, distance.toInt(), camera.speedLimitKmh, speedKmh),
        )
    }

    fun resetSectionTracking() {
        activeSectionId = null
        sectionSpeedSamples.clear()
    }

    internal fun calculateAlertLevel(
        camera: SpeedCamera,
        distanceM: Double,
        speedKmh: Float,
    ): AlertLevel? = when {
        distanceM > 500 -> null
        distanceM > 300 -> AlertLevel.L1
        distanceM > 100 -> AlertLevel.L2
        speedKmh > camera.speedLimitKmh -> AlertLevel.L3
        else -> AlertLevel.L2
    }

    internal fun filterByDirection(
        cameras: List<SpeedCamera>,
        heading: Float,
    ): List<SpeedCamera> = cameras.filter { camera ->
        val direction = camera.roadDirection ?: return@filter true
        val diff = angleDifference(direction, heading)
        diff <= DIRECTION_TOLERANCE_DEG
    }

    private fun buildMessage(
        level: AlertLevel,
        distanceM: Int,
        limitKmh: Int,
        speedKmh: Float,
    ): String = when (level) {
        AlertLevel.L1 -> "${distanceM}m 전방 ${limitKmh}km/h"
        AlertLevel.L2 -> "${distanceM}m! ${limitKmh}km/h"
        AlertLevel.L3 -> "과속! ${speedKmh.toInt()}/${limitKmh}"
        AlertLevel.SECTION -> "구간단속 ${limitKmh}km/h"
    }

    private fun haversineDistanceM(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun angleDifference(a: Float, b: Float): Float {
        val diff = ((a - b + 540) % 360) - 180
        return kotlin.math.abs(diff)
    }

    companion object {
        const val QUERY_RADIUS_M = 500
        const val DIRECTION_TOLERANCE_DEG = 45f
    }
}
