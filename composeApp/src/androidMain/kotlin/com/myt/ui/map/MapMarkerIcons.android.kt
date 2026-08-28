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
        val sizePx = (36 * context.resources.displayMetrics.density).toInt().coerceAtLeast(28)
        val bitmap = drawable.toBitmap(sizePx, sizePx)
        return BitmapDrawable(context.resources, bitmap)
    }

    fun cameraIcon(context: Context, highlighted: Boolean = false, scale: Float = 1f): Drawable {
        val drawable = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_map_speed_camera))
        val basePx = (if (highlighted) 40 else 34) * context.resources.displayMetrics.density
        val sizePx = (basePx * scale).toInt().coerceAtLeast(28)
        val bitmap = drawable.toBitmap(sizePx, sizePx)
        if (!highlighted) return BitmapDrawable(context.resources, bitmap)
        val glow = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(glow)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFFF3B30")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = sizePx * 0.12f
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.2f, paint)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return BitmapDrawable(context.resources, glow)
    }

    fun destIcon(context: Context): Drawable {
        val sizePx = (26 * context.resources.displayMetrics.density).toInt().coerceAtLeast(20)
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
