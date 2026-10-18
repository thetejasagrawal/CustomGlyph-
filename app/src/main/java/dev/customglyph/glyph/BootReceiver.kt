package dev.customglyph.glyph

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.customglyph.GlyphApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
        )
            return
        val app = context.applicationContext as GlyphApplication
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && !app.repository.restoreOnBoot) {
            app.repository.enabled = false
            app.repository.stopAt = 0
            app.glyph.refresh()
            return
        }
        if (
            app.repository.enabled &&
                app.repository.showUnlocked &&
                app.repository.restoreOnBoot &&
                app.glyph.directDisplay
        ) {
            try {
                DisplayService.start(context)
            } catch (e: RuntimeException) {
                app.glyph.report(
                    "Resume needed",
                    "Android blocked automatic restart. Open Custom Glyph and tap Apply. Your design is saved.",
                    true,
                )
                app.repository.prefs.edit().putBoolean("resume_needed", true).apply()
            }
        }
    }
}
