package com.myt.platform

/**
 * Opens OEM/system battery optimization settings so Phone Key auto-launch
 * is less likely to be killed by Doze (W2 A13).
 */
expect class BatteryOptimizationPlatform(context: Any) {
    /** @return true if an activity was started */
    fun openBatteryOptimizationSettings(): Boolean
}
