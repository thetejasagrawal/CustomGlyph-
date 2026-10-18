package dev.customglyph

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nothing.ketchum.*
import dev.customglyph.data.DesignRepository
import dev.customglyph.imaging.*
import dev.customglyph.model.*
import dev.customglyph.ui.EditorViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomGlyphInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val testContext
        get() = InstrumentationRegistry.getInstrumentation().context

    private fun enterEditor() {
        compose.waitForIdle()
        if (compose.onAllNodesWithText("Set up display").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Set up display").performClick()
        if (compose.onAllNodesWithText("Open editor").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Open editor").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Choose Image").fetchSemanticsNodes().isNotEmpty()
        }
        lateinit var vm: EditorViewModel
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[EditorViewModel::class.java] }
        compose.waitUntil(10_000) { !vm.editor.value.busy }
    }

    private fun fixture(name: String): File =
        File(context.cacheDir, name).also { file ->
            testContext.assets.open(name).use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }

    @Test
    fun saveToGalleryAndCreateScheduleThroughUi() {
        enterEditor()
        lateinit var vm: EditorViewModel
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[EditorViewModel::class.java] }
        compose.onNodeWithText("Save").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("UI glyph")
        compose.onAllNodesWithText("Save").onLast().performClick()
        compose.waitUntil(5000) { vm.library.value.any { it.name == "UI glyph" } }
        compose.onNodeWithText("Gallery").performClick()
        compose.onNodeWithText("UI glyph").assertIsDisplayed()
        compose.onNodeWithText("Schedule").performClick()
        val count = vm.rules.value.size
        compose.onAllNodesWithText("Add schedule").onLast().performScrollTo().performClick()
        compose.waitUntil(5000) { vm.rules.value.size > count }
        val addedId = vm.rules.value.last().id
        compose
            .onAllNodesWithContentDescription("Edit schedule")
            .onLast()
            .performScrollTo()
            .performClick()
        compose.onNodeWithContentDescription("Monday").performScrollTo().performClick()
        compose.onNodeWithText("Update schedule").performScrollTo().performClick()
        compose.waitUntil(5000) {
            vm.rules.value.last().id == addedId && 1 !in vm.rules.value.last().days
        }
        assertEquals(count + 1, vm.rules.value.size)
    }

    @Test
    fun onboardingEditorPreviewAndSettingsWork() {
        enterEditor()
        compose.onNodeWithText("Preview").performClick()
        compose.onNodeWithText("Done").performScrollTo().performClick()
        compose.onNodeWithText("Apply to Glyph").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Review display setup").assertIsDisplayed().performClick()
        compose.onNodeWithText("Display setup").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
    }

    @Test
    fun previewStaysVisibleWhileAdjustingBrightnessAndAfterRecreation() {
        enterEditor()
        lateinit var vm: EditorViewModel
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[EditorViewModel::class.java] }
        compose.onNodeWithText("Reset").performClick()
        val matrix = compose.onNode(hasContentDescription("Glyph Matrix preview", substring = true))
        val before = matrix.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Brightness").performScrollTo().performSemanticsAction(
            SemanticsActions.SetProgress
        ) {
            it(.4f)
        }
        matrix.assertIsDisplayed()
        assertEquals(before, matrix.fetchSemanticsNode().boundsInRoot)
        compose.waitUntil(5000) {
            kotlin.math.abs(vm.editor.value.draft.adjustments.brightness - .4f) < .01f
        }
        compose.waitUntil(5000) {
            kotlin.math.abs(DesignRepository(context).readDraft().adjustments.brightness - .4f) <
                .01f
        }
        matrix.performTouchInput { swipe(center, center + Offset(40f, 20f), 400) }
        compose.waitUntil(5000) { vm.editor.value.draft.adjustments.x > .5f }
        compose.onNodeWithText("Center").performClick()
        compose.waitUntil(5000) { vm.editor.value.draft.adjustments.x == 0f }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Apply to Glyph").assertIsDisplayed()
        assertEquals(.4f, vm.editor.value.draft.adjustments.brightness)
    }

    @Test
    fun mediaFormatsAlphaAndGifDisposalDecodeCorrectly() {
        for (name in listOf("alpha.png", "still.jpg", "still.webp", "shape.svg", "motion.gif")) {
            val media = MediaDecoder.decode(fixture(name))
            assertTrue(media.frames.first().width > 0)
            assertTrue(media.frames.first().argb.any { (it ushr 24) > 0 })
        }
        val gif = MediaDecoder.decode(fixture("motion.gif"))
        assertEquals(2, gif.frames.size)
        assertEquals(listOf(100, 200), gif.delays)
        val left = 16 * 32 + 6
        val right = 16 * 32 + 24
        assertTrue((gif.frames[0].argb[left] ushr 24) > 0)
        assertEquals(0, gif.frames[1].argb[left] ushr 24)
        assertTrue((gif.frames[1].argb[right] ushr 24) > 0)
        val malicious =
            File(context.cacheDir, "entity.svg").apply {
                writeText(
                    "<!DOCTYPE svg [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><svg xmlns=\"http://www.w3.org/2000/svg\"><text>&e;</text></svg>"
                )
            }
        try {
            MediaDecoder.decode(malicious)
            fail("External entity accepted")
        } catch (_: IllegalArgumentException) {}
        val invalid = File(context.cacheDir, "broken.png").apply { writeText("not an image") }
        try {
            MediaDecoder.decode(invalid)
            fail("Invalid image accepted")
        } catch (_: Exception) {}
    }

    @Test
    fun gallerySchedulesAndAppliedAnimationSurviveRepositoryRecreation() {
        val repo = DesignRepository(context)
        val imported = repo.importMedia(Uri.fromFile(fixture("motion.gif")))
        val draft =
            Draft(
                imported.file,
                "Test animation",
                Adjustments(x = 1.25f),
                DeviceProfile.PHONE_4A_PRO,
            )
        repo.saveDraft(draft)
        val sequence =
            FrameSequence(
                draft.profile,
                imported.media.frames.map {
                    MatrixRenderer.render(it, draft.adjustments, draft.profile)
                },
                imported.media.delays,
            )
        val saved = repo.saveDesign("Motion", draft, sequence)
        val rule = ScheduleRule(designId = saved.id, startMinute = 22 * 60, endMinute = 6 * 60)
        repo.saveRule(rule)
        repo.apply(draft, sequence)
        val reloaded = DesignRepository(context)
        assertEquals(draft, reloaded.readDraft())
        assertTrue(reloaded.designs().any { it.id == saved.id })
        assertTrue(reloaded.designs().any { it.recent })
        assertEquals(rule, reloaded.rules().first { it.id == rule.id })
        val persisted = reloaded.readSequence(saved.frameFile)
        assertArrayEquals(sequence.frames[1], persisted.frames[1])
        assertEquals(sequence.delays, persisted.delays)
        reloaded.deleteDesign(saved.id)
        assertFalse(reloaded.rules().any { it.designId == saved.id })
    }

    @Test
    fun pngAndSvgExportsKeepThePixelMapAndTransparentCorners() {
        for (profile in DeviceProfile.entries) {
            val pixels = MatrixRenderer.render(MatrixRenderer.sampleLogo(), Adjustments(), profile)
            val bytes =
                ByteArrayOutputStream().also { MatrixExport.png(pixels, profile, it) }.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(profile.size, bitmap.width)
            assertEquals(0, bitmap.getPixel(0, 0) ushr 24)
            for (y in 0 until profile.size) for (x in 0 until profile.size) if (
                profile.contains(x, y)
            ) {
                assertEquals(pixels[y * profile.size + x], bitmap.getPixel(x, y) and 255)
            }
            bitmap.recycle()
            val svg = MatrixExport.svg(pixels, profile)
            assertEquals(profile.ledCount, Regex("<rect ").findAll(svg).count())
        }
    }

    @Test
    fun officialSdkPreservesLayoutOnSupportedHardware() {
        Assume.assumeTrue(
            "Requires a physical Nothing phone; no identity spoofing.",
            Common.is23112() || Common.is25111p(),
        )
        val size = Common.getDeviceMatrixLength()
        val center = size / 2
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff000000.toInt())
        bitmap.setPixel(center, center - 3, -1)
        bitmap.setPixel(center + 3, center, 0xff808080.toInt())
        val image =
            GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setScale(100)
                .setBrightness(255)
                .setPosition(0, 0)
                .setOrientation(0)
                .setReverse(false)
                .build()
        val result = GlyphMatrixFrame.Builder().addTop(image).build(context).render()
        assertEquals(size * size, result.size)
        assertTrue(result[(center - 3) * size + center] > result[center * size + center + 3])
        assertTrue(result[center * size + center + 3] > 0)
        assertEquals(0, result[0])
        bitmap.recycle()
    }
}
