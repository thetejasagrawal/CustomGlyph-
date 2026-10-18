package dev.customglyph

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.customglyph.glyph.GlyphFrameEncoder
import dev.customglyph.model.DeviceProfile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Bitmap ownership regression tests; no vendor service or device identity is simulated. */
@RunWith(AndroidJUnit4::class)
class GlyphFrameEncodingTest {
    @Test
    fun sourceStaysAliveUntilRenderingFinishesAndResultSurvivesCleanup() {
        for (profile in DeviceProfile.entries) {
            val pixels = IntArray(profile.size * profile.size) { it % 256 }
            lateinit var source: Bitmap
            val result =
                GlyphFrameEncoder.renderPixels(profile, pixels) { bitmap ->
                    source = bitmap
                    assertFalse(bitmap.isRecycled)
                    val argb = IntArray(pixels.size)
                    // Read during deferred rendering, as the SDK does after constructing its frame.
                    bitmap.getPixels(argb, 0, profile.size, 0, 0, profile.size, profile.size)
                    IntArray(argb.size) { argb[it] and 255 }
                }
            assertTrue(source.isRecycled)
            assertArrayEquals(pixels, result)
        }
    }

    @Test
    fun renderingFailureReleasesSourceAndPreservesOriginalError() {
        val profile = DeviceProfile.PHONE_3
        lateinit var source: Bitmap
        val failure = IllegalStateException("Renderer refused the frame")
        try {
            GlyphFrameEncoder.renderPixels(profile, IntArray(profile.size * profile.size)) {
                source = it
                throw failure
            }
            fail("The renderer failure must reach the caller")
        } catch (error: IllegalStateException) {
            assertSame(failure, error)
        }
        assertTrue(source.isRecycled)
    }
}
