package com.myt.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

actual class LocalNotificationPlatform actual constructor(context: Any) {
    private val ctx = context as Context
    private var nextId = 2100

    actual fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            Triple(CHANNEL_CONTROL, "차량 제어", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_CHARGE, "충전", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_AUTOMATION, "자동화 · 알림", NotificationManager.IMPORTANCE_HIGH),
        ).forEach { (id, name, importance) ->
            nm.createNotificationChannel(
                NotificationChannel(id, name, importance).apply {
                    description = "MyT $name"
                },
            )
        }
    }

    actual fun post(title: String, body: String) {
        ensureChannels()
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val channel = when {
            title.contains("제어") -> CHANNEL_CONTROL
            title.contains("충전") || body.contains("충전") -> CHANNEL_CHARGE
            else -> CHANNEL_AUTOMATION
        }
        val notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val id = nextId++
        NotificationManagerCompat.from(ctx).notify(id, notification)
    }

    actual fun openAppNotificationSettings(): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        true
    }.getOrDefault(false)

    companion object {
        const val CHANNEL_CONTROL = "myt_control"
        const val CHANNEL_CHARGE = "myt_charge"
        const val CHANNEL_AUTOMATION = "myt_automation"
    }
}
