package com.myt.data.poi

import com.myt.domain.model.CameraType
import kotlin.test.Test
import kotlin.test.assertEquals

class PoiOtaCsvParserTest {
    @Test
    fun parsesFixedAndSectionStart() {
        val csv = """
            무인교통단속카메라관리번호,위도,경도,도로노선방향,제한속도,도로명,단속구분,구간길이
            cam-001,37.4985,127.0280,90,80,테헤란로,고정식,
            cam-002,37.5000,127.0300,180,80,강남대로,구간시작,2000
        """.trimIndent()

        val cameras = PoiOtaCsvParser.parseSpeedCameras(csv)

        assertEquals(2, cameras.size)

        val fixed = cameras[0]
        assertEquals("cam-001", fixed.id)
        assertEquals(CameraType.FIXED, fixed.cameraType)
        assertEquals(80, fixed.speedLimitKmh)
        assertEquals(90f, fixed.roadDirection)
        assertEquals("테헤란로", fixed.roadName)
        assertEquals(null, fixed.sectionLengthM)

        val sectionStart = cameras[1]
        assertEquals("cam-002", sectionStart.id)
        assertEquals(CameraType.SECTION_START, sectionStart.cameraType)
        assertEquals(2000, sectionStart.sectionLengthM)
    }
}

