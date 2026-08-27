package com.myt.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VinValidatorTest {
    @Test
    fun acceptsValidVin() {
        assertTrue(VinValidator.isValid("5YJ3E1EA1KF123456"))
    }

    @Test
    fun rejectsShortVin() {
        assertFalse(VinValidator.isValid("5YJ3E1EA1KF"))
        assertEquals("VIN은 17자리입니다", VinValidator.validationMessage("5YJ3E1EA1KF"))
    }

    @Test
    fun rejectsInvalidCharacters() {
        assertFalse(VinValidator.isValid("5YJ3E1EA1KF12345I"))
        assertEquals("VIN 형식이 올바르지 않습니다 (I, O, Q 제외)", VinValidator.validationMessage("5YJ3E1EA1KF12345I"))
    }

    @Test
    fun normalizeTrimsAndUppercases() {
        assertEquals("5YJ3E1EA1KF123456", VinValidator.normalize("  5yj3e1ea1kf123456 "))
    }

    @Test
    fun blankVinMessage() {
        assertNull(VinValidator.validationMessage("5YJ3E1EA1KF123456"))
        assertEquals("VIN을 입력해 주세요", VinValidator.validationMessage(""))
    }
}
