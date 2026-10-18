package dev.customglyph

import android.app.Application
import dev.customglyph.data.DesignRepository
import dev.customglyph.glyph.GlyphController

class GlyphApplication : Application() {
    val repository by lazy { DesignRepository(this) }
    val glyph by lazy { GlyphController(this, repository) }
}
