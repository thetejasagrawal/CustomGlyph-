package dev.customglyph.glyph

import android.content.Context
import android.graphics.Bitmap
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import dev.customglyph.model.DeviceProfile

/** Complete SDK rendering before releasing its source image; cache only the resulting LED data. */
internal object GlyphFrameEncoder {
    fun encode(context: Context, profile: DeviceProfile, pixels: IntArray): IntArray =
        renderPixels(profile, pixels) { bitmap ->
            val image =
                GlyphMatrixObject.Builder()
                    .setImageSource(bitmap)
                    .setPosition(0, 0)
                    .setScale(100)
                    .setOrientation(0)
                    .setBrightness(255)
                    .setReverse(false)
                    .build()
            GlyphMatrixFrame.Builder().addTop(image).build(context).render().also {
                check(it.size == pixels.size) { "Glyph SDK returned an unexpected matrix size." }
            }
        }

    internal fun renderPixels(
        profile: DeviceProfile,
        pixels: IntArray,
        render: (Bitmap) -> IntArray,
    ): IntArray {
        val size = profile.size
        require(pixels.size == size * size && pixels.all { it in 0..255 }) {
            "Invalid ${profile.title} pixel map."
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.setPixels(
                IntArray(pixels.size) { i ->
                    val v = pixels[i]
                    0xff000000.toInt() or (v shl 16) or (v shl 8) or v
                },
                0,
                size,
                0,
                0,
                size,
                size,
            )
            render(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}
