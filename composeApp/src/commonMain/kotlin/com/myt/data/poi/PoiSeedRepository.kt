package com.myt.data.poi

import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamera
import com.myt.domain.repository.PoiRepository

/** Seeds demo national cameras when DB is empty (M5 bundle fallback). */
class PoiSeedRepository(
    private val sql: SqlPoiRepository,
    private val fallback: PoiRepository,
) : PoiRepository {
    override fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int): List<SpeedCamera> {
        val fromSql = sql.findNearbyCameras(lat, lng, radiusM)
        return if (fromSql.isNotEmpty()) fromSql else fallback.findNearbyCameras(lat, lng, radiusM)
    }

    companion object {
        fun demoCameras(): List<SpeedCamera> = listOf(
            SpeedCamera("cam-su-001", 37.2636, 127.0286, 80, "영통대로", 90f, CameraType.FIXED),
            SpeedCamera("cam-su-002", 37.2851, 127.0532, 60, "광교중앙로", 180f, CameraType.FIXED),
            SpeedCamera("cam-su-003", 37.3210, 127.1088, 80, "수원영동고속도로", 0f, CameraType.SECTION_START, 1800),
            SpeedCamera("cam-demo-001", 37.4985, 127.0280, 80, "테헤란로", 90f, CameraType.FIXED),
        )
    }
}
