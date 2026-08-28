package com.myt.platform

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener

/**
 * App-controlled rotation: portrait = 0°, landscape = 270° (reverse landscape).
 * Ignores system auto-rotate setting.
 */
class OrientationController(private val activity: Activity) {
    private val listener = object : OrientationEventListener(activity) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            val target = when {
                // Portrait band (0°) — include upside-down as portrait per nav-app convention.
                orientation in 315..360 || orientation in 0..45 || orientation in 135..225 ->
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                // Landscape band → lock reverse landscape (270°).
                else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            if (activity.requestedOrientation != target) {
                activity.requestedOrientation = target
            }
        }
    }

    fun start() {
        if (listener.canDetectOrientation()) {
            listener.enable()
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun stop() {
        listener.disable()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
