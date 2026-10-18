package dev.customglyph

import dev.customglyph.glyph.DisplayPolicy
import dev.customglyph.model.*
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class SchedulePolicyTest {
    private fun at(value: String) = ZonedDateTime.parse(value)

    @Test
    fun bothMasksMatchHardwareAndRemainSymmetric() {
        for (profile in DeviceProfile.entries) {
            assertEquals(if (profile == DeviceProfile.PHONE_3) 489 else 137, profile.ledCount)
            for (y in 0 until profile.size) for (x in 0 until profile.size) {
                assertEquals(profile.contains(x, y), profile.contains(profile.size - 1 - x, y))
                assertEquals(profile.contains(x, y), profile.contains(x, profile.size - 1 - y))
            }
        }
    }

    @Test
    fun overnightBelongsToStartingWeekdayAndEndsExclusively() {
        val rule =
            ScheduleRule(
                designId = "night",
                startMinute = 22 * 60,
                endMinute = 6 * 60,
                days = setOf(1),
            )
        assertTrue(rule.matches(at("2026-10-19T22:00:00+05:30")))
        assertTrue(rule.matches(at("2026-10-20T05:59:00+05:30")))
        assertFalse(rule.matches(at("2026-10-20T06:00:00+05:30")))
        assertFalse(rule.matches(at("2026-10-20T23:00:00+05:30")))
    }

    @Test
    fun equalTimesMeanSelectedCalendarDays() {
        val rule = ScheduleRule(designId = "day", startMinute = 90, endMinute = 90, days = setOf(1))
        assertTrue(rule.matches(at("2026-10-19T00:01:00Z")))
        assertTrue(rule.matches(at("2026-10-19T23:59:00Z")))
        assertFalse(rule.matches(at("2026-10-20T00:01:00Z")))
    }

    @Test
    fun laterOverlappingRulesWinAndDisabledRulesDoNot() {
        val first = ScheduleRule(designId = "one", startMinute = 0, endMinute = 0)
        val second = ScheduleRule(designId = "two", startMinute = 9 * 60, endMinute = 12 * 60)
        val now = at("2026-10-19T10:00:00Z")
        assertEquals("two", SchedulePolicy.select(listOf(first, second), now))
        assertEquals("one", SchedulePolicy.select(listOf(first, second.copy(enabled = false)), now))
        assertNull(SchedulePolicy.select(listOf(second), now.plusHours(5)))
    }

    @Test
    fun daylightSavingFallBackUsesLocalClockForBothOccurrences() {
        val rule = ScheduleRule(designId = "dst", startMinute = 60, endMinute = 120)
        assertTrue(rule.matches(at("2026-11-01T01:30:00-04:00[America/New_York]")))
        assertTrue(rule.matches(at("2026-11-01T01:30:00-05:00[America/New_York]")))
        assertFalse(rule.matches(at("2026-11-01T02:00:00-05:00[America/New_York]")))
    }

    @Test
    fun stopTimerStaysExpiredAfterDeadline() {
        assertFalse(SchedulePolicy.expired(0, 100))
        assertFalse(SchedulePolicy.expired(100, 99))
        assertTrue(SchedulePolicy.expired(100, 100))
        assertTrue(SchedulePolicy.expired(100, 101))
    }

    @Test
    fun animationRespectsFrameDelaysAndLoops() {
        val sequence =
            FrameSequence(
                DeviceProfile.PHONE_4A_PRO,
                listOf(IntArray(169), IntArray(169)),
                listOf(100, 200),
            )
        assertEquals(0, sequence.indexAt(0))
        assertEquals(0, sequence.indexAt(99))
        assertEquals(1, sequence.indexAt(100))
        assertEquals(1, sequence.indexAt(299))
        assertEquals(0, sequence.indexAt(300))
    }

    @Test
    fun freshFaceUpReadingWinsOverAodAndStaleReadingDoesNotBlockIt() {
        assertFalse(DisplayPolicy.faceDown(true, false, true))
        assertTrue(DisplayPolicy.faceDown(true, true, false))
        assertTrue(DisplayPolicy.faceDown(false, false, true))
        assertFalse(DisplayPolicy.faceDown(false, true, false))
    }

    @Test
    fun lockUnlockHandoffNeverUsesAppRouteWhileLocked() {
        for (toy in listOf(true, false)) for (supported in listOf(true, false)) for (enabled in
            listOf(true, false)) for (interactive in listOf(true, false)) {
            val route = DisplayPolicy.route(toy, supported, enabled, interactive, locked = true)
            assertEquals(if (toy) DisplayPolicy.Route.TOY else null, route)
        }
        assertEquals(DisplayPolicy.Route.APP, DisplayPolicy.route(false, true, true, true, false))
        assertNull(DisplayPolicy.route(false, false, true, true, false))
        assertEquals(DisplayPolicy.Route.TOY, DisplayPolicy.route(true, true, true, true, false))
        assertNull(DisplayPolicy.route(false, true, false, true, false))
    }
}
