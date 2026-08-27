package com.myt.platform

/**
 * Thin bridge for iOS speech recognition.
 * Implemented in Kotlin/Native without full SFSpeechRecognizer wiring when Xcode
 * Speech APIs are unavailable at compile-time; ContentView can later call into
 * a Swift helper via this same API surface.
 */
internal object IosSpeechBridge {
    fun recognize(locale: String): String {
        // Placeholder until Xcode license + Speech entitlement allow full SFSpeechRecognizer.
        // Returning empty triggers a user-visible failure instead of a fake destination.
        throw IllegalStateException(
            "iOS 음성 인식은 Xcode에서 Speech 권한을 활성화한 뒤 사용할 수 있습니다 (locale=$locale)",
        )
    }
}
