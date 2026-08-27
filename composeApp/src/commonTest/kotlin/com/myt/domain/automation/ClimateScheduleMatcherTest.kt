package com.myt.domain.automation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClimateScheduleMatcherTest {
    private val base = ClimateSchedule(
        id = "t1",
        name = "test",
        hour = 7,
        minute = 0,
        repeat = ClimateRepeat.Weekdays,
        enabled = true,
    )

    @Test
    fun weekdays_firesMondayNotSunday() {
        assertTrue(
            ClimateScheduleMatcher.shouldFire(base, hour = 7, minute = 0, dayOfWeek = 1, dayKey = "2026-08-24"),
        )
        assertFalse(
            ClimateScheduleMatcher.shouldFire(base, hour = 7, minute = 0, dayOfWeek = 7, dayKey = "2026-08-23"),
        )
    }

    @Test
    fun doesNotRefireSameDay() {
        val fired = base.copy(lastFiredKey = "2026-08-24")
        assertFalse(
            ClimateScheduleMatcher.shouldFire(fired, hour = 7, minute = 0, dayOfWeek = 1, dayKey = "2026-08-24"),
        )
    }

    @Test
    fun once_onlyBeforeFirstFire() {
        val once = base.copy(repeat = ClimateRepeat.Once)
        assertTrue(
            ClimateScheduleMatcher.shouldFire(once, hour = 7, minute = 0, dayOfWeek = 3, dayKey = "2026-08-26"),
        )
        assertFalse(
            ClimateScheduleMatcher.shouldFire(
                once.copy(lastFiredKey = "2026-08-26"),
                hour = 7,
                minute = 0,
                dayOfWeek = 4,
                dayKey = "2026-08-27",
            ),
        )
    }
}
