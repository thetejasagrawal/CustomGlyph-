package dev.customglyph.imaging

import dev.customglyph.model.DeviceProfile
import kotlin.math.*

data class Adjustments(
    val scale: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val brightness: Float = .75f,
    val contrast: Float = 1f,
    val silhouette: Boolean = false,
    val invert: Boolean = false,
    val dither: Boolean = false,
) {
    fun normalized() =
        copy(
            scale = scale.sane(1f).coerceIn(.2f, 4f),
            x = x.sane(0f).coerceIn(-25f, 25f),
            y = y.sane(0f).coerceIn(-25f, 25f),
            rotation = ((rotation.sane(0f) + 180f) % 360f + 360f) % 360f - 180f,
            brightness = brightness.sane(.75f).coerceIn(0f, 1f),
            contrast = contrast.sane(1f).coerceIn(.25f, 3f),
        )

    private fun Float.sane(fallback: Float) = if (isFinite()) this else fallback
}

data class Raster(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(width > 0 && height > 0 && argb.size == width * height)
    }
}

/** One immutable luminance map shared by the editor and the SDK adapter. No hardware simulation. */
object MatrixRenderer {
    private val bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    fun render(
        source: Raster,
        value: Adjustments,
        profile: DeviceProfile = DeviceProfile.PHONE_3,
    ): IntArray {
        val a = value.normalized()
        val size = profile.size
        val output = IntArray(size * size)
        val radians = a.rotation * PI / 180
        val cs = cos(radians)
        val sn = sin(radians)
        val fit = size.toDouble() / max(source.width, source.height) * a.scale
        for (y in 0 until size) for (x in 0 until size) {
            if (!profile.contains(x, y)) continue
            var sum = 0.0
            // Area sampling makes thin shapes and subpixel translation stable at 25 pixels.
            for (sy in 0..5) for (sx in 0..5) {
                val dx = x + (sx + .5) / 6 - size / 2.0 - a.x
                val dy = y + (sy + .5) / 6 - size / 2.0 - a.y
                val px = (cs * dx + sn * dy) / fit + source.width / 2.0 - .5
                val py = (-sn * dx + cs * dy) / fit + source.height / 2.0 - .5
                sum += sample(source, px, py, a)
            }
            var light = sum / 36
            if (a.dither) light = if (light > (bayer[(y % 4) * 4 + x % 4] + .5) / 16) 1.0 else 0.0
            output[y * size + x] = (light * a.brightness * 255).roundToInt().coerceIn(0, 255)
        }
        return output
    }

    private fun sample(src: Raster, px: Double, py: Double, a: Adjustments): Double {
        val x = floor(px).toInt()
        val y = floor(py).toInt()
        val fx = px - x
        val fy = py - y
        var light = 0.0
        for (iy in 0..1) for (ix in 0..1) {
            val xx = x + ix
            val yy = y + iy
            if (xx !in 0 until src.width || yy !in 0 until src.height) continue
            val color = src.argb[yy * src.width + xx]
            val alpha = (color ushr 24) / 255.0
            val lum =
                if (a.silhouette) 1.0
                else {
                    val l =
                        (.2126 * ((color ushr 16) and 255) +
                            .7152 * ((color ushr 8) and 255) +
                            .0722 * (color and 255)) / 255
                    val inverted = if (a.invert) 1 - l else l
                    ((inverted - .5) * a.contrast + .5).coerceIn(0.0, 1.0)
                }
            light += lum * alpha * (if (ix == 0) 1 - fx else fx) * (if (iy == 0) 1 - fy else fy)
        }
        return light
    }

    fun sampleLogo(): Raster {
        val pixels = IntArray(25 * 25)
        for (y in 5..19) for (x in 5..19) {
            val topBottom = (y in 5..7 || y in 17..19) && x in 9..17
            val left = x in 5..7 && y in 9..15
            val corners = ((x in 7..9) && (y in 7..9 || y in 15..17))
            if (topBottom || left || corners) pixels[y * 25 + x] = -1
        }
        return Raster(25, 25, pixels)
    }
}
