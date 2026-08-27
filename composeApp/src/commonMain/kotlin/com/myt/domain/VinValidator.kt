package com.myt.domain

/** ISO 3779 VIN format check (17 chars, no I/O/Q). Check digit not validated. */
object VinValidator {
    private val pattern = Regex("""^[A-HJ-NPR-Z0-9]{17}$""")

    fun normalize(raw: String): String = raw.trim().uppercase()

    fun isValid(raw: String): Boolean = pattern.matches(normalize(raw))

    fun validationMessage(raw: String): String? = when {
        raw.isBlank() -> "VIN을 입력해 주세요"
        raw.length < 17 -> "VIN은 17자리입니다"
        !isValid(raw) -> "VIN 형식이 올바르지 않습니다 (I, O, Q 제외)"
        else -> null
    }
}
