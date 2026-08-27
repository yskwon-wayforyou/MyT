package com.myt.domain.geo

import kotlin.math.roundToInt

/**
 * Google Maps encoded polyline codec (precision=1e-5).
 *
 * Used for offline route visualization (Phase 1.5).
 */
object PolylineCodec {
    data class LatLng(val lat: Double, val lng: Double)

    fun encode(points: List<LatLng>): String {
        if (points.isEmpty()) return ""
        val sb = StringBuilder()
        var lastLat = 0
        var lastLng = 0

        points.forEach { p ->
            val lat = (p.lat * 1e5).roundToInt()
            val lng = (p.lng * 1e5).roundToInt()
            val dLat = lat - lastLat
            val dLng = lng - lastLng

            encodeSigned(dLat, sb)
            encodeSigned(dLng, sb)

            lastLat = lat
            lastLng = lng
        }
        return sb.toString()
    }

    fun decode(encoded: String): List<LatLng> {
        if (encoded.isBlank()) return emptyList()
        val res = ArrayList<LatLng>()

        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            val dLat = decodeSigned(encoded, index).also { index = it.second }
            val deltaLat = dLat.first
            lat += deltaLat

            val dLng = decodeSigned(encoded, index).also { index = it.second }
            val deltaLng = dLng.first
            lng += deltaLng

            res += LatLng(lat / 1e5, lng / 1e5)
        }
        return res
    }

    private fun encodeSigned(value: Int, sb: StringBuilder) {
        var v = value
        v = if (v < 0) (v shl 1).inv() else (v shl 1)
        while (v >= 0x20) {
            val ch = (0x20 or (v and 0x1f)) + 63
            sb.append(ch.toChar())
            v = v shr 5
        }
        sb.append((v + 63).toChar())
    }

    /**
     * @return Pair(delta, newIndex)
     */
    private fun decodeSigned(encoded: String, startIndex: Int): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var index = startIndex
        while (true) {
            if (index >= encoded.length) break
            val b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
            if (b < 0x20) break
        }
        val delta = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        return delta to index
    }
}

