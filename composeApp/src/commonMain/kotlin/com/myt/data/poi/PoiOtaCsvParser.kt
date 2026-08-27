package com.myt.data.poi

import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamera

/**
 * Phase 1.5 — POI OTA CSV 파서.
 *
 * 데이터 파일의 정확한 컬럼 스펙은 배포본마다 다를 수 있어서,
 * 헤더에 포함된 키워드를 기반으로 “가능한 범위”에서 인덱스를 추론합니다.
 */
object PoiOtaCsvParser {

    fun parseSpeedCameras(csvText: String): List<SpeedCamera> {
        val lines = csvText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines.first())
        val headerLower = header.map { it.trim().lowercase() }

        fun idx(vararg keys: String): Int =
            keys
                .asSequence()
                .map { k -> headerLower.indexOfFirst { it.contains(k.lowercase()) } }
                .firstOrNull { it >= 0 }
                ?: -1

        val idIdx = idx("관리번호", "카메라관리번호", "id")
        val latIdx = idx("위도", "latitude", "lat")
        val lngIdx = idx("경도", "longitude", "lng", "lon")
        val speedIdx = idx("제한속도", "속도", "speed")
        val roadNameIdx = idx("도로명", "도로노선", "road")
        val roadDirIdx = idx("도로노선방향", "방향", "direction", "dir")
        val typeIdx = idx("단속구분", "카메라유형", "유형", "type")
        val sectionLenIdx = idx("구간길이", "구간", "section_length", "sectionlen")

        // 필수 컬럼이 없으면 스킵합니다.
        if (latIdx < 0 || lngIdx < 0 || speedIdx < 0) return emptyList()

        val out = ArrayList<SpeedCamera>(maxOf(0, lines.size - 1))
        for (line in lines.drop(1)) {
            val cols = parseCsvLine(line)
            if (cols.isEmpty()) continue

            val lat = cols.getOrNull(latIdx)?.toDoubleOrNull() ?: continue
            val lng = cols.getOrNull(lngIdx)?.toDoubleOrNull() ?: continue
            val speedLimit = cols.getOrNull(speedIdx)?.toIntOrNull() ?: continue

            val id = cols.getOrNull(idIdx)
                ?.takeIf { it.isNotBlank() }
                ?: "cam-${lat}-${lng}-${speedLimit}"

            val roadName = cols.getOrNull(roadNameIdx)?.takeIf { it.isNotBlank() }
            val roadDirection = cols.getOrNull(roadDirIdx)?.toFloatOrNull()

            val cameraTypeRaw = cols.getOrNull(typeIdx)
            val cameraType = parseCameraType(cameraTypeRaw)

            val sectionLengthM = cols.getOrNull(sectionLenIdx)?.toIntOrNull()

            out += SpeedCamera(
                id = id,
                latitude = lat,
                longitude = lng,
                speedLimitKmh = speedLimit,
                roadName = roadName,
                roadDirection = roadDirection,
                cameraType = cameraType,
                sectionLengthM = sectionLengthM,
            )
        }

        return out
    }

    private fun parseCameraType(raw: String?): CameraType {
        val s = raw?.trim().orEmpty()
        if (s.contains("구간시작", ignoreCase = true) || s.contains("SECTION_START", ignoreCase = true)) {
            return CameraType.SECTION_START
        }
        if (s.contains("구간종료", ignoreCase = true) || s.contains("SECTION_END", ignoreCase = true)) {
            return CameraType.SECTION_END
        }
        if (s.contains("신호", ignoreCase = true) || s.contains("SIGNAL", ignoreCase = true)) {
            return CameraType.SIGNAL
        }
        if (s.contains("이동", ignoreCase = true) || s.contains("MOBILE", ignoreCase = true)) {
            return CameraType.MOBILE
        }
        return CameraType.FIXED
    }

    /**
     * 간단 CSV 라인 파서(따옴표 안에 있는 `,`는 분리하지 않음).
     *
     * 멀티라인 quoted field는 지원하지 않습니다.
     */
    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when (c) {
                '"' -> {
                    // CSV escape: "" -> "
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = !inQuotes
                }
                ',' -> {
                    if (inQuotes) {
                        sb.append(c)
                    } else {
                        out += sb.toString().trim()
                        sb.setLength(0)
                    }
                }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString().trim()
        return out
    }
}

