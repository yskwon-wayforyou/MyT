package com.myt.data.poi

import com.myt.data.local.MyTDatabase
import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamera
import com.myt.domain.repository.PoiRepository

/**
 * POI 카메라 DB + in-memory grid spatial index (M5).
 */
class SqlPoiRepository(
    private val db: MyTDatabase,
) : PoiRepository {
    private val spatialIndex = GridSpatialIndex()
    private var indexReady = false

    fun seedIfEmpty(cameras: List<SpeedCamera>) {
        val count = db.myTDatabaseQueries.countSpeedCameras().executeAsOne()
        if (count > 0L) return
        cameras.forEach { upsert(it) }
        invalidateIndex()
    }

    fun cameraCount(): Long = db.myTDatabaseQueries.countSpeedCameras().executeAsOne()

    override fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int): List<SpeedCamera> {
        if (radiusM <= 0) return emptyList()
        ensureIndex()
        return spatialIndex.query(lat, lng, radiusM)
    }

    fun upsert(camera: SpeedCamera) {
        upsertSilently(camera)
        invalidateIndex()
    }

    fun bulkUpsert(cameras: List<SpeedCamera>) {
        cameras.forEach { upsertSilently(it) }
        invalidateIndex()
    }

    /** Rebuilds spatial index immediately after OTA sync so alerts apply without restart. */
    fun refreshSpatialIndex() {
        invalidateIndex()
        ensureIndex()
    }

    private fun upsertSilently(camera: SpeedCamera) {
        db.myTDatabaseQueries.upsertSpeedCamera(
            id = camera.id,
            latitude = camera.latitude,
            longitude = camera.longitude,
            road_name = camera.roadName,
            road_direction = camera.roadDirection?.toDouble(),
            speed_limit = camera.speedLimitKmh.toLong(),
            camera_type = camera.cameraType.name,
            section_length = camera.sectionLengthM?.toLong(),
        )
    }

    private fun ensureIndex() {
        if (indexReady) return
        val cameras = db.myTDatabaseQueries.selectAllSpeedCameras().executeAsList().map { r ->
            SpeedCamera(
                id = r.id,
                latitude = r.latitude,
                longitude = r.longitude,
                speedLimitKmh = r.speed_limit.toInt(),
                roadName = r.road_name,
                roadDirection = r.road_direction?.toFloat(),
                cameraType = runCatching { CameraType.valueOf(r.camera_type) }.getOrDefault(CameraType.FIXED),
                sectionLengthM = r.section_length?.toInt(),
            )
        }
        spatialIndex.rebuild(cameras)
        indexReady = true
    }

    private fun invalidateIndex() {
        indexReady = false
    }
}

class HybridPoiRepository(
    private val sql: PoiRepository,
    private val fallback: PoiRepository,
) : PoiRepository {
    override fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int): List<SpeedCamera> {
        val fromSql = sql.findNearbyCameras(lat, lng, radiusM)
        return if (fromSql.isNotEmpty()) fromSql else fallback.findNearbyCameras(lat, lng, radiusM)
    }
}
