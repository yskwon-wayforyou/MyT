package com.myt.domain.voice

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SpeechErrorMessagesTest {
    @Test
    fun humanize_error7_suggestsShortRetry() {
        val msg = SpeechErrorMessages.humanize("SpeechRecognizer error 7")
        assertContains(msg, "인식하지 못")
        assertContains(msg, "짧게")
    }

    @Test
    fun humanize_permission_mentionsMic() {
        val msg = SpeechErrorMessages.humanize("ERROR_INSUFFICIENT_PERMISSIONS (9)")
        assertContains(msg, "마이크")
    }

    @Test
    fun humanize_blank_usesFallback() {
        assertEquals(
            "음성 인식에 실패했습니다. 다시 시도해 주세요.",
            SpeechErrorMessages.humanize(null),
        )
    }

    @Test
    fun humanize_unknown_keepsOriginal() {
        val raw = "unexpected STT failure xyz"
        assertEquals(raw, SpeechErrorMessages.humanize(raw))
        assertFalse(SpeechErrorMessages.humanize("timeout").contains("xyz"))
    }
}
