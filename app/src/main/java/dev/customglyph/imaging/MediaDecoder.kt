package dev.customglyph.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.caverock.androidsvg.SVG
import dev.customglyph.model.SourceMedia
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

/** Decodes untrusted documents with explicit byte, dimension, frame and duration limits. */
object MediaDecoder {
    const val MAX_BYTES = 32 * 1024 * 1024
    private const val MAX_FRAMES = 240
    private const val MAX_DURATION = 30_000

    fun decode(file: File): SourceMedia {
        require(file.length() in 1..MAX_BYTES) { "Choose a file smaller than 32 MB." }
        val signature = file.inputStream().use { it.readNBytes(16) }
        return when {
            signature.take(3).toByteArray().contentEquals("GIF".toByteArray()) -> decodeGif(file)
            looksLikeSvg(file) -> decodeSvg(file)
            else -> decodeBitmap(file)
        }
    }

    private fun looksLikeSvg(file: File): Boolean {
        val prefix =
            file
                .inputStream()
                .use { it.readNBytes(1024) }
                .toString(Charsets.UTF_8)
                .trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        return prefix.startsWith("<") &&
            (prefix.contains("<svg", ignoreCase = true) || prefix.startsWith("<?xml"))
    }

    private fun decodeBitmap(file: File): SourceMedia {
        var format = "IMAGE"
        val bitmap =
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                require(width in 1..30_000 && height in 1..30_000) {
                    "Unsupported image dimensions."
                }
                format = info.mimeType.substringAfter('/').uppercase()
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                val ratio = minOf(1f, 1024f / max(width, height))
                decoder.setTargetSize(
                    max(1, (width * ratio).roundToInt()),
                    max(1, (height * ratio).roundToInt()),
                )
            }
        return try {
            SourceMedia(listOf(bitmap.toRaster()), listOf(1000), format)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSvg(file: File): SourceMedia {
        require(file.length() <= 2 * 1024 * 1024) { "Choose an SVG smaller than 2 MB." }
        val xml = file.readText()
        // Self-contained vector artwork only: no entities, scripts, fonts, or external resources.
        require(
            !Regex(
                    "<!DOCTYPE|<!ENTITY|<script|<image|<foreignObject|@import|url\\(\\s*['\"]?(?!#)",
                    RegexOption.IGNORE_CASE,
                )
                .containsMatchIn(xml)
        ) {
            "Use a self-contained SVG with paths and shapes, without embedded images or external resources."
        }
        require(
            !Regex("(?:href|src)\\s*=\\s*['\"](?!#)", RegexOption.IGNORE_CASE).containsMatchIn(xml)
        ) {
            "SVG references must point to shapes inside the same file."
        }
        SVG.setInternalEntitiesEnabled(false)
        val svg = SVG.getFromString(xml)
        val viewBox = svg.documentViewBox
        val width = svg.documentWidth.takeIf { it.isFinite() && it > 0 } ?: viewBox?.width() ?: 512f
        val height =
            svg.documentHeight.takeIf { it.isFinite() && it > 0 } ?: viewBox?.height() ?: 512f
        require(width.isFinite() && height.isFinite() && width > 0 && height > 0) {
            "This SVG has invalid dimensions."
        }
        val scale = 1024f / max(width, height)
        val bitmap =
            Bitmap.createBitmap(
                (width * scale).roundToInt().coerceIn(1, 1024),
                (height * scale).roundToInt().coerceIn(1, 1024),
                Bitmap.Config.ARGB_8888,
            )
        return try {
            svg.documentWidth = bitmap.width.toFloat()
            svg.documentHeight = bitmap.height.toFloat()
            svg.renderToCanvas(Canvas(bitmap))
            SourceMedia(listOf(bitmap.toRaster()), listOf(1000), "SVG")
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeGif(file: File): SourceMedia {
        val bytes = file.readBytes()
        val parser = GifHeaderParser().setData(bytes)
        val header =
            try {
                parser.parseHeader()
            } finally {
                parser.clear()
            }
        require(header.status == GifDecoder.STATUS_OK && header.numFrames in 1..MAX_FRAMES) {
            "Use a GIF with 1–240 frames (up to 30 seconds)."
        }
        require(header.width in 1..4096 && header.height in 1..4096) {
            "GIF dimensions must be at most 4096 × 4096."
        }
        var sample = 1
        while (max(header.width, header.height) / sample > 128) sample *= 2
        val decoder = StandardGifDecoder(BitmapProvider(), header, ByteBuffer.wrap(bytes), sample)
        val frames = ArrayList<Raster>()
        val delays = ArrayList<Int>()
        try {
            repeat(header.numFrames) {
                decoder.advance()
                val delay = decoder.nextDelay.coerceAtLeast(83)
                require(delays.sum() + delay <= MAX_DURATION) {
                    "Use a GIF that plays for at most 30 seconds at 12 fps."
                }
                val bitmap = decoder.nextFrame ?: error("This GIF contains an unreadable frame.")
                try {
                    frames += bitmap.toRaster()
                } finally {
                    bitmap.recycle()
                }
                delays += delay
            }
        } finally {
            decoder.clear()
        }
        return SourceMedia(frames, delays, "GIF")
    }

    private class BitmapProvider : GifDecoder.BitmapProvider {
        override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap =
            Bitmap.createBitmap(width, height, config).apply { eraseColor(Color.TRANSPARENT) }

        override fun release(bitmap: Bitmap) = bitmap.recycle()

        override fun obtainByteArray(size: Int) = ByteArray(size)

        override fun release(bytes: ByteArray) = Unit

        override fun obtainIntArray(size: Int) = IntArray(size)

        override fun release(array: IntArray) = Unit
    }
}

fun Bitmap.toRaster(): Raster =
    Raster(
        width,
        height,
        IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) },
    )
