package com.myt.domain.model

import kotlinx.serialization.Serializable

enum class TelemetrySource {
    None,
    Device,
    Degraded,
    Fleet,
    Cache,
}

@Serializable
enum class DriveDensity {
    Minimal,
    Standard,
    Pro,
}

@Serializable
enum class PressureUnit {
    Psi,
    Bar,
}
