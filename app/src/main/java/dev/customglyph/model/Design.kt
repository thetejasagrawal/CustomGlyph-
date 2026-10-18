package dev.customglyph.model

import dev.customglyph.imaging.Adjustments
import dev.customglyph.imaging.Raster
import java.time.ZonedDateTime
import java.util.UUID

/** A decoded source; GIF delays are bounded to keep playback at or below 12 fps. */
data class SourceMedia(val frames: List<Raster>, val delays: List<Int>, val format: String) {
    init {
        require(frames.isNotEmpty() && frames.size == delays.size)
        require(delays.all { it >= 83 })
    }

    val duration: Long
        get() = delays.sum().toLong()
}

data class Draft(
    val image: String? = null,
    val name: String = "Glyph mark",
    val adjustments: Adjustments = Adjustments(),
    val profile: DeviceProfile = DeviceProfile.PHONE_3,
)

/** Hardware-ready frames. The exact same maps are used by preview and export. */
data class FrameSequence(
    val profile: DeviceProfile,
    val frames: List<IntArray>,
    val delays: List<Int>,
) {
    init {
        require(frames.isNotEmpty() && frames.size <= 240 && frames.size == delays.size)
        require(
            frames.all {
                it.size == profile.size * profile.size && it.all { value -> value in 0..255 }
            }
        )
        require(delays.all { it in 83..30_000 })
    }

    val duration: Long
        get() = delays.sum().toLong()

    fun indexAt(elapsedMs: Long, speed: Float = 1f): Int {
        if (frames.size == 1) return 0
        var remaining = ((elapsedMs.coerceAtLeast(0) * speed).toLong() % duration)
        delays.forEachIndexed { index, delay ->
            if (remaining < delay) return index
            remaining -= delay
        }
        return 0
    }
}

data class SavedDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val draft: Draft,
    val frameFile: String,
    val recent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Local wall-clock schedule. Overnight rules belong to the weekday on which they start. */
data class ScheduleRule(
    val id: String = UUID.randomUUID().toString(),
    val designId: String,
    val startMinute: Int,
    val endMinute: Int,
    val days: Set<Int> = (1..7).toSet(),
    val enabled: Boolean = true,
) {
    init {
        require(startMinute in 0..1439 && endMinute in 0..1439)
        require(days.isNotEmpty() && days.all { it in 1..7 })
    }

    fun matches(now: ZonedDateTime): Boolean {
        if (!enabled) return false
        val minute = now.hour * 60 + now.minute
        return when {
            startMinute == endMinute -> now.dayOfWeek.value in days
            startMinute < endMinute ->
                now.dayOfWeek.value in days && minute in startMinute until endMinute
            minute >= startMinute -> now.dayOfWeek.value in days
            minute < endMinute -> now.minusDays(1).dayOfWeek.value in days
            else -> false
        }
    }
}

object SchedulePolicy {
    /**
     * Later rules win when windows overlap. Outside windows, the manually applied design is used.
     */
    fun select(rules: List<ScheduleRule>, now: ZonedDateTime): String? =
        rules.lastOrNull { it.matches(now) }?.designId

    fun expired(stopAt: Long, nowMillis: Long): Boolean = stopAt > 0 && nowMillis >= stopAt
}
