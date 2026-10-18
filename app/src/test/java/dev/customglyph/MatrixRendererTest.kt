package dev.customglyph

import dev.customglyph.glyph.*
import dev.customglyph.imaging.*
import org.junit.Assert.*
import org.junit.Test

class MatrixRendererTest {
    private val white = Raster(25, 25, IntArray(625) { -1 })

    @Test
    fun hardwareMaskHas489SymmetricLeds() {
        assertEquals(
            489,
            (0..24).sumOf { y ->
                (0..24).count { x -> dev.customglyph.model.DeviceProfile.PHONE_3.contains(x, y) }
            },
        )
        for (y in 0..24) for (x in 0..24) {
            assertEquals(
                dev.customglyph.model.DeviceProfile.PHONE_3.contains(x, y),
                dev.customglyph.model.DeviceProfile.PHONE_3.contains(24 - x, y),
            )
            assertEquals(
                dev.customglyph.model.DeviceProfile.PHONE_3.contains(x, y),
                dev.customglyph.model.DeviceProfile.PHONE_3.contains(x, 24 - y),
            )
        }
        assertFalse(dev.customglyph.model.DeviceProfile.PHONE_3.contains(0, 0))
        assertTrue(dev.customglyph.model.DeviceProfile.PHONE_3.contains(12, 0))
    }

    @Test
    fun transparentPixelsStayDarkEvenWhenInvertedOrContrasted() {
        val transparent = Raster(25, 25, IntArray(625) { 0x00ffffff })
        for (silhouette in listOf(true, false)) {
            assertTrue(
                MatrixRenderer.render(
                        transparent,
                        Adjustments(contrast = 3f, invert = true, silhouette = silhouette),
                    )
                    .all { it == 0 }
            )
        }
    }

    @Test
    fun brightnessZeroAndClippedCornersStayDark() {
        assertTrue(MatrixRenderer.render(white, Adjustments(brightness = 0f)).all { it == 0 })
        val frame = MatrixRenderer.render(white, Adjustments(brightness = 1f))
        assertEquals(255, frame[12 * 25 + 12])
        assertEquals(0, frame[0])
        assertEquals(0, frame[624])
    }

    @Test
    fun halfAlphaAndBrightnessMultiply() {
        val half = Raster(25, 25, IntArray(625) { 0x80ffffff.toInt() })
        assertEquals(64, MatrixRenderer.render(half, Adjustments(brightness = .5f))[312])
    }

    @Test
    fun silhouetteMakesBlackLogoVisible() {
        val black = Raster(25, 25, IntArray(625) { 0xff000000.toInt() })
        assertEquals(0, MatrixRenderer.render(black, Adjustments())[312])
        assertEquals(
            255,
            MatrixRenderer.render(black, Adjustments(brightness = 1f, silhouette = true))[312],
        )
        assertEquals(
            255,
            MatrixRenderer.render(black, Adjustments(brightness = 1f, invert = true))[312],
        )
    }

    @Test
    fun rotationAndTranslationPreserveAsymmetricFeature() {
        val p = IntArray(625)
        for (y in 5..7) for (x in 11..13) p[y * 25 + x] = -1
        val src = Raster(25, 25, p)
        val rotated = MatrixRenderer.render(src, Adjustments(rotation = 90f, brightness = 1f))
        assertTrue(rotated[12 * 25 + 18] > 200)
        assertEquals(0, rotated[6 * 25 + 12])
        val moved = MatrixRenderer.render(src, Adjustments(x = 3f, y = 2f, brightness = 1f))
        assertTrue(moved[8 * 25 + 15] > 200)
    }

    @Test
    fun outOfBoundsImageIsBlank() {
        assertTrue(MatrixRenderer.render(white, Adjustments(x = 25f, y = 25f)).all { it == 0 })
    }

    @Test
    fun ditherProducesOnlyOffAndSelectedBrightness() {
        val gray = Raster(25, 25, IntArray(625) { 0xff888888.toInt() })
        val result = MatrixRenderer.render(gray, Adjustments(brightness = 1f, dither = true))
        assertTrue(result.all { it == 0 || it == 255 })
        assertTrue(result.count { it == 255 } in 180..310)
    }

    @Test
    fun malformedAdjustmentsAreNormalized() {
        val a =
            Adjustments(
                    scale = Float.NaN,
                    brightness = Float.POSITIVE_INFINITY,
                    rotation = 910f,
                    x = 1000f,
                )
                .normalized()
        assertEquals(1f, a.scale)
        assertEquals(.75f, a.brightness)
        assertEquals(-170f, a.rotation)
        assertEquals(25f, a.x)
    }

    @Test
    fun faceDownDebouncesAndUsesHysteresis() {
        val detector = FaceDownDetector()
        assertFalse(detector.update(-9.8f, 0))
        assertFalse(detector.update(-9.8f, 200))
        assertTrue(detector.update(-9.8f, 400))
        for (t in 500L..1500L step 100) assertTrue(detector.update(-7f, t))
        for (t in 1600L..4000L step 100) detector.update(9.8f, t)
        assertFalse(detector.faceDown)
    }

    @Test
    fun displayPolicyAllCombinations() {
        for (enabled in listOf(false, true)) for (interactive in listOf(false, true)) for (locked in
            listOf(false, true)) for (down in listOf(false, true)) {
            val expected = enabled && (down || (interactive && !locked))
            assertEquals(expected, DisplayPolicy.shouldDisplay(enabled, interactive, locked, down))
        }
    }
}
