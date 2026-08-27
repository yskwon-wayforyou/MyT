package com.myt.debug

import com.myt.platform.AppInfo
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CrashIssueFormatterTest {
    @Test
    fun titleIncludesKindAndType() {
        val title = CrashIssueFormatter.title("crash", "IllegalStateException", "boom")
        assertTrue(title.startsWith("[MyT][crash]"))
        assertContains(title, "IllegalStateException")
    }

    @Test
    fun bodyIncludesSectionsAndRedactsVin() {
        val body = CrashIssueFormatter.body(
            kind = "crash",
            appInfo = AppInfo("0.2.1", "1", "Android 15", "Pixel", "Android"),
            crashReport = "Exception: boom\nVIN LRW3E7FSXSC474472",
            logTail = "WARN Fleet: failed",
            pendingId = "crash-1",
        )
        assertContains(body, "pending_id: `crash-1`")
        assertContains(body, "### Crash / exception")
        assertContains(body, "### Runtime log tail")
        assertContains(body, "VIN…4472")
    }
}
