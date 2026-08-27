package com.myt.platform

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.myt.widget.MyTGlanceWidget
import com.myt.phase2.WidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual object WidgetSnapshotPublisher {
    @Volatile
    private var appContext: Context? = null

    fun bind(context: Context) {
        appContext = context.applicationContext
    }

    actual fun publish(snapshot: WidgetSnapshot) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SOC, snapshot.socPercent)
            .putInt(KEY_RANGE, snapshot.rangeKm)
            .putInt(KEY_LOCKED, when (snapshot.locked) {
                true -> 1
                false -> 0
                null -> -1
            })
            .putLong(KEY_UPDATED, snapshot.updatedAtMs)
            .apply()
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { MyTGlanceWidget().updateAll(ctx) }
        }
    }

    actual fun read(): WidgetSnapshot? {
        val ctx = appContext ?: return null
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_SOC)) return null
        val lockedFlag = prefs.getInt(KEY_LOCKED, -1)
        return WidgetSnapshot(
            socPercent = prefs.getInt(KEY_SOC, 0),
            rangeKm = prefs.getInt(KEY_RANGE, 0),
            locked = when (lockedFlag) {
                1 -> true
                0 -> false
                else -> null
            },
            updatedAtMs = prefs.getLong(KEY_UPDATED, 0L),
        )
    }

    fun read(context: Context): WidgetSnapshot? {
        if (appContext == null) bind(context)
        return read()
    }

    const val PREFS = "myt_widget_snapshot"
    private const val KEY_SOC = "soc"
    private const val KEY_RANGE = "range_km"
    private const val KEY_LOCKED = "locked"
    private const val KEY_UPDATED = "updated_at"
}
