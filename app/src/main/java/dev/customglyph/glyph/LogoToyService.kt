package dev.customglyph.glyph

import android.app.Service
import android.content.Intent
import android.os.*
import com.nothing.ketchum.GlyphToy
import dev.customglyph.GlyphApplication

/**
 * Exported because Nothing OS discovers/binds TOY services. Accepts no caller-supplied pixels or
 * files.
 */
class LogoToyService : Service() {
    private val glyph
        get() = (application as GlyphApplication).glyph

    private val messenger =
        Messenger(
            Handler(Looper.getMainLooper()) { msg ->
                if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                    if (msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA) == GlyphToy.EVENT_AOD)
                        glyph.aodTick()
                    true
                } else false
            }
        )

    override fun onBind(intent: Intent?): IBinder {
        glyph.attach(GlyphController.TOY)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        glyph.detach(GlyphController.TOY)
        return false
    }

    override fun onDestroy() {
        glyph.detach(GlyphController.TOY)
        super.onDestroy()
    }
}
