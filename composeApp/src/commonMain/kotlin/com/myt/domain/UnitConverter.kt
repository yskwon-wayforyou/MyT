package com.myt.domain

object UnitConverter {
    const val MPH_TO_KMH = 1.60934f
    const val PSI_TO_BAR = 0.0689476f
    const val MI_TO_KM = 1.60934f

    fun mphToKmh(mph: Float): Float = mph * MPH_TO_KMH
    fun kmhToMph(kmh: Float): Float = kmh / MPH_TO_KMH
    fun psiToBar(psi: Float): Float = psi * PSI_TO_BAR
    fun miToKm(miles: Float): Float = miles * MI_TO_KM

    fun formatSpeed(speedKmh: Float, useKmh: Boolean): String {
        val value = if (useKmh) speedKmh else kmhToMph(speedKmh)
        return value.toInt().toString()
    }

    fun speedUnitLabel(useKmh: Boolean): String = if (useKmh) "km/h" else "mph"
}
