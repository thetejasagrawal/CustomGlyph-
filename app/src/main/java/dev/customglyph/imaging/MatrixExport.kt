package dev.customglyph.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import dev.customglyph.model.DeviceProfile
import java.io.OutputStream
import java.util.Locale

object MatrixExport {
    /** Native pixel map, with transparent corners outside the physical LED mask. */
    fun png(
        pixels: IntArray,
        profile: DeviceProfile,
        output: OutputStream,
        artwork: Boolean = false,
    ) {
        val bitmap =
            if (artwork) artwork(pixels, profile)
            else
                Bitmap.createBitmap(
                    IntArray(pixels.size) { i ->
                        val x = i % profile.size
                        val y = i / profile.size
                        val v = pixels[i]
                        if (!profile.contains(x, y)) 0
                        else 0xff000000.toInt() or (v shl 16) or (v shl 8) or v
                    },
                    profile.size,
                    profile.size,
                    Bitmap.Config.ARGB_8888,
                )
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not encode PNG."
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun artwork(pixels: IntArray, profile: DeviceProfile): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xff080808.toInt()
        canvas.drawCircle(500f, 500f, 500f, paint)
        val pitch = 820f / profile.size
        val led = pitch * profile.aperture
        for (y in 0 until profile.size) for (x in 0 until profile.size) {
            if (!profile.contains(x, y)) continue
            val v = (22 + pixels[y * profile.size + x] * 233 / 255).coerceIn(0, 255)
            paint.color = 0xff000000.toInt() or (v shl 16) or (v shl 8) or v
            val left = 90 + (x + .5f) * pitch - led / 2
            val top = 90 + (y + .5f) * pitch - led / 2
            canvas.drawRoundRect(left, top, left + led, top + led, led * .12f, led * .12f, paint)
        }
        return bitmap
    }

    fun svg(pixels: IntArray, profile: DeviceProfile): String = buildString {
        val n = profile.size
        append(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$n\" height=\"$n\" viewBox=\"0 0 $n $n\">\n"
        )
        for (y in 0 until n) for (x in 0 until n) {
            if (!profile.contains(x, y)) continue
            val hex = String.format(Locale.ROOT, "%02x", pixels[y * n + x])
            append("<rect x=\"$x\" y=\"$y\" width=\"1\" height=\"1\" fill=\"#$hex$hex$hex\"/>\n")
        }
        append("</svg>\n")
    }
}
