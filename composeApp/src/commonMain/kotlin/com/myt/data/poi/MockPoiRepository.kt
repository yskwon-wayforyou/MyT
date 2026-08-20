package com.myt.data.poi

import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamera
import com.myt.domain.repository.PoiRepository
import kotlin.math.abs

class MockPoiRepository : PoiRepository {
    private val demoCameras = listOf(
        SpeedCamera(
            id = "cam-001",
            latitude = 37.4985,
            longitude = 127.0280,
            speedLimitKmh = 80,
            roadName = "테헤란로",
            roadDirection = 90f,
            cameraType = CameraType.FIXED,
        ),
        SpeedCamera(
            id = "section-start-001",
            latitude = 37.5000,
            longitude = 127.0300,
            speedLimitKmh = 80,
            roadName = "강남대로",
            roadDirection = 180f,
            cameraType = CameraType.SECTION_START,
            sectionLengthM = 2_000,
        ),
    )

    override fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int): List<SpeedCamera> {
        return demoCameras.filter { camera ->
            approximateDistanceM(lat, lng, camera.latitude, camera.longitude) <= radiusM
        }
    }

    private fun approximateDistanceM(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val dLat = abs(lat2 - lat1) * 111_000
        val dLng = abs(lng2 - lng1) * 88_000
        return kotlin.math.sqrt(dLat * dLat + dLng * dLng)
    }
}
