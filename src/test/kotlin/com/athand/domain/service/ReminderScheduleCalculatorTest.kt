package com.athand.domain.service

import com.athand.domain.model.UserPreferences
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderScheduleCalculatorTest {

    private val calculator = ReminderScheduleCalculator()

    @Test
    fun `returns null when reminders are disabled`() {
        val now = LocalDateTime.of(2026, 8, 2, 9, 0)

        val next = calculator.nextReminderTime(
            now = now,
            preferences = UserPreferences(reminderEnabled = false, reminderTime = LocalTime.of(9, 30))
        )

        assertNull(next)
    }

    @Test
    fun `returns null when reminder time is missing`() {
        val now = LocalDateTime.of(2026, 8, 2, 9, 0)

        val next = calculator.nextReminderTime(
            now = now,
            preferences = UserPreferences(reminderEnabled = true, reminderTime = null)
        )

        assertNull(next)
    }

    @Test
    fun `schedules reminder for later today when time is in the future`() {
        val now = LocalDateTime.of(2026, 8, 2, 9, 0)

        val next = calculator.nextReminderTime(
            now = now,
            preferences = UserPreferences(reminderEnabled = true, reminderTime = LocalTime.of(9, 30))
        )

        assertEquals(LocalDateTime.of(2026, 8, 2, 9, 30), next)
    }

    @Test
    fun `schedules reminder for tomorrow when today's time already passed`() {
        val now = LocalDateTime.of(2026, 8, 2, 20, 0)

        val next = calculator.nextReminderTime(
            now = now,
            preferences = UserPreferences(reminderEnabled = true, reminderTime = LocalTime.of(9, 30))
        )

        assertEquals(LocalDateTime.of(2026, 8, 3, 9, 30), next)
    }
}
