package com.myt.data.poi

import com.myt.domain.model.SpeedCamera
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight grid spatial index (M5 R-Tree substitute for mobile POI sets).
 *
 * Cells are ~1.1 km at equator; suitable for tens of thousands of cameras.
 */
class GridSpatialIndex(
    private val cellSizeDegrees: Double = 0.01,
) {
    private val cells = LinkedHashMap<Long, MutableList<SpeedCamera>>()

    fun rebuild(cameras: List<SpeedCamera>) {
        cells.clear()
        cameras.forEach { cam ->
            cellKey(cam.latitude, cam.longitude).let { key ->
                cells.getOrPut(key) { ArrayList(4) }.add(cam)
            }
        }
    }

    fun query(lat: Double, lng: Double, radiusM: Int): List<SpeedCamera> {
        if (radiusM <= 0 || cells.isEmpty()) return emptyList()

        val latDelta = radiusM / 111_000.0
        val cosLat = cos(lat * PI / 180.0).takeIf { it != 0.0 } ?: 1.0
        val lngDelta = radiusM / (111_000.0 * cosLat)

        val minLat = lat - latDelta
        val maxLat = lat + latDelta
        val minLng = lng - lngDelta
        val maxLng = lng + lngDelta

        val minCellLat = floorCell(minLat)
        val maxCellLat = floorCell(maxLat)
        val minCellLng = floorCell(minLng)
        val maxCellLng = floorCell(maxLng)

        val out = ArrayList<SpeedCamera>()
        var cellLat = minCellLat
        while (cellLat <= maxCellLat) {
            var cellLng = minCellLng
            while (cellLng <= maxCellLng) {
                cells[pack(cellLat, cellLng)]?.forEach { cam ->
                    if (haversineMeters(lat, lng, cam.latitude, cam.longitude) <= radiusM) {
                        out += cam
                    }
                }
                cellLng++
            }
            cellLat++
        }
        return out
    }

    private fun floorCell(value: Double): Long = kotlin.math.floor(value / cellSizeDegrees).toLong()

    private fun cellKey(lat: Double, lng: Double): Long = pack(floorCell(lat), floorCell(lng))

    private fun pack(latCell: Long, lngCell: Long): Long = (latCell shl 32) xor (lngCell and 0xFFFF_FFFFL)

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLng = (lng2 - lng1) * PI / 180.0
        val a = sin(dLat / 2).let { it * it } +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLng / 2).let { it * it }
        return 6_371_000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
