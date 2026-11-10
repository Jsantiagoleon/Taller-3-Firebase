package com.example.tallerfirebase.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

// Genera marcadores personalizados (círculo con la inicial del usuario y una punta),
// para no usar los íconos por defecto del mapa.
object MarkerIcons {

    private const val CIRCLE_DP = 48f
    private const val TAIL_DP = 10f
    private const val BORDER_DP = 3f
    private const val TEXT_SP = 20f

    fun create(context: Context, label: String, @ColorInt color: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val size = CIRCLE_DP * density
        val tail = TAIL_DP * density
        val border = BORDER_DP * density
        val radius = size / 2f

        val bitmap = Bitmap.createBitmap(size.toInt(), (size + tail).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Punta inferior
        paint.color = color
        val path = Path().apply {
            moveTo(radius - tail, size - tail)
            lineTo(radius + tail, size - tail)
            lineTo(radius, size + tail)
            close()
        }
        canvas.drawPath(path, paint)

        // Borde blanco y círculo de color
        paint.color = Color.WHITE
        canvas.drawCircle(radius, radius, radius, paint)
        paint.color = color
        canvas.drawCircle(radius, radius, radius - border, paint)

        // Inicial del nombre
        paint.color = Color.WHITE
        paint.textSize = TEXT_SP * density
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        val textY = radius - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(label, radius, textY, paint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun initialOf(nombre: String?): String =
        nombre?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
