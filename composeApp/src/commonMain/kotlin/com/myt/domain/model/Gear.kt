package com.myt.domain.model

enum class Gear {
    PARK,
    REVERSE,
    NEUTRAL,
    DRIVE,
    ;

    fun displayLabel(): String = when (this) {
        PARK -> "P"
        REVERSE -> "R"
        NEUTRAL -> "N"
        DRIVE -> "D"
    }
}
