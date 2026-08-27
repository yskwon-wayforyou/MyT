package com.myt.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.myt.R

internal object MapMarkerIcons {
    fun vehicleIcon(context: Context, headingDegrees: Float?): Drawable {
        val drawable = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_vehicle))
        val sizePx = (32 * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        val bitmap = drawable.toBitmap(sizePx, sizePx)
        return BitmapDrawable(context.resources, bitmap).apply {
            if (headingDegrees != null) {
                // osmdroid Marker.rotation handles heading; keep drawable upright.
            }
        }
    }

    fun cameraIcon(context: Context): Drawable {
        val drawable = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_speed_camera))
        val sizePx = (26 * context.resources.displayMetrics.density).toInt().coerceAtLeast(20)
        return BitmapDrawable(context.resources, drawable.toBitmap(sizePx, sizePx))
    }

    fun destIcon(context: Context): Drawable {
        val sizePx = (22 * context.resources.displayMetrics.density).toInt().coerceAtLeast(18)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#30D158")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.4f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 5f, paint)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        return bitmap
    }
}
