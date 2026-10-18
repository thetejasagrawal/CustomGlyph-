package dev.customglyph.glyph

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Opens system UI only. Nothing OS still owns the selection and all access checks. */
internal object GlyphSettingsNavigator {
    enum class Destination {
        ALWAYS_ON,
        TOY_MANAGER,
        SYSTEM_SETTINGS,
        UNAVAILABLE,
    }

    fun openAlwaysOn(context: Context): Destination {
        // The AOD picker is firmware-specific, not a guaranteed SDK entry point.
        // Never route through private Settings fragments or bypass an unexported activity.
        val routes =
            listOf(
                Destination.ALWAYS_ON to toyActivity("AodToySelectActivity"),
                Destination.TOY_MANAGER to toyActivity("ToysManagerActivity"),
                Destination.SYSTEM_SETTINGS to Intent(Settings.ACTION_SETTINGS),
            )
        for ((destination, intent) in routes) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return destination
            } catch (_: ActivityNotFoundException) {
                // Older firmware can omit the picker or manager.
            } catch (_: SecurityException) {
                // Some firmware exposes a screen only to system callers.
            }
        }
        return Destination.UNAVAILABLE
    }

    private fun toyActivity(name: String) =
        Intent()
            .setComponent(
                ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.$name",
                )
            )
}
