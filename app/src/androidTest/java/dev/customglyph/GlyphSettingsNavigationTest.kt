package dev.customglyph

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.customglyph.glyph.GlyphSettingsNavigator
import dev.customglyph.glyph.GlyphSettingsNavigator.Destination
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Tests intent routing, not the availability of any Nothing OS screen on a device. */
@RunWith(AndroidJUnit4::class)
class GlyphSettingsNavigationTest {
    private class NavigationContext(val launch: (Intent) -> Unit) :
        ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        val attempts = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            attempts += intent
            launch(intent)
        }
    }

    @Test
    fun directPickerDoesNotOpenExtraScreens() {
        val context = NavigationContext {}
        assertEquals(Destination.ALWAYS_ON, GlyphSettingsNavigator.openAlwaysOn(context))
        assertEquals(1, context.attempts.size)
        assertEquals(
            "com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity",
            context.attempts.single().component?.className,
        )
        assertEquals("com.nothing.thirdparty", context.attempts.single().component?.packageName)
    }

    @Test
    fun missingOrRestrictedPickerFallsBackWithoutCrashing() {
        for (failure in listOf(ActivityNotFoundException(), SecurityException())) {
            val context = NavigationContext { intent ->
                if (intent.component?.className?.endsWith("AodToySelectActivity") == true)
                    throw failure
            }
            assertEquals(Destination.TOY_MANAGER, GlyphSettingsNavigator.openAlwaysOn(context))
            assertEquals(2, context.attempts.size)
            assertTrue(
                context.attempts.last().component!!.className.endsWith("ToysManagerActivity")
            )
        }
    }

    @Test
    fun absentGlyphScreensUseSystemSettingsAndHandleNoSettingsApp() {
        val context = NavigationContext { intent ->
            if (intent.component != null) throw ActivityNotFoundException()
        }
        assertEquals(Destination.SYSTEM_SETTINGS, GlyphSettingsNavigator.openAlwaysOn(context))
        assertEquals(Settings.ACTION_SETTINGS, context.attempts.last().action)
        val unavailable = NavigationContext { throw ActivityNotFoundException() }
        assertEquals(Destination.UNAVAILABLE, GlyphSettingsNavigator.openAlwaysOn(unavailable))
    }
}
