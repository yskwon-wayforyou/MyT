package com.myt.debug

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * JVM-side simulation of the Android export file write path.
 */
class DebugLogExportFileTest {
    @Test
    fun writesUtf8LogFileLikeAndroidExporter() {
        val dir = File.createTempFile("myt-debug-test", "").apply {
            delete()
            mkdirs()
        }
        val file = File(dir, DebugLogExporter.defaultFileName())
        val content = "MyT Debug Report\n--- LOG ---\nline"
        file.writeText(content, Charsets.UTF_8)
        assertTrue(file.exists())
        assertTrue(file.readText().contains("MyT Debug Report"))
        file.delete()
        dir.delete()
    }
}
