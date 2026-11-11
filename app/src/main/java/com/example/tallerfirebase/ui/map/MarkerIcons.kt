package com.example.tallerfirebase.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

// Genera marcadores personalizados (foto circular del usuario o inicial con una punta).
object MarkerIcons {

    private const val CIRCLE_DP = 48f
    private const val TAIL_DP = 10f
    private const val BORDER_DP = 3f
    private const val TEXT_SP = 20f

    fun create(
        context: Context,
        label: String,
        @ColorInt color: Int,
        photoBitmap: Bitmap? = null
    ): Drawable {
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

        // Borde exterior
        paint.color = Color.WHITE
        canvas.drawCircle(radius, radius, radius, paint)

        if (photoBitmap != null) {
            // Fondo color
            paint.color = color
            canvas.drawCircle(radius, radius, radius - border, paint)

            // Foto circular
            val innerRadius = radius - border
            val shader = BitmapShader(photoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()
            val scale = (innerRadius * 2f) / photoBitmap.width.coerceAtLeast(photoBitmap.height)
            matrix.postScale(scale, scale)
            matrix.postTranslate(
                radius - (photoBitmap.width * scale) / 2f,
                radius - (photoBitmap.height * scale) / 2f
            )
            shader.setLocalMatrix(matrix)

            val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
            }
            canvas.drawCircle(radius, radius, innerRadius, photoPaint)
        } else {
            // Círculo de color con Inicial
            paint.color = color
            canvas.drawCircle(radius, radius, radius - border, paint)

            paint.color = Color.WHITE
            paint.textSize = TEXT_SP * density
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            val textY = radius - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(label, radius, textY, paint)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    fun initialOf(nombre: String?): String =
        nombre?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
