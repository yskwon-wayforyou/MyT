package com.myt.platform

import android.content.Context
import android.view.WindowManager

actual class ScreenPlatform actual constructor(context: Any) {
    private val appContext = (context as Context).applicationContext

    actual fun keepScreenOn(enable: Boolean) {
        // Applied via Activity window flags in a future iteration
    }

    actual fun currentWidthDp(): Int {
        val metrics = appContext.resources.displayMetrics
        return (metrics.widthPixels / metrics.density).toInt()
    }

    actual fun currentHeightDp(): Int {
        val metrics = appContext.resources.displayMetrics
        return (metrics.heightPixels / metrics.density).toInt()
    }
}
