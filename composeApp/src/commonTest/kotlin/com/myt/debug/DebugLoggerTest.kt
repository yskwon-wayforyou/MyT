package com.myt.debug

import com.myt.platform.AppInfo
import com.myt.test.TestSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugLoggerTest {
    @Test
    fun storesAndClearsEntries() = runBlocking {
        val logger = DebugLogger(TestSettings())
        logger.i("Test", "hello")
        logger.e("Test", "boom", IllegalStateException("x"))
        assertEquals(2, logger.snapshot().size)
        logger.clear()
        assertEquals(0, logger.snapshot().size)
    }

    @Test
    fun respectsDisabledFlag() = runBlocking {
        val logger = DebugLogger(TestSettings())
        logger.isEnabled = false
        logger.i("Test", "hidden")
        assertEquals(0, logger.snapshot().size)
    }

    @Test
    fun exportReportContainsHeader() {
        val logger = DebugLogger(TestSettings())
        logger.i("Fleet", "fetch ok")
        val report = DebugLogExporter.buildReport(
            entries = logger.snapshotBlocking(),
            appInfo = AppInfo("0.1.0", "1", "Test OS", "Test Device", "Test"),
        )
        assertTrue(report.contains("MyT Debug Report"))
        assertTrue(report.contains("[INFO] Fleet"))
    }
}
