package com.myt.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

actual class BatteryOptimizationPlatform actual constructor(context: Any) {
    private val ctx = context as Context

    actual fun openBatteryOptimizationSettings(): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val packageName = ctx.packageName
        val intents = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                pm != null &&
                !pm.isIgnoringBatteryOptimizations(packageName)
            ) {
                add(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    },
                )
            }
            add(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(ctx.packageManager) != null) {
                ContextCompat.startActivity(ctx, intent, null)
                return true
            }
        }
        return false
    }
}
