package com.myt.domain

object UnitConverter {
    const val MPH_TO_KMH = 1.60934f
    const val PSI_TO_BAR = 0.0689476f
    const val MI_TO_KM = 1.60934f

    fun mphToKmh(mph: Float): Float = mph * MPH_TO_KMH
    fun kmhToMph(kmh: Float): Float = kmh / MPH_TO_KMH
    fun psiToBar(psi: Float): Float = psi * PSI_TO_BAR
    fun barToPsi(bar: Float): Float = bar / PSI_TO_BAR
    fun miToKm(miles: Float): Float = miles * MI_TO_KM

    fun formatSpeed(speedKmh: Float, useKmh: Boolean): String {
        val value = if (useKmh) speedKmh else kmhToMph(speedKmh)
        return value.toInt().toString()
    }

    fun speedUnitLabel(useKmh: Boolean): String = if (useKmh) "km/h" else "mph"

    fun formatPressure(bar: Float, usePsi: Boolean): String =
        if (usePsi) barToPsi(bar).toInt().toString() else "%.1f".format(bar)

    fun pressureUnitLabel(usePsi: Boolean): String = if (usePsi) "psi" else "bar"

    fun pressureWarn(bar: Float?): Boolean {
        if (bar == null) return false
        return bar < 2.2f || bar > 3.2f
    }
}
