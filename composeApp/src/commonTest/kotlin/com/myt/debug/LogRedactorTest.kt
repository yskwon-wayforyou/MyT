package com.myt.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogRedactorTest {
    @Test
    fun redactsBearerToken() {
        val input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.abc.def"
        val out = LogRedactor.redact(input)
        assertFalse(out.contains("eyJhbGci"))
        assertTrue(out.contains("[REDACTED]") || out.contains("[JWT_REDACTED]"))
    }

    @Test
    fun masksVin() {
        val vin = "5YJ3E1EA1KF123456"
        val out = LogRedactor.redact("vehicle $vin")
        assertTrue(out.contains("3456"))
        assertFalse(out.contains(vin))
    }

    @Test
    fun redactsSecretKeys() {
        val out = LogRedactor.redact("client_secret=super-secret-value")
        assertEquals("client_secret=[REDACTED]", out)
    }
}
