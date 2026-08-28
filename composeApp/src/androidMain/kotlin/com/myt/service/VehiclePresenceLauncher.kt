package com.myt.service

import android.content.Context
import android.content.Intent
import com.myt.MainActivity
import com.myt.navigation.MyTRoutes

/**
 * Brings MyT to the foreground when Tesla Phone Key / vehicle BT presence is detected.
 */
object VehiclePresenceLauncher {
    private const val PREFS = "myt_presence_launch"
    private const val KEY_LAST_LAUNCH_MS = "last_launch_ms"
    private const val DEBOUNCE_MS = 8_000L

    const val EXTRA_ROUTE = "myt.route"

    fun onVehiclePresent(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_LAUNCH_MS, 0L)
        if (now - last < DEBOUNCE_MS) return
        prefs.edit().putLong(KEY_LAST_LAUNCH_MS, now).apply()

        val intent = Intent(app, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
            putExtra(EXTRA_ROUTE, MyTRoutes.GAUGE)
        }
        runCatching { app.startActivity(intent) }
    }
}
