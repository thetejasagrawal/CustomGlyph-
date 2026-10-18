package dev.customglyph.glyph

/** Pure policy, separate from sensors and vendor calls for deterministic transition tests. */
object DisplayPolicy {
    enum class Route {
        APP,
        TOY,
    }

    fun shouldDisplay(
        enabled: Boolean,
        interactive: Boolean,
        locked: Boolean,
        faceDown: Boolean,
    ): Boolean = enabled && ((interactive && !locked) || faceDown)

    /** AOD is authoritative only when no fresh sensor reading is available. */
    fun faceDown(readingFresh: Boolean, measuredDown: Boolean, selectedToyAod: Boolean): Boolean =
        if (readingFresh) measuredDown else selectedToyAod

    /** Never fight the system Always-On selection with an app request while locked. */
    fun route(
        toyBound: Boolean,
        directSupported: Boolean,
        showUnlocked: Boolean,
        interactive: Boolean,
        locked: Boolean,
    ): Route? =
        when {
            toyBound -> Route.TOY
            directSupported && showUnlocked && interactive && !locked -> Route.APP
            else -> null
        }
}

/** Low-pass gravity + hysteresis + dwell avoids flicker while carrying the phone. */
class FaceDownDetector {
    private var filtered: Float? = null
    private var candidateSince: Long? = null
    var faceDown: Boolean = false
        private set

    fun update(z: Float, nowMillis: Long): Boolean {
        if (!z.isFinite()) return faceDown
        val gravity = filtered?.let { .8f * it + .2f * z } ?: z
        filtered = gravity
        val candidate = if (faceDown) gravity < -6f else gravity < -7.5f
        if (candidate == faceDown) candidateSince = null
        else {
            val start = candidateSince ?: nowMillis.also { candidateSince = it }
            if (nowMillis - start >= 350) {
                faceDown = candidate
                candidateSince = null
            }
        }
        return faceDown
    }
}
