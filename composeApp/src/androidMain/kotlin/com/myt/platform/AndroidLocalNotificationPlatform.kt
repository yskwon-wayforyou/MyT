package com.myt.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myt.MainActivity
import com.myt.domain.model.NotificationCategory
import com.myt.service.VehiclePresenceLauncher
import com.myt.navigation.MyTRoutes

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
            Triple(CHANNEL_SPEEDCAM, "단속 카메라", NotificationManager.IMPORTANCE_HIGH),
        ).forEach { (id, name, importance) ->
            nm.createNotificationChannel(
                NotificationChannel(id, name, importance).apply {
                    description = "MyT $name"
                },
            )
        }
    }

    actual fun post(title: String, body: String, category: NotificationCategory, route: String?) {
        ensureChannels()
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val channel = channelFor(category)
        val deepRoute = route ?: defaultRoute(category)
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(VehiclePresenceLauncher.EXTRA_ROUTE, deepRoute)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            category.ordinal + 3000,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(
                if (category == NotificationCategory.SpeedCam || category == NotificationCategory.Automation) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )
            .build()
        NotificationManagerCompat.from(ctx).notify(nextId++, notification)
    }

    actual fun openAppNotificationSettings(): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun channelFor(category: NotificationCategory): String = when (category) {
        NotificationCategory.Control -> CHANNEL_CONTROL
        NotificationCategory.Charge -> CHANNEL_CHARGE
        NotificationCategory.Automation -> CHANNEL_AUTOMATION
        NotificationCategory.SpeedCam -> CHANNEL_SPEEDCAM
    }

    private fun defaultRoute(category: NotificationCategory): String = MyTRoutes.GAUGE

    companion object {
        const val CHANNEL_CONTROL = "myt_control"
        const val CHANNEL_CHARGE = "myt_charge"
        const val CHANNEL_AUTOMATION = "myt_automation"
        const val CHANNEL_SPEEDCAM = "myt_speedcam"
    }
}
